package com.sumup.actors.schema.messages

import akka.http.scaladsl.model.HttpResponse

final case class GetSchema(completeFn: HttpResponse => Unit, name: String)

