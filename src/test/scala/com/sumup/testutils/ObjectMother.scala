package com.sumup.testutils

import com.sumup.diff.entities.{Add, SchemaDiffResult}
import com.sumup.diff.enums.FieldChangeOperationType
import com.sumup.dto.{Consumer, Schema}
import com.sumup.dto.requests.{ConsumerRequest, SchemaRequest}
import com.sumup.testutils.builders._
import spray.json.JsString

object ObjectMother {
  def defaultSchemaRequest(): SchemaRequest = SchemaRequestBuilder.aSchemaRequest().build()
  def defaultConsumerRequest(): ConsumerRequest = ConsumerRequestBuilder.aConsumerRequest().build()
  def defaultConsumer(): Consumer = ConsumerBuilder.aConsumer().build()
  def defaultSchema(): Schema = SchemaBuilder.aSchema().build()
  def defaultSchemaDiffResultForSameSchema(): SchemaDiffResult = {
    SchemaDiffResultBuilder
      .aSchemaDiffResult()
      .withIsSameSchema(true)
      .withIsMajorUpgradable(false)
      .withIsMinorUpgradable(false)
      .withIsMajorUpgrade(false)
      .withIsMinorUpgrade(false)
      .withFieldChanges(List())
      .build()
  }
  def defaultSchemaDiffResultForUpgradableSchema(): SchemaDiffResult = {
    SchemaDiffResultBuilder
      .aSchemaDiffResult()
      .withIsSameSchema(false)
      .withIsMajorUpgradable(true)
      .withIsMinorUpgradable(true)
      .withIsMajorUpgrade(false)
      .withIsMinorUpgrade(false)
      .withFieldChanges(
        List(
          new Add(
            FieldChangeOperationType.ADD,
            "/0/some-new-field",
            JsString("my-new-field")
          )
        )
      )
      .build()
  }
}
