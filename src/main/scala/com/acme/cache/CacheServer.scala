package com.acme.cache
import akka.actor
import akka.actor.Scheduler
import akka.actor.typed.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.acme.cache.CacheActorManager.CacheActorManagerMessage
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext
import scala.io.StdIn
import scala.util.control.NonFatal

class CacheServer

object CacheServer extends App with CacheRoutes {
    import akka.actor.typed.scaladsl.adapter._

    val backendClient: BackendClient = ???
    val typedActorSystem: ActorSystem[CacheActorManagerMessage] = ActorSystem[CacheActorManagerMessage](CacheActorManager(backendClient), name = "CacheManager")


    implicit val executionContext: ExecutionContext = typedActorSystem.executionContext
    implicit val actorSystem: actor.ActorSystem = typedActorSystem.toUntyped
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val timeout: Timeout = 3 seconds
    implicit val scheduler: Scheduler = actorSystem.scheduler

    val logger: LoggingAdapter = Logging(actorSystem, classOf[CacheServer])
    val interface = "0.0.0.0"
    val port = 8080

    Http().bindAndHandle(routes, interface = interface, port = port)


    logger.debug("Iot System up")

    try {
        logger.info(">>> Press ENTER to exit <<<")
        StdIn.readLine()
    } catch {
        case NonFatal(e) => typedActorSystem.terminate()
    }finally {
        typedActorSystem.terminate()
    }

}

trait CacheRoutes extends JsonSupport {

    val routes: Route = ???

}