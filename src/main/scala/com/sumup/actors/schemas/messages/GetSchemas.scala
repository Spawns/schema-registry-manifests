package com.sumup.actors.schemas.messages

import akka.http.scaladsl.model.HttpResponse

final case class GetSchemas(completeFn: HttpResponse => Unit)
