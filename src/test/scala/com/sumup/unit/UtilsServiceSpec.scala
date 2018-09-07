package com.sumup.unit

import com.sumup.UtilsService
import com.sumup.dto.fields.{IntField, RecordField, StringField}
import org.scalatest.{Outcome, fixture}

class UtilsServiceSpec extends fixture.FunSpec {
  type FixtureParam = UtilsService

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = new UtilsService()
    test(fixture)
  }

  describe(".sortFields") {
    describe("with a `RecordField`") {
      describe("containing other `RecordField`")  {
        it("sorts all fields including `RecordField`'s fields by `name` and `type`") { serializationService =>
          val firstField = IntField("1")
          val secondField = IntField("2")
          val fourthField = StringField("5")
          val nestedRecordField = RecordField("4", List(secondField, firstField))
          val recordField = RecordField("3", List(fourthField, nestedRecordField, secondField, firstField))

          val mixedFields = List(
            secondField,
            recordField,
            firstField,
            fourthField
          )

          val sortedFields = List(
            firstField,
            secondField,
            recordField.copy(fields = List(
              firstField,
              secondField,
              nestedRecordField.copy(fields = List(firstField, secondField)),
              fourthField)
            ),
            fourthField
          )

          assert(serializationService.sortFields(mixedFields) == sortedFields)
        }
      }

      describe("not containing other `RecordField`") {
        it("sorts all fields including `RecordField`'s fields by `name` and `type`") { serializationService =>
          val firstField = IntField("1")
          val secondField = IntField("2")
          val fourthField = StringField("4")
          val recordField = RecordField("3", List(fourthField, secondField, firstField))

          val mixedFields = List(
            secondField,
            recordField,
            firstField,
            fourthField
          )

          val sortedFields = List(
            firstField,
            secondField,
            recordField.copy(fields = List(firstField, secondField, fourthField)),
            fourthField
          )

          assert(serializationService.sortFields(mixedFields) == sortedFields)
        }
      }
    }

    describe("with a `PrimitiveField`") {
      it("sorts them by `name` and `type`") { serializationService =>
        val firstField = IntField("1")
        val secondField = IntField("2")
        val thirdField = StringField("3")

        val mixedFields = List(
          secondField,
          firstField,
          thirdField
        )

        val sortedFields = List(
          firstField,
          secondField,
          thirdField
        )

        assert(serializationService.sortFields(mixedFields) == sortedFields)
      }
    }
  }
}
