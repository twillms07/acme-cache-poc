package com.acme.cache

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.acme.cache.CacheActor.{BackendResponse, CacheActorMessage, CacheActorRequest, CacheActorTimeout}
import com.acme.cache.CacheActorManager.{BackendClientResponse, BackendValue, CacheActorManagerMessage, CacheActorManagerTimeout}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class CacheActor(context: ActorContext[CacheActorMessage], key: String, cacheManager: ActorRef[CacheActorManagerMessage])
                (implicit backendClient: BackendClient[BackendRequest]) extends AbstractBehavior[CacheActorMessage] with BackendClientService {

    private var cacheValue: String = ""
    val buffer: StashBuffer[CacheActorMessage] = StashBuffer[CacheActorMessage](capacity = 100)
    val timerKey = s"timerKey-$key"
    val timerDelay: FiniteDuration = 10 seconds


    def onMessage(msg: CacheActorMessage): Behavior[CacheActorMessage] = init(msg)

    def init(msg: CacheActorMessage): Behavior[CacheActorMessage] = msg match {
        case CacheActorRequest(request, replyTo) ⇒
            context.log.debug(template = "Received initial request {} will get backend response.", request)
            val backendResponseF: Future[String] = getBackendClientResponse(BackendRequest(request))
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
            cacheManager ! BackendClientResponse(key,response,replyTo)
            buffer.unstashAll(context, available(msg))
    }

    def available(msg: CacheActorMessage): Behavior[CacheActorMessage] = Behaviors.receiveMessage {
        case CacheActorRequest(request, replyTo) ⇒
            context.log.debug(template = "Received backend response - ",request)
            cacheManager ! BackendClientResponse(key,cacheValue, replyTo)
            available(msg)

        case CacheActorTimeout ⇒
            cacheManager ! CacheActorManagerTimeout(key)
            Behaviors.stopped
    }

    override def onSignal: PartialFunction[Signal, Behavior[CacheActorMessage]] = {
        case PostStop ⇒
            context.log.debug(template = "We're stopping Cache Actor - {}", key)
            context.log.info(template = "Cache with key {} shutting down", key)
            this
    }
}

object CacheActor {

    trait CacheActorMessage
    final case class CacheActorRequest(request:String, replyTo: ActorRef[BackendValue]) extends CacheActorMessage
    final case class BackendResponse(response: String, replyTo: ActorRef[BackendValue]) extends CacheActorMessage
    final case object CacheActorTimeout extends CacheActorMessage

    def apply(key: String, cacheMgr: ActorRef[CacheActorManagerMessage])(implicit backendClient: BackendClient[BackendRequest]): Behavior[CacheActorMessage] =
        Behaviors.setup(context ⇒ new CacheActor(context, key, cacheMgr))

}

