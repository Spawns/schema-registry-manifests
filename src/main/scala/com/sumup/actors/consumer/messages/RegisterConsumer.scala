package com.sumup.actors.consumer.messages

import akka.http.scaladsl.model.HttpResponse
import com.sumup.dto.requests.ConsumerRequest

final case class RegisterConsumer(completeFn: HttpResponse => Unit, consumerRequest: ConsumerRequest)

