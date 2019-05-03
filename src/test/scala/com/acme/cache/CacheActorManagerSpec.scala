package com.acme.cache

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import com.acme.cache.CacheActorManager._
import org.scalatest.WordSpecLike
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class CacheActorManagerSpec extends ScalaTestWithActorTestKit with WordSpecLike {

    implicit val executionContext: ExecutionContext = system.executionContext
    val testBackend = new TestBackend()

    "CacheManagerActor " must {
        "Create a Cache Actor if one doesn't exist for the given key " in {
            val testCacheManager: ActorRef[CacheActorManager.CacheActorManagerMessage] = spawn(CacheActorManager(testBackend))
            val testHttpServerProbe = createTestProbe[CacheActorManagerResponse]()
            testCacheManager ! GetBackendValue("testKey", "testRequest1", testHttpServerProbe.ref)
            testHttpServerProbe.expectMessage(max = 10 seconds, BackendValue("BackendResponse-testRequest1"))
            testCacheManager ! GetCacheList(testHttpServerProbe.ref)
            testHttpServerProbe.expectMessage(max = 10 seconds, CacheListResponse(Set("testKey")))
        }

        "Don't create another Cache Actor if one exists for a given key " in {
            val testCacheManager: ActorRef[CacheActorManager.CacheActorManagerMessage] = spawn(CacheActorManager(testBackend))
            val testHttpServerProbe = createTestProbe[CacheActorManagerResponse]()
            testCacheManager ! GetBackendValue("testKey", "testRequest1", testHttpServerProbe.ref)
            testHttpServerProbe.expectMessage(max = 10 seconds, BackendValue("BackendResponse-testRequest1"))
            testCacheManager ! GetCacheList(testHttpServerProbe.ref)
            testHttpServerProbe.expectMessage(max = 10 seconds, CacheListResponse(Set("testKey")))
            testCacheManager ! GetBackendValue("testKey", "testRequest1", testHttpServerProbe.ref)
            testHttpServerProbe.expectMessage(max = 10 seconds, BackendValue("BackendResponse-testRequest1"))
            testCacheManager ! GetCacheList(testHttpServerProbe.ref)
            testHttpServerProbe.expectMessage(max = 10 seconds, CacheListResponse(Set("testKey")))
        }

        "Remove a Cache Actor if one exists for a given key " in {
            val testCacheManager: ActorRef[CacheActorManager.CacheActorManagerMessage] = spawn(CacheActorManager(testBackend))
            val testHttpServerProbe = createTestProbe[CacheActorManagerResponse]()
            testCacheManager ! GetBackendValue("testKey1", "testRequest1", testHttpServerProbe.ref)
            testHttpServerProbe.expectMessage(max = 10 seconds, BackendValue("BackendResponse-testRequest1"))
            testCacheManager ! GetCacheList(testHttpServerProbe.ref)
            testHttpServerProbe.expectMessage(max = 10 seconds, CacheListResponse(Set("testKey1")))
            testCacheManager ! GetBackendValue("testKey2", "testRequest2", testHttpServerProbe.ref)
            testHttpServerProbe.expectMessage(max = 10 seconds, BackendValue("BackendResponse-testRequest2"))
            testCacheManager ! GetCacheList(testHttpServerProbe.ref)
            testHttpServerProbe.expectMessage(max = 10 seconds, CacheListResponse(Set("testKey1", "testKey2")))
            testCacheManager ! CacheActorManagerTimeout("testKey1")
            testCacheManager ! GetCacheList(testHttpServerProbe.ref)
            testHttpServerProbe.expectMessage(max = 10 seconds, CacheListResponse(Set("testKey2")))
        }

    }

}

