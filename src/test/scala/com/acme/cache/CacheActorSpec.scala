package com.acme.cache

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import com.acme.cache.CacheActor.{CacheActorRequest}
import com.acme.cache.CacheActorManager.{BackendClientResponse, BackendValue, CacheActorManagerMessage, CacheActorManagerTimeout}
import org.scalatest.WordSpecLike
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class CacheActorSpec extends ScalaTestWithActorTestKit with WordSpecLike {

    import TestBackendClient.TestBackendClientImpl
    implicit val executionContext: ExecutionContext = system.executionContext

    "Cache actor " must {
        "Start out in init behavior and when receiving a CacheRequest request from the backend" in {
            val testCacheManager = createTestProbe[CacheActorManagerMessage]()
            val testHttpServer = createTestProbe[BackendValue]()
            val testCache: ActorRef[CacheActor.CacheActorMessage] = spawn(CacheActor("testKey", testCacheManager.ref))
            testCache ! CacheActorRequest("testRequest1",testHttpServer.ref)
            testCacheManager.expectMessage(BackendClientResponse("testKey","TestBackendResponse-testRequest1", testHttpServer.ref))
        }

        "Stash all additional requests while waiting for the backend response " in {
            val testCacheManager = createTestProbe[CacheActorManagerMessage]()
            val testCache: ActorRef[CacheActor.CacheActorMessage] = spawn(CacheActor("testKey", testCacheManager.ref))
            val testHttpServer = createTestProbe[BackendValue]()
            testCache ! CacheActorRequest("testRequest1", testHttpServer.ref)
            testCache ! CacheActorRequest("testRequest2", testHttpServer.ref)
            testCache ! CacheActorRequest("testRequest3", testHttpServer.ref)
            testCacheManager.expectMessage(BackendClientResponse("testKey", "TestBackendResponse-testRequest1", testHttpServer.ref))
            testCacheManager.expectMessage(BackendClientResponse("testKey","TestBackendResponse-testRequest1", testHttpServer.ref))
            testCacheManager.expectMessage(BackendClientResponse("testKey", "TestBackendResponse-testRequest1", testHttpServer.ref))
        }

        "Receive a cacheTimeout message when the cache needs to be renewed " in {
            val testCacheManager = createTestProbe[CacheActorManagerMessage]()
            val testHttpServer = createTestProbe[BackendValue]()
            val testCache: ActorRef[CacheActor.CacheActorMessage] = spawn(CacheActor("testKey", testCacheManager.ref))
            testCache ! CacheActorRequest("testRequest1", testHttpServer.ref)
            testCacheManager.expectMessage(BackendClientResponse("testKey","TestBackendResponse-testRequest1", testHttpServer.ref))
            testCacheManager.expectMessage(20 seconds, CacheActorManagerTimeout("testKey"))
        }

    }

}

