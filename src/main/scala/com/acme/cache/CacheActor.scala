package com.acme.cache

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.acme.cache.CacheActor.{BackendResponse, CacheRequest, CacheResponse, CacheTimeout, XmlCacheMessage}
import com.acme.cache.CacheManager.{XmlCacheManagerMessage, XmlCacheTimeout}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class CacheActor(context: ActorContext[XmlCacheMessage],
                 key: String, backendClient: BackendClient,
                 cacheManager: ActorRef[XmlCacheManagerMessage]) extends AbstractBehavior[XmlCacheMessage]{

    private var cacheValue: String = ""
    val buffer: StashBuffer[XmlCacheMessage] = StashBuffer[XmlCacheMessage](capacity = 100)
    val timerKey = s"timerKey-$key"
    val timerDelay: FiniteDuration = 5 seconds


    def onMessage(msg: XmlCacheMessage): Behavior[XmlCacheMessage] = init(msg)

    def init(msg: XmlCacheMessage): Behavior[XmlCacheMessage] = msg match {
        case CacheRequest(request, replyTo) ⇒
            context.log.debug(template = "Received initial request {} will get backend response.", request)
            val backendResponseF: Future[String] = backendClient.getBackendResponse(request)
            context.pipeToSelf(backendResponseF){
                case Success(r) ⇒
                    context.log.debug(template = "Successful response from backend system - {}",r)
                    BackendResponse(r, replyTo)
                case Failure(e) ⇒
                    context.log.error(template = "Error occurred in backend system request - {}", e)
                    BackendResponse(e.getMessage,replyTo)
            }
            Behaviors.withTimers(timer ⇒ waiting(msg,timer))
    }

    def waiting(msg:XmlCacheMessage, timer: TimerScheduler[XmlCacheMessage]):Behavior[XmlCacheMessage] = Behaviors.receiveMessage{
        case CacheRequest(request,replyTo) ⇒
            context.log.debug(template = "In waiting received request - ",request)
            buffer.stash(msg)
            waiting(msg,timer)
        case BackendResponse(response,replyTo) ⇒
            context.log.debug(template = "Received backend response - ",response)
            timer.startSingleTimer(timerKey, CacheTimeout, timerDelay)
            cacheValue = response
            replyTo ! CacheResponse(response)
            buffer.unstashAll(context, available(msg))
    }

    def available(msg: XmlCacheMessage): Behavior[XmlCacheMessage] = Behaviors.receiveMessage {
        case CacheRequest(request, replyTo) ⇒
            context.log.debug(template = "Received backend response - ",request)
            replyTo ! CacheResponse(cacheValue)
            available(msg)

        case CacheTimeout ⇒
            cacheManager ! XmlCacheTimeout(key)
            Behaviors.stopped
    }

    override def onSignal: PartialFunction[Signal, Behavior[XmlCacheMessage]] = {
        case PostStop ⇒
            println(s"We're stopping")
            context.log.info(template = "XmlCache with key {} shutting down", key)
            this
    }
}

object CacheActor {

    trait XmlCacheMessage
    final case class CacheRequest(request:String, replyTo: ActorRef[CacheResponse]) extends XmlCacheMessage
    final case class BackendResponse(response: String, replyTo: ActorRef[CacheResponse]) extends XmlCacheMessage
    final case object CacheTimeout extends XmlCacheMessage
    final case class CacheResponse(response:String)

    def apply(key: String, backendClient: BackendClient, xmlCacheMgr: ActorRef[XmlCacheManagerMessage]): Behavior[XmlCacheMessage] =
        Behaviors.setup(context ⇒ new CacheActor(context, key, backendClient, xmlCacheMgr))

}

