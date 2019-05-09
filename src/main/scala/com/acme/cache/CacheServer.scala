package com.acme.cache

import akka.actor
import akka.actor.Scheduler
import akka.actor.typed.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import com.acme.cache.CacheActorManager.{CacheActorManagerMessage, CacheActorManagerResponse, GetBackendValue}

import scala.concurrent.duration._
import scala.concurrent.{Future}
import scala.io.StdIn
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

class CacheServer

object CacheServer extends App with CacheRoutes {
    import akka.actor.typed.scaladsl.adapter._
    import com.acme.cache.ReferenceBackend.ReferenceBackendImpl

    val cacheActor: CacheActor.type = CacheActor
    val typedActorSystem: ActorSystem[CacheActorManagerMessage] =
        ActorSystem[CacheActorManagerMessage](CacheActorManager(), name = "CacheManager")

    implicit val actorSystem: actor.ActorSystem = typedActorSystem.toUntyped
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val timeout: Timeout = 3 seconds
    implicit val scheduler: Scheduler = actorSystem.scheduler

    val logger: LoggingAdapter = Logging(actorSystem, classOf[CacheServer])
    val interface = "0.0.0.0"
    val port = 8080

    Http().bindAndHandle(routes, interface = interface, port = port)

    logger.debug("Cache System up")

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

    implicit val scheduler: Scheduler
    implicit val timeout: Timeout

    val typedActorSystem:ActorSystem[CacheActorManagerMessage]
    val logger: LoggingAdapter

    val getCache: Route = path(pm = "cache" / "key" / Segment / "request" / Segment) {(key,request) ⇒
        get {
            val resultMaybe: Future[CacheActorManagerResponse] = typedActorSystem.ask(ref ⇒ GetBackendValue(key,request,ref))
            onComplete(resultMaybe) {
                case Success(result) ⇒
                    complete(StatusCodes.OK, result)
                case Failure(exception) ⇒
                    complete(StatusCodes.InternalServerError, exception)
            }
        }
    }

    val routes: Route = getCache

}