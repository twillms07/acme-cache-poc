package com.acme.cache

import scala.concurrent.Future

trait BackendClient[R] {
    def backendClientResponse(request: R): Future[String]
}
