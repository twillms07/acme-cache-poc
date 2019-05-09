package com.acme.cache

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.acme.cache.CacheActorManager._
import com.acme.cache.CacheActor._
import scala.collection.concurrent.TrieMap

class CacheActorManager(context: ActorContext[CacheActorManagerMessage])
                       (implicit backendClient: BackendClient[BackendRequest]) extends AbstractBehavior[CacheActorManagerMessage] {


    val cacheMap: TrieMap[String, ActorRef[CacheActorMessage]] = TrieMap.empty

    def onMessage(msg: CacheActorManagerMessage): Behavior[CacheActorManagerMessage] = msg match {

        case GetBackendValue(key, request, replyTo) ⇒
            val cacheActorO = cacheMap.get(key)
            cacheActorO match {
                case Some(cacheActor) ⇒
                    cacheActor ! CacheActorRequest(request,replyTo)
                case None ⇒
                    val cacheActor: ActorRef[CacheActorMessage] = context.spawn(CacheActor(key,context.self), key)
                    cacheMap += (key → cacheActor)
                    cacheActor ! CacheActorRequest(request,replyTo)
            }
            Behaviors.same

        case BackendClientResponse(id, response, replyTo) ⇒
            replyTo ! BackendValue(response)
            Behaviors.same

        case CacheActorManagerTimeout(key) ⇒
            val cacheActorO = cacheMap.get(key)
            cacheActorO match {
                case Some(cacheActor) ⇒
                    cacheActor ! CacheActorTimeout
                    cacheMap -= (key)
                case None ⇒
                    context.log.debug( template = "Cache Actor with key - {} not found", key)
            }
            Behaviors.same

        case GetCacheList(replyTo) ⇒
            val cacheList: collection.Set[String] = cacheMap.keySet
            replyTo ! CacheListResponse(cacheList)
            Behaviors.same
    }
}


object CacheActorManager {

    trait CacheActorManagerMessage
    case class CacheActorManagerTimeout(key:String) extends CacheActorManagerMessage
    case class GetBackendValue(key: String, request: String, replyTo: ActorRef[BackendValue]) extends CacheActorManagerMessage
    case class BackendClientResponse(key: String, response: String, replyTo: ActorRef[BackendValue]) extends CacheActorManagerMessage
    case class GetCacheList(replyTo: ActorRef[CacheListResponse]) extends CacheActorManagerMessage

    trait CacheActorManagerResponse
    case class BackendValue(response: String) extends CacheActorManagerResponse
    case class CacheListResponse(cacheList: collection.Set[String]) extends CacheActorManagerResponse

    def apply()(implicit backendClient: BackendClient[BackendRequest]): Behavior[CacheActorManagerMessage] =
        Behaviors.setup(context ⇒ new CacheActorManager(context))
}
