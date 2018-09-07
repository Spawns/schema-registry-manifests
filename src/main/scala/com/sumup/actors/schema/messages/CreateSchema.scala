package com.sumup.actors.schema.messages

import akka.http.scaladsl.model.HttpResponse
import com.sumup.dto.requests.SchemaRequest

final case class CreateSchema(completeFn: HttpResponse => Unit, schemaRequest: SchemaRequest)

