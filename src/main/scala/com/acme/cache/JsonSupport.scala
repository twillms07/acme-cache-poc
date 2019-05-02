package com.acme.cache

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.jackson.Serialization
import org.json4s.{DefaultFormats, Formats, jackson}

trait JsonSupport extends Json4sSupport {
    implicit val formats: Formats = DefaultFormats
    implicit val serialization: Serialization.type = jackson.Serialization
}
