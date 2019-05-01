package com.acme.cache

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.acme.cache.CacheManager.XmlCacheManagerMessage

class CacheManager(context: ActorContext[XmlCacheManagerMessage]) extends AbstractBehavior[XmlCacheManagerMessage]{

    override def onMessage(msg: XmlCacheManagerMessage): Behavior[XmlCacheManagerMessage] = msg match {
        case _ ⇒
            println("This is working")
            Behaviors.same
    }

}


object CacheManager {
    trait XmlCacheManagerMessage
    case class XmlCacheTimeout(key:String) extends XmlCacheManagerMessage

    def apply():Behavior[XmlCacheManagerMessage] = Behaviors.setup(context ⇒ new CacheManager(context))
}
