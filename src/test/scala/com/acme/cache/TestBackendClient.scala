package com.acme.cache
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object TestBackendClient {

    implicit object TestBackendClientImpl extends BackendClient[BackendRequest] {
        override def backendClientResponse(request: BackendRequest): Future[String] = {
            println(s"TestBackendClientImpl - $request")
            Thread.sleep(500)
            Future(s"TestBackendResponse-${request.request}")
        }
    }
}
