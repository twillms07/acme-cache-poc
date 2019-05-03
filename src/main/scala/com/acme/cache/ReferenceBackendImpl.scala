package com.acme.cache

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

class ReferenceBackendImpl() extends BackendClient {
    override def getBackendResponse(request: String): Future[String] = {
        println(s"This is getting called")
//        Thread.sleep(500)
        Future(s"ReferenceBackendResponse-$request")
    }
}
