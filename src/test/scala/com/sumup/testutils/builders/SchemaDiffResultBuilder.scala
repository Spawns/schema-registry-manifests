package com.sumup.testutils.builders

import com.sumup.diff.entities.{Add, Operation, SchemaDiffResult}
import com.sumup.diff.enums.FieldChangeOperationType
import spray.json.JsString


object SchemaDiffResultBuilder {
  final val DEFAULT_IS_SAME_SCHEMA: Boolean = false
  final var DEFAULT_IS_MAJOR_UPGRADABLE: Boolean = true
  final var DEFAULT_IS_MINOR_UPGRADABLE: Boolean = true
  final var DEFAULT_IS_MAJOR_UPGRADE: Boolean = false
  final var DEFAULT_IS_MINOR_UPGRADE: Boolean = false
  final var DEFAULT_FIELD_CHANGES: List[Operation] = List(
    new Add(
      FieldChangeOperationType.ADD,
      "/0/new-field",
      JsString("my-new-field")
    )
  )

  def aSchemaDiffResult() = new SchemaDiffResultBuilder
}

class SchemaDiffResultBuilder extends Builder[SchemaDiffResult] {
  private var isSameSchema: Boolean = SchemaDiffResultBuilder.DEFAULT_IS_SAME_SCHEMA
  private var isMajorUpgradable: Boolean = SchemaDiffResultBuilder.DEFAULT_IS_MAJOR_UPGRADABLE
  private var isMinorUpgradable: Boolean = SchemaDiffResultBuilder.DEFAULT_IS_MINOR_UPGRADABLE
  private var isMajorUpgrade: Boolean = SchemaDiffResultBuilder.DEFAULT_IS_MAJOR_UPGRADE
  private var isMinorUpgrade: Boolean = SchemaDiffResultBuilder.DEFAULT_IS_MINOR_UPGRADE
  private var fieldChanges: List[Operation] = SchemaDiffResultBuilder.DEFAULT_FIELD_CHANGES

  def withIsSameSchema(newIsSameSchema: Boolean): SchemaDiffResultBuilder = {
    isSameSchema = newIsSameSchema
    this
  }

  def withIsMajorUpgradable(newIsUpgradable: Boolean): SchemaDiffResultBuilder = {
    isMajorUpgradable = newIsUpgradable
    this
  }

  def withIsMinorUpgradable(newIsUpgradable: Boolean): SchemaDiffResultBuilder = {
    isMinorUpgradable = newIsUpgradable
    this
  }

  def withIsMajorUpgrade(newIsUpgrade: Boolean): SchemaDiffResultBuilder = {
    isMajorUpgrade = newIsUpgrade
    this
  }

  def withIsMinorUpgrade(newIsUpgrade: Boolean): SchemaDiffResultBuilder = {
    isMinorUpgrade = newIsUpgrade
    this
  }

  def withFieldChanges(newFieldChanges: List[Operation]): SchemaDiffResultBuilder = {
    fieldChanges = newFieldChanges
    this
  }

  override def build(): SchemaDiffResult =
    SchemaDiffResult(
      isSameSchema,
      isMajorUpgradable,
      isMinorUpgradable,
      isMajorUpgrade,
      isMinorUpgrade,
      fieldChanges
    )
}
