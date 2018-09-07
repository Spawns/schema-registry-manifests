package com.sumup.diff

import com.sumup.diff.entities.SchemaDiffResult
import com.sumup.diff.enums.FieldChangeOperationType
import com.sumup.diff.exceptions.{MajorSchemaChangesException, MismatchingSchemaException}
import com.sumup.dto.Schema
import com.sumup.dto.fields.{ArrayField, Field, PrimitiveField, RecordField}
import com.sumup.dto.requests.SchemaRequest
import com.sumup.json.SchemaRegistryJsonProtocol.FieldFormat
import com.sumup.serde.SerializationService
import com.typesafe.scalalogging.Logger
import spray.json.DefaultJsonProtocol

import scala.collection.mutable.ListBuffer

class DiffProcessingService(implicit val serializationService: SerializationService) extends DefaultJsonProtocol {

  import gnieh.diffson.sprayJson._
  import spray.json._

  val log = Logger(getClass)

  def fullProcess(
                   schemaRequest: SchemaRequest,
                   schema: Schema,
                   isMajorUpgrade: Boolean,
                   isMinorUpgrade: Boolean
                 ): SchemaDiffResult = {
    val diffPatch = diff(schemaRequest, schema)
    process(diffPatch, schemaRequest, schema, isMajorUpgrade, isMinorUpgrade)
  }

  def diff(schemaRequest: SchemaRequest, schema: Schema): JsonPatch = {
    if (schemaRequest.name != schema.name) {
      throw MismatchingSchemaException(s"Given schema name: ${schemaRequest.name}, actual name: ${schema.name}.")
    }

    // NOTE: The order of comparison is very, very important.
    // When a new field is added in the `request`,
    // it'll be an `Add` operation, but if it's missing a `Remove` one and etc.
    JsonDiff.diff(
      // NOTE: Order to guarantee that shuffled fields won't count as schema difference.
      // This is to combat non-deterministic JSON serialization libraries that don't respect order.
      serializationService.serializeFieldsSorted(schema.fields).stripMargin,
      serializationService.serializeFieldsSorted(schemaRequest.fields).stripMargin,
      remember = true
    )
  }

  def process(
               patch: JsonPatch,
               schemaRequest: SchemaRequest,
               schema: Schema,
               isMajorUpgrade: Boolean,
               isMinorUpgrade: Boolean
             ): SchemaDiffResult = {
    var changes = ListBuffer[entities.Operation]()

    patch.ops.foreach {
      case add: Add =>
        changes += new entities.Add(
          FieldChangeOperationType.ADD,
          add.path.toString,
          add.value
        )
      case remove: Remove =>
        changes += new entities.Remove(
          FieldChangeOperationType.REMOVE,
          remove.path.toString,
          remove.old
        )
      case replace: Replace =>
        changes += new entities.Replace(
          FieldChangeOperationType.REPLACE,
          replace.path.toString,
          replace.value,
          replace.old
        )
      case move: Move =>
        changes += new entities.Move(FieldChangeOperationType.MOVE, move.from.toString, move.path.toString)
      case copy: Copy =>
        log.error(s"`Copy` operation found for ${schema.name} and path - ${copy.path}")
        changes += new entities.Copy(FieldChangeOperationType.COPY, copy.from.toString, copy.path.toString)
      case test: Test =>
        log.error(s"`Test` operation found ${schema.name} and path - ${test.path}")
        changes += new entities.Test(FieldChangeOperationType.TEST, test.path.toString, test.value)
    }

    if (changes.isEmpty) {
      SchemaDiffResult(
        isSameSchema = true,
        isMajorUpgradable = false,
        isMinorUpgradable = false,
        isMajorUpgrade,
        isMinorUpgrade
      )
    } else {
      val areThereMajorChanges = changes.exists { o =>
        o.isInstanceOf[entities.Remove] ||
          o.isInstanceOf[entities.Replace] ||
          (
            o.isInstanceOf[entities.Remove] &&
              !isIgnoredField(o.asInstanceOf[entities.Remove].path.toString)
            )
      }

      // NOTE: Even though minor changes on theory must be limited to minor version bumps only,
      // this limitation does not provide a clear benefit,
      // therefore allow major version bumps even though changes are only minor.
      if (areThereMajorChanges && !isMajorUpgrade) {
        SchemaDiffResult(
          isSameSchema = false,
          isMajorUpgradable = true,
          isMinorUpgradable = false,
          isMajorUpgrade,
          isMinorUpgrade,
          changes.toList
        )
      } else {
        SchemaDiffResult(
          isSameSchema = false,
          isMajorUpgradable = true,
          isMinorUpgradable = true,
          isMajorUpgrade,
          isMinorUpgrade,
          changes.toList
        )
      }
    }
  }

  def isIgnoredField(value: String): Boolean = {
    value == "/_id"
  }
}
