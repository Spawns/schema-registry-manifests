package com.sumup.actors.compatibility.messages

import akka.http.scaladsl.model.HttpResponse
import com.sumup.dto.requests.SchemaRequest

final case class CompatibilityCheck(completeFn: HttpResponse => Unit, schemaRequest: SchemaRequest)

