package com.acme.cache

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.acme.cache.CacheManager.CacheManagerMessage

class CacheManager(context: ActorContext[CacheManagerMessage]) extends AbstractBehavior[CacheManagerMessage]{

    override def onMessage(msg: CacheManagerMessage): Behavior[CacheManagerMessage] = msg match {
        case _ ⇒
            println("This is working")
            Behaviors.same
    }

}


object CacheManager {
    trait CacheManagerMessage
    case class CacheTimeout(key:String) extends CacheManagerMessage

    def apply():Behavior[CacheManagerMessage] = Behaviors.setup(context ⇒ new CacheManager(context))
}
