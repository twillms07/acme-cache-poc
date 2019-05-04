package com.acme.cache

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class BackendRequest(request: String)

object ReferenceBackend{

    implicit object ReferenceBackendImpl extends BackendClient[BackendRequest] {
        def backendClientResponse(request: BackendRequest): Future[String] = {
            println(s"backendClientResponse - $request")
            Thread.sleep(500)
            Future(s"ReferenceBackendResponse-$request")
        }

    }
}
