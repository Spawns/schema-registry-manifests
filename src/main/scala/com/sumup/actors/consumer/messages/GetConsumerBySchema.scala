package com.sumup.actors.consumer.messages

import akka.http.scaladsl.model.HttpResponse

final case class GetConsumerBySchema(
                                      completeFn: HttpResponse => Unit,
                                      name: String,
                                      schemaName: String,
                                      schemaMajorVersion: Int,
                                      schemaMinorVersion: Int
                                    )

