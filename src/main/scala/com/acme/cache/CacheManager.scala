package com.acme.cache

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.acme.cache.CacheActor.CacheActorMessage
import com.acme.cache.CacheManager.{BackendValue, CacheManagerMessage, GetBackendValue}

import scala.collection.concurrent.TrieMap

class CacheManager(context: ActorContext[CacheManagerMessage]) extends AbstractBehavior[CacheManagerMessage]{

    var cacheMap: TrieMap[String, ActorRef[CacheActorMessage]] = TrieMap.empty

    override def onMessage(msg: CacheManagerMessage): Behavior[CacheManagerMessage] = msg match {
        case GetBackendValue(id, item, replyTo) ⇒
            val cacheActorO = cacheMap.get(id)
            cacheActor match {
                case Some(cacheActor) ⇒
                    cacheActor !
                case None ⇒
            }
            println("This is working")
            Behaviors.same
    }

}


object CacheManager {
    trait CacheManagerMessage
    case class CacheTimeout(key:String) extends CacheManagerMessage
    case class GetBackendValue(id: String, request: String, replyTo: ActorRef[BackendValue]) extends CacheManagerMessage
    case class BackendResponse(id: String, response: String) extends CacheManagerMessage
    case class BackendValue(response: String)

    def apply(): Behavior[CacheManagerMessage] = Behaviors.setup(context ⇒ new CacheManager(context))
}
