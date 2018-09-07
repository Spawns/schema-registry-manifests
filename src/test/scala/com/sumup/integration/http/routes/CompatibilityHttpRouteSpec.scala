package com.sumup.integration.http.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.sumup.UtilsService
import com.sumup.dto.FieldType
import com.sumup.dto.fields.{ArrayField, IntField, RecordField, StringField}
import com.sumup.dto.responses.standard.NotFoundResponse
import com.sumup.serde.SerializationService
import com.sumup.storage.repositories.SchemaRepository
import com.sumup.testutils.builders.SchemaRequestBuilder
import com.sumup.testutils.{DatabaseSpec, ObjectMother}
import org.scalatest.{BeforeAndAfterEach, DoNotDiscover, FunSpec}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

@DoNotDiscover
class CompatibilityHttpRouteSpec extends FunSpec with DatabaseSpec with BeforeAndAfterEach {
  import spray.json._
  implicit val system: ActorSystem = ActorSystem("CompatibilityHttpRouteSpec")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  override implicit var ec: ExecutionContext = system.dispatcher
  val serverAddress = s"http://${config.getString("application.http.address")}:${config.getInt("application.http.port")}"

  override def beforeEach(): Unit = {
    cleanDatabase()
    super.beforeEach()
  }
  override def afterEach: Unit = cleanDatabase()

  val schemaRepository = new SchemaRepository()(mongoClientWrapper = mongoClientWrapper)
  val utilsService = new UtilsService
  val serializationService = new SerializationService()(utilsService=utilsService)

  describe("POST /api/compatibility") {
    val apiUrl = s"$serverAddress/api/compatibility"

    describe("with incomplete `SchemaRequest` body") {
      it("returns an error") {
        val entity = HttpEntity(
          ContentTypes.`application/json`,
          JsObject(
            "name" -> JsString("example-schema"),
            "applicationId" -> JsString("example-app"),
            "majorVersion" -> JsNumber(1),
            "minorVersion" -> JsNumber(3),
            "fields" -> JsNull
          ).toString()
        )

        val future = Http().singleRequest(
          HttpRequest(
            HttpMethods.POST,
            uri = apiUrl,
            entity = entity
          )
        )
        val response = Await.result(future, 5 seconds)
        assert(response.status == StatusCodes.BadRequest)
        assert(
          response.entity ==
            HttpEntity(ContentTypes.`text/plain(UTF-8)`, "The request content was malformed:\nExpected List as JsArray, but got null")
        )
      }
    }

    describe("with valid `SchemaRequest` body") {
      describe("and not at least one field specified in `fields`") {
        it("returns an error") {
          val entity = HttpEntity(
            ContentTypes.`application/json`,
            JsObject(
              "name" -> JsString("example-schema"),
              "applicationId" -> JsString("example-app"),
              "majorVersion" -> JsNumber(1),
              "minorVersion" -> JsNumber(3),
              "fields" -> JsArray()
            ).toString()
          )

          val future = Http().singleRequest(
            HttpRequest(
              HttpMethods.POST,
              uri = apiUrl,
              entity = entity
            )
          )
          val response = Await.result(future, 5 seconds)
          assert(response.status == StatusCodes.BadRequest)
          assert(
            response.entity ==
              HttpEntity(ContentTypes.`text/plain(UTF-8)`, "The request content was malformed:\n`fields` is empty")
          )
        }
      }

      describe("and at least one field specified in `fields`") {
        describe("with existing schema not matching `SchemaRequest`") {
          it("responds with `NotFoundResponse`") {
            val schemaRequest = ObjectMother.defaultSchemaRequest()
            val isCreated = schemaRepository.create(
              schemaRequest.name + "different",
              schemaRequest.applicationId,
              schemaRequest.majorVersion,
              schemaRequest.minorVersion,
              schemaRequest.fields
            )

            assert(isCreated)

            val entity = HttpEntity(
              ContentTypes.`application/json`,
              serializationService.serialize(schemaRequest)
            )

            val future = Http().singleRequest(
              HttpRequest(
                HttpMethods.POST,
                uri = apiUrl,
                entity = entity
              )
            )
            val response = Await.result(future, 5 seconds)
            assert(response.status == StatusCodes.NotFound)
            assert(
              response.entity ==
                HttpEntity(ContentTypes.`application/json`, serializationService.serialize(NotFoundResponse()))
            )
          }
        }

        describe("with existing schema matching by `SchemaRequest`") {
          describe("with primitive fields") {
            describe("that are the same") {
              it("responds with `isSameSchema` true") {
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(
                    List(
                      IntField("example-field-1"),
                      StringField("example-field-2")
                    )
                  )
                  .build()
                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  schemaRequest.fields
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(true),
                    "isMajorUpgradable" -> JsBoolean(false),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray()
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that are the same but shuffled") {
              it("responds with `isSameSchema` true") {
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(
                    List(
                      StringField("example-field-2"),
                      IntField("example-field-1")
                    )
                  )
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  schemaRequest.fields
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(true),
                    "isMajorUpgradable" -> JsBoolean(false),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray()
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause they're duplicate") {
              it("responds with field changes for `add`") {
                val field = IntField("example-field")
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(field, field))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  List(field)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(true),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("add"),
                        "path" -> JsString("/-"),
                        "value" -> JsObject(
                          "name" -> JsString(field.name),
                          "type" -> JsString(field.`type`.toString),
                          "isIdentity" -> JsBoolean(field.isIdentity)
                        )
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause there's a name change") {
              it("responds with field changes for `replace`") {
                val field = IntField("example-field")
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(field))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  List(
                    field.copy(name = "old-example-field")
                  )
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("replace"),
                        "path" -> JsString("/0/name"),
                        "value" -> JsString(field.name),
                        "oldValue" -> JsString("old-example-field")
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause there's a type change") {
              it("responds with field changes for `replace`") {
                val field = IntField("example-field")
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(field))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  List(
                    field.copy(`type` = FieldType.STRING)
                  )
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("replace"),
                        "path" -> JsString("/0/type"),
                        "value" -> JsString(field.`type`.toString),
                        "oldValue" -> JsString(FieldType.STRING.toString)
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }
          }

          describe("with array fields") {
            describe("that are the same") {
              it("responds with `isSameSchema` true") {
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(
                    List(
                      ArrayField("example-field-1", FieldType.INT),
                      ArrayField("example-field-2", FieldType.STRING)
                    )
                  )
                  .build()
                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  schemaRequest.fields
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(true),
                    "isMajorUpgradable" -> JsBoolean(false),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray()
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that are the same but shuffled") {
              it("responds with `isSameSchema` true") {
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(
                    List(
                      ArrayField("example-field-2", FieldType.STRING),
                      ArrayField("example-field-1", FieldType.INT)
                    )
                  )
                  .build()
                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  schemaRequest.fields
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(true),
                    "isMajorUpgradable" -> JsBoolean(false),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray()
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause they're duplicate") {
              it("responds with field changes for `add`") {
                val field = ArrayField("example-field", FieldType.INT)
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(field, field))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  List(field)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(true),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("add"),
                        "path" -> JsString("/-"),
                        "value" -> JsObject(
                          "name" -> JsString(field.name),
                          "type" -> JsString(field.`type`.toString),
                          "isIdentity" -> JsBoolean(field.isIdentity),
                          "items" -> JsString(field.items.toString)
                        )
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause there's a name change") {
              it("responds with field changes for `replace`") {
                val field = ArrayField("example-field", FieldType.INT)
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(field.copy(name="new-example-field")))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  List(field)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("replace"),
                        "path" -> JsString("/0/name"),
                        "value" -> JsString("new-example-field"),
                        "oldValue" -> JsString("example-field")
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause there's a type change to primitive field") {
              it("responds with field changes for `replace` and `remove`") {
                val field = ArrayField("example-field", FieldType.INT)
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(field.copy(`type` = FieldType.STRING)))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  List(field)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("remove"),
                        "path" -> JsString("/0/items"),
                        "oldValue" -> JsString(field.items.toString)
                      ),
                      JsObject(
                        "operation" -> JsString("replace"),
                        "path" -> JsString("/0/type"),
                        "value" -> JsString(FieldType.STRING.toString),
                        "oldValue" -> JsString(field.`type`.toString)
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause there's a type change to record field") {
              it("responds with field changes for `replace` and `remove`") {
                val field = ArrayField("example-field", FieldType.INT)
                val recordField = RecordField(
                  "example-field",
                  List(
                    IntField("example-int-field")
                  )
                )

                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(
                    List(recordField)
                  )
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  List(field)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("add"),
                        "path" -> JsString("/0/fields"),
                        "value" -> JsArray(
                          JsObject(
                            "name" -> JsString("example-int-field"),
                            "type" -> JsString(FieldType.INT.toString),
                            "isIdentity" -> JsBoolean(false)
                          )
                        )
                      ),
                      JsObject(
                        "operation" -> JsString("remove"),
                        "path" -> JsString("/0/items"),
                        "oldValue" -> JsString(field.items.toString)
                      ),
                      JsObject(
                        "operation" -> JsString("replace"),
                        "path" -> JsString("/0/type"),
                        "value" -> JsString(FieldType.RECORD.toString),
                        "oldValue" -> JsString(field.`type`.toString)
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause there's an items type change") {
              it("responds with field changes for `replace`") {
                val field = ArrayField("example-field", FieldType.INT)
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(field.copy(items = FieldType.STRING)))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  List(field)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("replace"),
                        "path" -> JsString("/0/items"),
                        "value" -> JsString(FieldType.STRING.toString),
                        "oldValue" -> JsString(field.items.toString)
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }
          }

          describe("with record fields") {
            describe("that are the same") {
              it("responds with `isSameSchema` true") {
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(
                    List(
                      RecordField("example-field-1", List(StringField("example-string-field"))),
                      RecordField("example-field-2", List(IntField("example-int-field")))
                    )
                  )
                  .build()
                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  schemaRequest.fields
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(true),
                    "isMajorUpgradable" -> JsBoolean(false),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray()
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that are the same, but their fields are shuffled") {
              it("responds with `isSameSchema` true") {
                val requestRecordField = RecordField(
                  "example-field-1",
                  List(
                    StringField("example-string-field"),
                    IntField("example-int-field")
                  )
                )

                val dbRecordField = RecordField(
                  "example-field-1",
                  List(
                    IntField("example-int-field"),
                    StringField("example-string-field")
                  )
                )

                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(
                    List(requestRecordField)
                  )
                  .build()
                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  List(dbRecordField)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(true),
                    "isMajorUpgradable" -> JsBoolean(false),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray()
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ in a single primitive field") {
              it("responds with field changes for `add`") {
                val requestField = RecordField(
                  "example-record-field",
                  List(
                    IntField("example-int-field"),
                    IntField("additional-example-int-field")
                  )
                )

                val dbField = RecordField(
                  "example-record-field",
                  List(
                    IntField("example-int-field")
                  )
                )

                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(requestField))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  List(dbField)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(true),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("add"),
                        "path" -> JsString("/0/fields/0"),
                        "value" -> JsObject(
                          "name" -> JsString("additional-example-int-field"),
                          "type" -> JsString(FieldType.INT.toString),
                          "isIdentity" -> JsBoolean(false)
                        )
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ in a single array field") {
              it("responds with field changes for `add`") {
                val requestField = RecordField(
                  "example-record-field",
                  List(
                    IntField("example-int-field"),
                    ArrayField("additional-example-array-field", FieldType.INT)
                  )
                )

                val dbField = RecordField(
                  "example-record-field",
                  List(
                    IntField("example-int-field")
                  )
                )

                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(requestField))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  List(dbField)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(true),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("add"),
                        "path" -> JsString("/0/fields/0"),
                        "value" -> JsObject(
                          "name" -> JsString("additional-example-array-field"),
                          "type" -> JsString(FieldType.ARRAY.toString),
                          "isIdentity" -> JsBoolean(false),
                          "items" -> JsString(FieldType.INT.toString)
                        )
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ in a single record field") {
              it("responds with field changes for `add`") {
                val requestField = RecordField(
                  "example-record-field",
                  List(
                    RecordField(
                      "example-record-field",
                      List(
                        StringField("example-string-field"),
                        RecordField(
                          "example-nested-record-field",
                          List(
                            ArrayField("example-array-field", FieldType.INT),
                            IntField("example-int-field")
                          )
                        )
                      )
                    )
                  )
                )

                val dbField = RecordField(
                  "example-record-field",
                  List(
                    RecordField(
                      "example-record-field",
                      List(
                        StringField("example-string-field"),
                        RecordField(
                          "example-nested-record-field",
                          List(
                            ArrayField("example-array-field", FieldType.INT)
                          )
                        )
                      )
                    )
                  )
                )

                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(requestField))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion,
                  List(dbField)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(true),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(false),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("add"),
                        "path" -> JsString("/0/fields/0/fields/0/fields/-"),
                        "value" -> JsObject(
                          "name" -> JsString("example-int-field"),
                          "type" -> JsString(FieldType.INT.toString),
                          "isIdentity" -> JsBoolean(false)
                        )
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }
          }
        }

        describe("with existing schema matching by 1 minor version prior to `SchemaRequest`") {
          describe("with primitive fields") {
            describe("that are the same") {
              it("responds with `isSameSchema` true") {
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(
                    List(
                      IntField("example-field-1"),
                      StringField("example-field-2")
                    )
                  )
                  .build()
                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  schemaRequest.fields
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(true),
                    "isMajorUpgradable" -> JsBoolean(false),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray()
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that are the same but shuffled") {
              it("responds with `isSameSchema` true") {
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(
                    List(
                      StringField("example-field-2"),
                      IntField("example-field-1")
                    )
                  )
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  schemaRequest.fields
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(true),
                    "isMajorUpgradable" -> JsBoolean(false),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray()
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause they're duplicate") {
              it("responds with field changes for `add`") {
                val field = IntField("example-field")
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(field, field))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  List(field)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(true),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("add"),
                        "path" -> JsString("/-"),
                        "value" -> JsObject(
                          "name" -> JsString(field.name),
                          "type" -> JsString(field.`type`.toString),
                          "isIdentity" -> JsBoolean(false)
                        )
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause there's a name change") {
              it("responds with field changes for `replace`") {
                val field = IntField("example-field")
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(field))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  List(
                    field.copy(name = "old-example-field")
                  )
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("replace"),
                        "path" -> JsString("/0/name"),
                        "value" -> JsString(field.name),
                        "oldValue" -> JsString("old-example-field")
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause there's a type change") {
              it("responds with field changes for `replace`") {
                val field = IntField("example-field")
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(field))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  List(
                    field.copy(`type` = FieldType.STRING)
                  )
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("replace"),
                        "path" -> JsString("/0/type"),
                        "value" -> JsString(field.`type`.toString),
                        "oldValue" -> JsString(FieldType.STRING.toString)
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }
          }

          describe("with array fields") {
            describe("that are the same") {
              it("responds with `isSameSchema` true") {
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(
                    List(
                      ArrayField("example-field-1", FieldType.INT),
                      ArrayField("example-field-2", FieldType.STRING)
                    )
                  )
                  .build()
                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  schemaRequest.fields
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(true),
                    "isMajorUpgradable" -> JsBoolean(false),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray()
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that are the same but shuffled") {
              it("responds with `isSameSchema` true") {
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(
                    List(
                      ArrayField("example-field-2", FieldType.STRING),
                      ArrayField("example-field-1", FieldType.INT)
                    )
                  )
                  .build()
                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  schemaRequest.fields
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(true),
                    "isMajorUpgradable" -> JsBoolean(false),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray()
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause they're duplicate") {
              it("responds with field changes for `add`") {
                val field = ArrayField("example-field", FieldType.INT)
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(field, field))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  List(field)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(true),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("add"),
                        "path" -> JsString("/-"),
                        "value" -> JsObject(
                          "name" -> JsString(field.name),
                          "type" -> JsString(field.`type`.toString),
                          "isIdentity" -> JsBoolean(false),
                          "items" -> JsString(field.items.toString)
                        )
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause there's a name change") {
              it("responds with field changes for `replace`") {
                val field = ArrayField("example-field", FieldType.INT)
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(field, field))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  List(field)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(true),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("add"),
                        "path" -> JsString("/-"),
                        "value" -> JsObject(
                          "name" -> JsString(field.name),
                          "type" -> JsString(field.`type`.toString),
                          "isIdentity" -> JsBoolean(false),
                          "items" -> JsString(field.items.toString)
                        )
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause there's a type change to primitive field") {
              it("responds with field changes for `replace` and `remove`") {
                val field = ArrayField("example-field", FieldType.INT)
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(field.copy(`type` = FieldType.STRING)))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  List(field)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("remove"),
                        "path" -> JsString("/0/items"),
                        "oldValue" -> JsString(field.items.toString)
                      ),
                      JsObject(
                        "operation" -> JsString("replace"),
                        "path" -> JsString("/0/type"),
                        "value" -> JsString(FieldType.STRING.toString),
                        "oldValue" -> JsString(field.`type`.toString)
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause there's a type change to record field") {
              it("responds with field changes for `replace` and `remove`") {
                val field = ArrayField("example-field", FieldType.INT)
                val recordField = RecordField(
                  "example-field",
                  List(
                    IntField("example-int-field")
                  )
                )

                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(
                    List(recordField)
                  )
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  List(field)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("add"),
                        "path" -> JsString("/0/fields"),
                        "value" -> JsArray(
                          JsObject(
                            "name" -> JsString("example-int-field"),
                            "type" -> JsString(FieldType.INT.toString),
                            "isIdentity" -> JsBoolean(false)
                          )
                        )
                      ),
                      JsObject(
                        "operation" -> JsString("remove"),
                        "path" -> JsString("/0/items"),
                        "oldValue" -> JsString(field.items.toString)
                      ),
                      JsObject(
                        "operation" -> JsString("replace"),
                        "path" -> JsString("/0/type"),
                        "value" -> JsString(FieldType.RECORD.toString),
                        "oldValue" -> JsString(field.`type`.toString)
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ cause there's an items type change") {
              it("responds with field changes for `replace`") {
                val field = ArrayField("example-field", FieldType.INT)
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(field.copy(items = FieldType.STRING)))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  List(field)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("replace"),
                        "path" -> JsString("/0/items"),
                        "value" -> JsString(FieldType.STRING.toString),
                        "oldValue" -> JsString(field.items.toString)
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }
          }

          describe("with record fields") {
            describe("that are the same") {
              it("responds with `isSameSchema` true") {
                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(
                    List(
                      RecordField("example-field-1", List(StringField("example-string-field"))),
                      RecordField("example-field-2", List(IntField("example-int-field")))
                    )
                  )
                  .build()
                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  schemaRequest.fields
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(true),
                    "isMajorUpgradable" -> JsBoolean(false),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray()
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that are the same, but their fields are shuffled") {
              it("responds with `isSameSchema` true") {
                val requestRecordField = RecordField(
                  "example-field-1",
                  List(
                    StringField("example-string-field"),
                    IntField("example-int-field")
                  )
                )

                val dbRecordField = RecordField(
                  "example-field-1",
                  List(
                    IntField("example-int-field"),
                    StringField("example-string-field")
                  )
                )

                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(
                    List(requestRecordField)
                  )
                  .build()
                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  List(dbRecordField)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(true),
                    "isMajorUpgradable" -> JsBoolean(false),
                    "isMinorUpgradable" -> JsBoolean(false),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray()
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ in a single primitive field") {
              it("responds with field changes for `add`") {
                val requestField = RecordField(
                  "example-record-field",
                  List(
                    IntField("example-int-field"),
                    IntField("additional-example-int-field")
                  )
                )

                val dbField = RecordField(
                  "example-record-field",
                  List(
                    IntField("example-int-field")
                  )
                )

                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(requestField))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  List(dbField)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(true),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("add"),
                        "path" -> JsString("/0/fields/0"),
                        "value" -> JsObject(
                          "name" -> JsString("additional-example-int-field"),
                          "type" -> JsString(FieldType.INT.toString),
                          "isIdentity" -> JsBoolean(false)
                        )
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ in a single array field") {
              it("responds with field changes for `add`") {
                val requestField = RecordField(
                  "example-record-field",
                  List(
                    IntField("example-int-field"),
                    ArrayField("additional-example-array-field", FieldType.INT)
                  )
                )

                val dbField = RecordField(
                  "example-record-field",
                  List(
                    IntField("example-int-field")
                  )
                )

                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(requestField))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  List(dbField)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(true),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("add"),
                        "path" -> JsString("/0/fields/0"),
                        "value" -> JsObject(
                          "name" -> JsString("additional-example-array-field"),
                          "type" -> JsString(FieldType.ARRAY.toString),
                          "isIdentity" -> JsBoolean(false),
                          "items" -> JsString(FieldType.INT.toString)
                        )
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }

            describe("that differ in a single record field") {
              it("responds with field changes for `add`") {
                val requestField = RecordField(
                  "example-record-field",
                  List(
                    RecordField(
                      "example-record-field",
                      List(
                        StringField("example-string-field"),
                        RecordField(
                          "example-nested-record-field",
                          List(
                            ArrayField("example-array-field", FieldType.INT),
                            IntField("example-int-field")
                          )
                        )
                      )
                    )
                  )
                )

                val dbField = RecordField(
                  "example-record-field",
                  List(
                    RecordField(
                      "example-record-field",
                      List(
                        StringField("example-string-field"),
                        RecordField(
                          "example-nested-record-field",
                          List(
                            ArrayField("example-array-field", FieldType.INT)
                          )
                        )
                      )
                    )
                  )
                )

                val schemaRequest = SchemaRequestBuilder
                  .aSchemaRequest()
                  .withFields(List(requestField))
                  .build()

                val isCreated = schemaRepository.create(
                  schemaRequest.name,
                  schemaRequest.applicationId,
                  schemaRequest.majorVersion,
                  schemaRequest.minorVersion - 1,
                  List(dbField)
                )

                assert(isCreated)

                val future = Http().singleRequest(
                  HttpRequest(
                    HttpMethods.POST,
                    uri = apiUrl,
                    entity = HttpEntity(
                      ContentTypes.`application/json`,
                      serializationService.serialize(schemaRequest)
                    )
                  )
                )

                val responseEntity = HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "isSameSchema" -> JsBoolean(false),
                    "isMajorUpgradable" -> JsBoolean(true),
                    "isMinorUpgradable" -> JsBoolean(true),
                    "isMajorUpgrade" -> JsBoolean(false),
                    "isMinorUpgrade" -> JsBoolean(true),
                    "fieldChanges" -> JsArray(
                      JsObject(
                        "operation" -> JsString("add"),
                        "path" -> JsString("/0/fields/0/fields/0/fields/-"),
                        "value" -> JsObject(
                          "name" -> JsString("example-int-field"),
                          "type" -> JsString(FieldType.INT.toString),
                          "isIdentity" -> JsBoolean(false)
                        )
                      )
                    )
                  ).prettyPrint
                )

                val response = Await.result(future, 5 seconds)
                assert(response.status == StatusCodes.OK)
                assert(response.entity == responseEntity)
              }
            }
          }
        }
      }
    }
  }
}
