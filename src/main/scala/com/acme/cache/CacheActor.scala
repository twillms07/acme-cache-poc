package com.acme.cache

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.acme.cache.CacheActor.{BackendResponse, CacheActorMessage, CacheActorRequest, CacheActorTimeout, CacheResponse}
import com.acme.cache.CacheManager.{CacheManagerMessage, CacheTimeout}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class CacheActor(context: ActorContext[CacheActorMessage],
                 key: String, backendClient: BackendClient,
                 cacheManager: ActorRef[CacheManagerMessage]) extends AbstractBehavior[CacheActorMessage]{

    private var cacheValue: String = ""
    val buffer: StashBuffer[CacheActorMessage] = StashBuffer[CacheActorMessage](capacity = 100)
    val timerKey = s"timerKey-$key"
    val timerDelay: FiniteDuration = 5 seconds


    def onMessage(msg: CacheActorMessage): Behavior[CacheActorMessage] = init(msg)

    def init(msg: CacheActorMessage): Behavior[CacheActorMessage] = msg match {
        case CacheActorRequest(request, replyTo) ⇒
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

    def waiting(msg:CacheActorMessage, timer: TimerScheduler[CacheActorMessage]):Behavior[CacheActorMessage] = Behaviors.receiveMessage{
        case CacheActorRequest(request,replyTo) ⇒
            context.log.debug(template = "In waiting received request - ",request)
            buffer.stash(msg)
            waiting(msg,timer)
        case BackendResponse(response,replyTo) ⇒
            context.log.debug(template = "Received backend response - ",response)
            timer.startSingleTimer(timerKey, CacheActorTimeout, timerDelay)
            cacheValue = response
            replyTo ! CacheResponse(response)
            buffer.unstashAll(context, available(msg))
    }

    def available(msg: CacheActorMessage): Behavior[CacheActorMessage] = Behaviors.receiveMessage {
        case CacheActorRequest(request, replyTo) ⇒
            context.log.debug(template = "Received backend response - ",request)
            replyTo ! CacheResponse(cacheValue)
            available(msg)

        case CacheActorTimeout ⇒
            cacheManager ! CacheTimeout(key)
            Behaviors.stopped
    }

    override def onSignal: PartialFunction[Signal, Behavior[CacheActorMessage]] = {
        case PostStop ⇒
            println(s"We're stopping")
            context.log.info(template = "XmlCache with key {} shutting down", key)
            this
    }
}

object CacheActor {

    trait CacheActorMessage
    final case class CacheActorRequest(request:String, replyTo: ActorRef[CacheResponse]) extends CacheActorMessage
    final case class BackendResponse(response: String, replyTo: ActorRef[CacheResponse]) extends CacheActorMessage
    final case object CacheActorTimeout extends CacheActorMessage
    final case class CacheResponse(response:String)

    def apply(key: String, backendClient: BackendClient, xmlCacheMgr: ActorRef[CacheManagerMessage]): Behavior[CacheActorMessage] =
        Behaviors.setup(context ⇒ new CacheActor(context, key, backendClient, xmlCacheMgr))

}

