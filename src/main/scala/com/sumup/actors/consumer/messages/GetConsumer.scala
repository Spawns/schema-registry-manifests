package com.sumup.actors.consumer.messages

import akka.http.scaladsl.model.HttpResponse

final case class GetConsumer(
                              completeFn: HttpResponse => Unit,
                              name: String
                            )

