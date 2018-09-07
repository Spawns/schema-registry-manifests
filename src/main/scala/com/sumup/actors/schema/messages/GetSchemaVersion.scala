package com.sumup.actors.schema.messages

import akka.http.scaladsl.model.HttpResponse

final case class GetSchemaVersion(completeFn: HttpResponse => Unit, name: String, majorVersion: Int, minorVersion: Int)

