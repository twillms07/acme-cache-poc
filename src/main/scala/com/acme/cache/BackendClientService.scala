package com.acme.cache

import scala.concurrent.Future

trait BackendClientService {
    def getBackendClientResponse[R](req:R)(implicit backendClientImpl: BackendClient[R]): Future[String] =
        backendClientImpl.backendClientResponse(req)
}
