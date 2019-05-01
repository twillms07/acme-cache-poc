package com.acme.cache
import akka.actor
import akka.actor.Scheduler
import akka.actor.typed.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.io.StdIn
import scala.util.control.NonFatal

class CacheServer

object CacheServer extends App with CacheRoutes {
    import akka.actor.typed.scaladsl.adapter._
//    val actorSystemTyped = ActorSystem[???](???(),"DeviceMgr")


//    implicit val executionContext: ExecutionContext = actorSystemTyped.executionContext
//    implicit val actorSystem: actor.ActorSystem = actorSystemTyped.toUntyped
//    implicit val materializer: ActorMaterializer = ActorMaterializer()
//    implicit val timeout: Timeout = 3 seconds
//    implicit val scheduler: Scheduler = actorSystem.scheduler
//
//    val logger: LoggingAdapter = Logging(actorSystem, classOf[XmlCacheServer])
//    val interface = "0.0.0.0"
//    val port = 8080
//
//    Http().bindAndHandle(routes, interface = interface, port = port)

//
//    logger.debug("Iot System up")
//
//    try {
//        logger.info(">>> Press ENTER to exit <<<")
//        StdIn.readLine()
//    } catch {
//        case NonFatal(e) => actorSystemTyped.terminate()
//    }finally {
//        actorSystemTyped.terminate()
//    }

}

trait CacheRoutes extends ScalaXmlSupport {

    val routes: Route = ???

}