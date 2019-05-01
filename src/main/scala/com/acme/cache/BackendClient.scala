package com.acme.cache

import scala.concurrent.Future

trait BackendClient {
    def getBackendResponse(request: String): Future[String]
}
