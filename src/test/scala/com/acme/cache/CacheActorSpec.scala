package com.acme.cache

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import akka.util.Timeout
import com.acme.cache.CacheActor.{CacheRequest, CacheResponse, XmlCacheMessage}
import com.acme.cache.CacheManager.{XmlCacheManagerMessage, XmlCacheTimeout}
import org.scalatest.WordSpecLike

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class CacheActorSpec extends ScalaTestWithActorTestKit with WordSpecLike {

    implicit val executionContext: ExecutionContext = system.executionContext
    val testBackend = new TestBackend()

    "XmlCache actor " must {
        "Start out in init behavior and when receiving a CacheRequest request from the backend" in {
            val testXmlCacheManager = createTestProbe[XmlCacheManagerMessage]()
            val testXmlCache: ActorRef[CacheActor.XmlCacheMessage] = spawn(CacheActor("testKey",testBackend, testXmlCacheManager.ref))
            val testProbe = createTestProbe[CacheResponse]()
            testXmlCache ! CacheRequest("testRequest1",testProbe.ref)
            testProbe.expectMessage(CacheResponse("BackendResponse-testRequest1"))
        }

        "Stash all additional requests while waiting for the backend response " in {
            val testXmlCacheManager = createTestProbe[XmlCacheManagerMessage]()
            val testXmlCache: ActorRef[CacheActor.XmlCacheMessage] = spawn(CacheActor("testKey",testBackend, testXmlCacheManager.ref))
            val testProbe = createTestProbe[CacheResponse]()
            testXmlCache ! CacheRequest("testRequest1", testProbe.ref)
            testXmlCache ! CacheRequest("testRequest2", testProbe.ref)
            testXmlCache ! CacheRequest("testRequest3", testProbe.ref)
            testProbe.expectMessage(CacheResponse("BackendResponse-testRequest1"))
            testProbe.expectMessage(CacheResponse("BackendResponse-testRequest1"))
            testProbe.expectMessage(CacheResponse("BackendResponse-testRequest1"))
        }

        "Receive a cacheTimeout message when the cache needs to be renewed " in {
            val testXmlCacheManager = createTestProbe[XmlCacheManagerMessage]()
            val testXmlCache: ActorRef[CacheActor.XmlCacheMessage] = spawn(CacheActor("testKey",testBackend, testXmlCacheManager.ref))
            val cacheResponseTestProbe = createTestProbe[CacheResponse]()
            testXmlCache ! CacheRequest("testRequest1", cacheResponseTestProbe.ref)
            cacheResponseTestProbe.expectMessage(CacheResponse("BackendResponse-testRequest1"))
            testXmlCacheManager.expectMessage(20 seconds, XmlCacheTimeout("testKey"))
        }

    }

}

class TestBackend(implicit val executionContext: ExecutionContext) extends BackendClient {
    override def getBackendResponse(request: String): Future[String] = {
        Thread.sleep(500)
        Future(s"BackendResponse-$request")
    }
}