package com.sumup.integration.http.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.sumup.UtilsService
import com.sumup.dto.FieldType
import com.sumup.dto.fields.{EnumField, LongField, StringField}
import com.sumup.json.SchemaRegistryJsonProtocol.ShortObjectIdFormat
import com.sumup.serde.SerializationService
import com.sumup.storage.repositories.SchemaRepository
import org.scalatest.{BeforeAndAfterEach, DoNotDiscover, FunSpec}
import com.sumup.testutils.{DatabaseSpec, ObjectMother}
import com.sumup.testutils.builders.{SchemaBuilder, SchemaRequestBuilder}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import spray.json.DefaultJsonProtocol._

@DoNotDiscover
class SchemaHttpRouteSpec extends FunSpec with DatabaseSpec with BeforeAndAfterEach {
  import spray.json._
  implicit val system: ActorSystem = ActorSystem("SchemaHttpRouteSpec")
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

  def enumFieldToJsObject[A](field: EnumField[A]): JsObject = {
    val allowedValues = field.valueType match {
      case FieldType.STRING => field.asInstanceOf[EnumField[String]].allowedValues.toJson
      case FieldType.INT => field.asInstanceOf[EnumField[Int]].allowedValues.toJson
      case FieldType.LONG => field.asInstanceOf[EnumField[Long]].allowedValues.toJson
      case FieldType.DOUBLE => field.asInstanceOf[EnumField[Double]].allowedValues.toJson
      case FieldType.FLOAT => field.asInstanceOf[EnumField[Float]].allowedValues.toJson
    }

    JsObject(
      "name" -> JsString(field.name),
      "type" -> JsString(field.`type`.toString),
      "isIdentity" -> JsBoolean(field.isIdentity),
      "valueType" -> JsString(field.valueType.toString),
      "allowedValues" -> allowedValues
    )
  }

  def enumFieldToJsObjectWithoutIsIdentity[A](field: EnumField[A]): JsObject = {
    val allowedValues = field.valueType match {
      case FieldType.STRING => field.asInstanceOf[EnumField[String]].allowedValues.toJson
      case FieldType.INT => field.asInstanceOf[EnumField[Int]].allowedValues.toJson
      case FieldType.LONG => field.asInstanceOf[EnumField[Long]].allowedValues.toJson
      case FieldType.DOUBLE => field.asInstanceOf[EnumField[Double]].allowedValues.toJson
      case FieldType.FLOAT => field.asInstanceOf[EnumField[Float]].allowedValues.toJson
    }

    JsObject(
      "name" -> JsString(field.name),
      "type" -> JsString(field.`type`.toString),
      "valueType" -> JsString(field.valueType.toString),
      "allowedValues" -> allowedValues
    )
  }

  describe("POST /api/schema") {
    val apiUrl = s"$serverAddress/api/schema"

    describe("with incomplete `SchemaRequest` body") {
      it("returns an error and does not create a schema") {
        val name = "example-schema"
        val applicationId = "example-app"
        val majorVersion = 1
        val minorVersion = 3

        val entity = HttpEntity(
          ContentTypes.`application/json`,
          JsObject(
            "name" -> JsString(name),
            "applicationId" -> JsString(applicationId),
            "majorVersion" -> JsNumber(majorVersion),
            "minorVersion" -> JsNumber(minorVersion),
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

        assert(
          schemaRepository.getByNameAndVersion(name, majorVersion, minorVersion) == null
        )
      }
    }

    describe("with complete `SchemaRequest` body") {
      describe("and not at least one field specified in `fields`") {
        it("returns an error and does not create a schema") {
          val name = "example-schema"
          val applicationId = "example-app"
          val majorVersion = 1
          val minorVersion = 3

          val entity = HttpEntity(
            ContentTypes.`application/json`,
            JsObject(
              "name" -> JsString(name),
              "applicationId" -> JsString(applicationId),
              "majorVersion" -> JsNumber(majorVersion),
              "minorVersion" -> JsNumber(minorVersion),
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

          assert(
            schemaRepository.getByNameAndVersion(name, majorVersion, minorVersion) == null
          )
        }
      }

      describe("and at least one field specified in `fields`") {
        describe("but a schema already exists") {
          it("returns an error and does not create a schema") {
            val schemaRequest = ObjectMother.defaultSchemaRequest()
            assert(
              schemaRepository.create(
                schemaRequest.name,
                schemaRequest.applicationId,
                schemaRequest.majorVersion,
                schemaRequest.minorVersion,
                schemaRequest.fields
              )
            )

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
            assert(response.status == StatusCodes.BadRequest)
            assert(
              response.entity ==
                HttpEntity(
                  ContentTypes.`application/json`,
                  JsObject(
                    "code" -> JsNumber(400),
                    "type" -> JsString("Bad Request"),
                    "message" -> JsString("Schema already exists.")
                  ).prettyPrint
                )
            )

            assert(
              schemaRepository.getByNameAndVersion(
                schemaRequest.name,
                schemaRequest.majorVersion,
                schemaRequest.minorVersion
              ) != null
            )
          }
        }

        describe("and schema does not already exist") {
          describe("with specified `isIdentity` in fields") {
            it("creates a schema with fields' specified `isIdentity` and returns a 201 created response") {
              val name = "example-schema"
              val applicationId = "example-app"
              val majorVersion = 1
              val minorVersion = 1

              val aFieldName = "a-field"
              val aFieldType = FieldType.INT
              val aFieldIsIdentity = true

              val bFieldName = "b-field"
              val bFieldType = FieldType.STRING
              val bFieldIsIdentity = true

              val entity = HttpEntity(
                ContentTypes.`application/json`,
                JsObject(
                  "name" -> JsString(name),
                  "applicationId" -> JsString(applicationId),
                  "majorVersion" -> JsNumber(majorVersion),
                  "minorVersion" -> JsNumber(minorVersion),
                  "fields" -> JsArray(
                    JsObject(
                      "name" -> JsString(aFieldName),
                      "type" -> JsString(aFieldType.toString),
                      "isIdentity" -> JsBoolean(aFieldIsIdentity)
                    ),
                    JsObject(
                      "name" -> JsString(bFieldName),
                      "type" -> JsString(bFieldType.toString),
                      "isIdentity" -> JsBoolean(bFieldIsIdentity)
                    )
                  )
                ).prettyPrint
              )

              val future = Http().singleRequest(
                HttpRequest(
                  HttpMethods.POST,
                  uri = apiUrl,
                  entity = entity
                )
              )
              val response = Await.result(future, 5 seconds)
              assert(
                response.entity ==
                  HttpEntity(
                    ContentTypes.`application/json`,
                    JsObject(
                      "code" -> JsNumber(201),
                      "type" -> JsString("Created"),
                      "message" -> JsString("The request has been fulfilled and resulted in a new resource being created.")
                    ).prettyPrint
                  )
              )
              assert(response.status == StatusCodes.Created)

              val schema = schemaRepository.getByNameAndVersion(name, majorVersion, minorVersion)
              assert(schema != null)
              assert(schema.fields.length == 2)
              val createdAfield = schema.fields.head
              assert(createdAfield.name == aFieldName)
              assert(createdAfield.`type` == aFieldType)
              assert(createdAfield.isIdentity == aFieldIsIdentity)

              val createdBfield = schema.fields.last
              assert(createdBfield.name == bFieldName)
              assert(createdBfield.`type` == bFieldType)
              assert(createdBfield.isIdentity == bFieldIsIdentity)
            }

            it("creates a schema with enum fields' specified `isIdentity` and returns a 201 created response") {
              val name = "example-schema"
              val applicationId = "example-app"
              val majorVersion = 1
              val minorVersion = 1

              val aField = EnumField(name, List(1, 2), FieldType.INT, isIdentity = true)
              val bField = EnumField(name, List("test", "test2"), FieldType.STRING, isIdentity = true)
              val cField = EnumField(name, List(10.1000.toFloat, 20.2000.toFloat), FieldType.FLOAT, isIdentity = true)
              val dField = EnumField(name, List(10.1000, 20.2000), FieldType.DOUBLE, isIdentity = true)
              val eField = EnumField(name, List(1000000000L, 2000000000L), FieldType.LONG, isIdentity = true)

              val entity = HttpEntity(
                ContentTypes.`application/json`,
                JsObject(
                  "name" -> JsString(name),
                  "applicationId" -> JsString(applicationId),
                  "majorVersion" -> JsNumber(majorVersion),
                  "minorVersion" -> JsNumber(minorVersion),
                  "fields" -> JsArray(
                    enumFieldToJsObject[Int](aField),
                    enumFieldToJsObject[String](bField),
                    enumFieldToJsObject[Float](cField),
                    enumFieldToJsObject[Double](dField),
                    enumFieldToJsObject[Long](eField)
                  )
                ).prettyPrint
              )

              val future = Http().singleRequest(
                HttpRequest(
                  HttpMethods.POST,
                  uri = apiUrl,
                  entity = entity
                )
              )
              val response = Await.result(future, 5 seconds)
              assert(
                response.entity ==
                  HttpEntity(
                    ContentTypes.`application/json`,
                    JsObject(
                      "code" -> JsNumber(201),
                      "type" -> JsString("Created"),
                      "message" -> JsString("The request has been fulfilled and resulted in a new resource being created.")
                    ).prettyPrint
                  )
              )
              assert(response.status == StatusCodes.Created)

              val schema = schemaRepository.getByNameAndVersion(name, majorVersion, minorVersion)
              assert(schema != null)
              assert(schema.fields.length == 5)
              val createdAField = schema.fields.head
              assert(createdAField.name == aField.name)
              assert(createdAField.`type` == aField.`type`)
              assert(createdAField.isIdentity == aField.isIdentity)
              assert(createdAField.asInstanceOf[EnumField[Int]].valueType == aField.valueType)
              assert(createdAField.asInstanceOf[EnumField[Int]].allowedValues == aField.allowedValues)

              val createdBField = schema.fields(1)
              assert(createdBField.name == bField.name)
              assert(createdBField.`type` == bField.`type`)
              assert(createdBField.isIdentity == bField.isIdentity)
              assert(createdBField.asInstanceOf[EnumField[String]].valueType == bField.valueType)
              assert(createdBField.asInstanceOf[EnumField[String]].allowedValues == bField.allowedValues)

              val createdCField = schema.fields(2)
              assert(createdCField.name == cField.name)
              assert(createdCField.`type` == cField.`type`)
              assert(createdCField.isIdentity == cField.isIdentity)
              assert(createdCField.asInstanceOf[EnumField[Float]].valueType == cField.valueType)
              assert(createdCField.asInstanceOf[EnumField[Float]].allowedValues == cField.allowedValues)

              val createdDField = schema.fields(3)
              assert(createdDField.name == dField.name)
              assert(createdDField.`type` == dField.`type`)
              assert(createdDField.isIdentity == dField.isIdentity)
              assert(createdDField.asInstanceOf[EnumField[Double]].valueType == dField.valueType)
              assert(createdDField.asInstanceOf[EnumField[Double]].allowedValues == dField.allowedValues)

              val createdEField = schema.fields(4)
              assert(createdEField.name == eField.name)
              assert(createdEField.`type` == eField.`type`)
              assert(createdEField.isIdentity == eField.isIdentity)
              assert(createdEField.asInstanceOf[EnumField[Long]].valueType == eField.valueType)
              assert(createdEField.asInstanceOf[EnumField[Long]].allowedValues == eField.allowedValues)
            }

            it("fails with enum fields' specified `isIdentity` and not given valueType") {
              val name = "example-schema"
              val applicationId = "example-app"
              val majorVersion = 1
              val minorVersion = 1

              val field = EnumField(name, List(1, 2), FieldType.INT, isIdentity = true)

              val entity = HttpEntity(
                ContentTypes.`application/json`,
                JsObject(
                  "name" -> JsString(name),
                  "applicationId" -> JsString(applicationId),
                  "majorVersion" -> JsNumber(majorVersion),
                  "minorVersion" -> JsNumber(minorVersion),
                  "fields" -> JsArray(
                    JsObject(
                      "name" -> JsString(field.name),
                      "type" -> JsString(field.`type`.toString),
                      "isIdentity" -> JsBoolean(field.isIdentity),
                      "allowedValues" -> field.allowedValues.toJson
                    )
                  )
                ).prettyPrint
              )

              val future = Http().singleRequest(
                HttpRequest(
                  HttpMethods.POST,
                  uri = apiUrl,
                  entity = entity
                )
              )
              val response = Await.result(future, 5 seconds)
              assert(
                response.entity ==
                  HttpEntity(
                    ContentTypes.`text/plain(UTF-8)`,
                    "The request content was malformed:\n`valueType` is blank/empty"
                  )
              )
              assert(response.status == StatusCodes.BadRequest)
            }

            it("fails with enum fields' specified `isIdentity` and not given allowedValues") {
              val name = "example-schema"
              val applicationId = "example-app"
              val majorVersion = 1
              val minorVersion = 1

              val field = EnumField(name, List(1, 2), FieldType.INT, isIdentity = true)

              val entity = HttpEntity(
                ContentTypes.`application/json`,
                JsObject(
                  "name" -> JsString(name),
                  "applicationId" -> JsString(applicationId),
                  "majorVersion" -> JsNumber(majorVersion),
                  "minorVersion" -> JsNumber(minorVersion),
                  "fields" -> JsArray(
                    JsObject(
                      "name" -> JsString(field.name),
                      "type" -> JsString(field.`type`.toString),
                      "isIdentity" -> JsBoolean(field.isIdentity),
                      "valueType" -> JsString(field.valueType.toString)
                    )
                  )
                ).prettyPrint
              )

              val future = Http().singleRequest(
                HttpRequest(
                  HttpMethods.POST,
                  uri = apiUrl,
                  entity = entity
                )
              )
              val response = Await.result(future, 5 seconds)
              assert(
                response.entity ==
                  HttpEntity(
                    ContentTypes.`text/plain(UTF-8)`,
                    "The request content was malformed:\n`allowedValues` is blank/empty"
                  )
              )
              assert(response.status == StatusCodes.BadRequest)
            }
          }

          describe("with no `isIdentity` in fields") {
            it("creates a schema with fields' default `isIdentity` and returns a 201 created response") {
              val schemaRequest = SchemaRequestBuilder.aSchemaRequest().withMinorVersion(1).build()

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
              assert(response.status == StatusCodes.Created)
              assert(
                response.entity ==
                  HttpEntity(
                    ContentTypes.`application/json`,
                    JsObject(
                      "code" -> JsNumber(201),
                      "type" -> JsString("Created"),
                      "message" -> JsString("The request has been fulfilled and resulted in a new resource being created.")
                    ).prettyPrint
                  )
              )

              val schema = schemaRepository.getByNameAndVersion(schemaRequest.name, schemaRequest.majorVersion, schemaRequest.minorVersion)
              assert(schema != null)
              schema.fields.foreach(f => assert(!f.isIdentity))
            }

            it("creates a schema with enum fields' default `isIdentity` and returns a 201 created response")  {
              val name = "example-schema"
              val applicationId = "example-app"
              val majorVersion = 1
              val minorVersion = 1

              val aField = EnumField(name, List(1, 2), FieldType.INT)
              val bField = EnumField(name, List("test", "test2"), FieldType.STRING)
              val cField = EnumField(name, List(10.1000.toFloat, 20.2000.toFloat), FieldType.FLOAT)
              val dField = EnumField(name, List(10.1000, 20.2000), FieldType.DOUBLE)
              val eField = EnumField(name, List(1000000000L, 2000000000L), FieldType.LONG)

              val entity = HttpEntity(
                ContentTypes.`application/json`,
                JsObject(
                  "name" -> JsString(name),
                  "applicationId" -> JsString(applicationId),
                  "majorVersion" -> JsNumber(majorVersion),
                  "minorVersion" -> JsNumber(minorVersion),
                  "fields" -> JsArray(
                    enumFieldToJsObjectWithoutIsIdentity[Int](aField),
                    enumFieldToJsObjectWithoutIsIdentity[String](bField),
                    enumFieldToJsObjectWithoutIsIdentity[Float](cField),
                    enumFieldToJsObjectWithoutIsIdentity[Double](dField),
                    enumFieldToJsObjectWithoutIsIdentity[Long](eField)
                  )
                ).prettyPrint
              )

              val future = Http().singleRequest(
                HttpRequest(
                  HttpMethods.POST,
                  uri = apiUrl,
                  entity = entity
                )
              )
              val response = Await.result(future, 5 seconds)
              assert(
                response.entity ==
                  HttpEntity(
                    ContentTypes.`application/json`,
                    JsObject(
                      "code" -> JsNumber(201),
                      "type" -> JsString("Created"),
                      "message" -> JsString("The request has been fulfilled and resulted in a new resource being created.")
                    ).prettyPrint
                  )
              )
              assert(response.status == StatusCodes.Created)

              val schema = schemaRepository.getByNameAndVersion(name, majorVersion, minorVersion)
              assert(schema != null)
              assert(schema.fields.length == 5)
              val createdAField = schema.fields.head
              assert(createdAField.name == aField.name)
              assert(createdAField.`type` == aField.`type`)
              assert(!createdAField.isIdentity)
              assert(createdAField.asInstanceOf[EnumField[Int]].valueType == aField.valueType)
              assert(createdAField.asInstanceOf[EnumField[Int]].allowedValues == aField.allowedValues)

              val createdBField = schema.fields(1)
              assert(createdBField.name == bField.name)
              assert(createdBField.`type` == bField.`type`)
              assert(!createdBField.isIdentity)
              assert(createdBField.asInstanceOf[EnumField[String]].valueType == bField.valueType)
              assert(createdBField.asInstanceOf[EnumField[String]].allowedValues == bField.allowedValues)

              val createdCField = schema.fields(2)
              assert(createdCField.name == cField.name)
              assert(createdCField.`type` == cField.`type`)
              assert(!createdCField.isIdentity)
              assert(createdCField.asInstanceOf[EnumField[Float]].valueType == cField.valueType)
              assert(createdCField.asInstanceOf[EnumField[Float]].allowedValues == cField.allowedValues)

              val createdDField = schema.fields(3)
              assert(createdDField.name == dField.name)
              assert(createdDField.`type` == dField.`type`)
              assert(!createdDField.isIdentity)
              assert(createdDField.asInstanceOf[EnumField[Double]].valueType == dField.valueType)
              assert(createdDField.asInstanceOf[EnumField[Double]].allowedValues == dField.allowedValues)

              val createdEField = schema.fields(4)
              assert(createdEField.name == eField.name)
              assert(createdEField.`type` == eField.`type`)
              assert(!createdEField.isIdentity)
              assert(createdEField.asInstanceOf[EnumField[Long]].valueType == eField.valueType)
              assert(createdEField.asInstanceOf[EnumField[Long]].allowedValues == eField.allowedValues)

            }

            it("fails with enum fields' default `isIdentity` and not given valueType") {
              val name = "example-schema"
              val applicationId = "example-app"
              val majorVersion = 1
              val minorVersion = 1

              val field = EnumField(name, List(1, 2), FieldType.INT, isIdentity = true)

              val entity = HttpEntity(
                ContentTypes.`application/json`,
                JsObject(
                  "name" -> JsString(name),
                  "applicationId" -> JsString(applicationId),
                  "majorVersion" -> JsNumber(majorVersion),
                  "minorVersion" -> JsNumber(minorVersion),
                  "fields" -> JsArray(
                    JsObject(
                      "name" -> JsString(field.name),
                      "type" -> JsString(field.`type`.toString),
                      "allowedValues" -> field.allowedValues.toJson
                    )
                  )
                ).prettyPrint
              )

              val future = Http().singleRequest(
                HttpRequest(
                  HttpMethods.POST,
                  uri = apiUrl,
                  entity = entity
                )
              )
              val response = Await.result(future, 5 seconds)
              assert(
                response.entity ==
                  HttpEntity(
                    ContentTypes.`text/plain(UTF-8)`,
                    "The request content was malformed:\n`valueType` is blank/empty"
                  )
              )
              assert(response.status == StatusCodes.BadRequest)
            }

            it("fails with enum fields' default `isIdentity` and not given allowedValues") {
              val name = "example-schema"
              val applicationId = "example-app"
              val majorVersion = 1
              val minorVersion = 1

              val field = EnumField(name, List(1, 2), FieldType.INT, isIdentity = true)

              val entity = HttpEntity(
                ContentTypes.`application/json`,
                JsObject(
                  "name" -> JsString(name),
                  "applicationId" -> JsString(applicationId),
                  "majorVersion" -> JsNumber(majorVersion),
                  "minorVersion" -> JsNumber(minorVersion),
                  "fields" -> JsArray(
                    JsObject(
                      "name" -> JsString(field.name),
                      "type" -> JsString(field.`type`.toString),
                      "valueType" -> JsString(field.valueType.toString)
                    )
                  )
                ).prettyPrint
              )

              val future = Http().singleRequest(
                HttpRequest(
                  HttpMethods.POST,
                  uri = apiUrl,
                  entity = entity
                )
              )
              val response = Await.result(future, 5 seconds)
              assert(
                response.entity ==
                  HttpEntity(
                    ContentTypes.`text/plain(UTF-8)`,
                    "The request content was malformed:\n`allowedValues` is blank/empty"
                  )
              )
              assert(response.status == StatusCodes.BadRequest)
            }
          }
        }
      }
    }
  }

  describe("GET /api/schema/{name}") {
    val apiUrl = s"$serverAddress/api/schema"

    describe("with non-existing schemas by `name`") {
      it("responds with not found response") {
        val otherSchema = SchemaBuilder.aSchema().withName("different-name").build()
        assert(
          schemaRepository.create(
            otherSchema.name,
            otherSchema.applicationId,
            otherSchema.majorVersion,
            otherSchema.minorVersion,
            otherSchema.fields
          )
        )

        val future = Http().singleRequest(
          HttpRequest(
            HttpMethods.GET,
            uri = apiUrl + "/unicorn-schema"
          )
        )

        val response = Await.result(future, 5 seconds)
        assert(response.status == StatusCodes.NotFound)
        assert(
          response.entity == HttpEntity(
            ContentTypes.`application/json`,
            JsObject(
              "code" -> JsNumber(404),
              "type" -> JsString("Not Found"),
              "message" -> JsString("The requested resource could not be found but may be available again in the future.")
            ).prettyPrint
          )
        )
      }
    }

    describe("with one existing schema by `name`") {
      it("returns the schema in a json array") {
        val otherSchema = SchemaBuilder.aSchema().withName("different-name").build()
        val field = StringField("example-field")
        val schema = SchemaBuilder.aSchema().withFields(List(field)).build()

        assert(
          schemaRepository.create(
            otherSchema.name,
            otherSchema.applicationId,
            otherSchema.majorVersion,
            otherSchema.minorVersion,
            otherSchema.fields
          )
        )
        assert(
          schemaRepository.create(
            schema.name,
            schema.applicationId,
            schema.majorVersion,
            schema.minorVersion,
            schema.fields
          )
        )

        val future = Http().singleRequest(
          HttpRequest(
            HttpMethods.GET,
            uri = s"$apiUrl/${schema.name}"
          )
        )

        val response = Await.result(future, 5 seconds)
        assert(response.status == StatusCodes.OK)
        assert(
          response.entity == HttpEntity(
            ContentTypes.`application/json`,
            JsArray(
              JsObject(
                "name" -> JsString(schema.name),
                "applicationId" -> JsString(schema.applicationId),
                "majorVersion" -> JsNumber(schema.majorVersion),
                "_id" -> ShortObjectIdFormat.write(
                  schemaRepository.getByNameAndVersion(
                    schema.name, schema.majorVersion, schema.minorVersion
                  )._id.get
                ),
                "fields" -> JsArray(
                  JsObject(
                    "name" -> JsString(field.name),
                    "type" -> JsString(field.`type`.toString),
                    "isIdentity" -> JsBoolean(field.isIdentity)
                  )
                ),
                "minorVersion" -> JsNumber(schema.minorVersion)
              )
            ).prettyPrint
          )
        )
      }
    }

    describe("with many existing schemas by `name`") {
      it("returns the schemas in a json array") {
        val otherSchema = SchemaBuilder.aSchema().withName("different-name").build()
        val field = StringField("example-field")
        val schemaOne = SchemaBuilder.aSchema().withMajorVersion(1).withFields(List(field)).build()
        val schemaTwo = SchemaBuilder.aSchema().withMajorVersion(2).withFields(List(field)).build()
        val schemaThree = SchemaBuilder.aSchema().withMajorVersion(3).withFields(List(field)).build()

        for (schema <- List(otherSchema, schemaOne, schemaTwo, schemaThree)) {
          assert(
            schemaRepository.create(
              schema.name,
              schema.applicationId,
              schema.majorVersion,
              schema.minorVersion,
              schema.fields
            )
          )
        }

        val future = Http().singleRequest(
          HttpRequest(
            HttpMethods.GET,
            uri = s"$apiUrl/${schemaOne.name}"
          )
        )

        val response = Await.result(future, 5 seconds)
        assert(response.status == StatusCodes.OK)
        assert(
          response.entity == HttpEntity(
            ContentTypes.`application/json`,
            JsArray(
              List(schemaOne, schemaTwo, schemaThree).map { schema =>
                JsObject(
                  "name" -> JsString(schema.name),
                  "applicationId" -> JsString(schema.applicationId),
                  "majorVersion" -> JsNumber(schema.majorVersion),
                  "_id" -> ShortObjectIdFormat.write(
                    schemaRepository.getByNameAndVersion(
                      schema.name, schema.majorVersion, schema.minorVersion
                    )._id.get
                  ),
                  "fields" -> JsArray(
                    JsObject(
                      "name" -> JsString(field.name),
                      "type" -> JsString(field.`type`.toString),
                      "isIdentity" -> JsBoolean(field.isIdentity)
                    )
                  ),
                  "minorVersion" -> JsNumber(schema.minorVersion)
                )
              }.toVector
            ).prettyPrint
          )
        )
      }
    }
  }

  describe("GET /api/schema/{name}/major_version/{major_version}/minor_version/{minor_version}") {
    val apiUrl = s"$serverAddress/api/schema/%s/major_version/%d/minor_version/%d"

    describe("with schemas not matching by name") {
      it("responds with not found response") {
        val otherSchema = SchemaBuilder.aSchema().withName("different-name").build()
        assert(
          schemaRepository.create(
            otherSchema.name,
            otherSchema.applicationId,
            otherSchema.majorVersion,
            otherSchema.minorVersion,
            otherSchema.fields
          )
        )

        val future = Http().singleRequest(
          HttpRequest(
            HttpMethods.GET,
            uri = apiUrl.format("not-your-schema", 19, 20)
          )
        )

        val response = Await.result(future, 5 seconds)
        assert(response.status == StatusCodes.NotFound)
        assert(
          response.entity == HttpEntity(
            ContentTypes.`application/json`,
            JsObject(
              "code" -> JsNumber(404),
              "type" -> JsString("Not Found"),
              "message" -> JsString("The requested resource could not be found but may be available again in the future.")
            ).prettyPrint
          )
        )
      }
    }

    describe("with schemas matching by name") {
      describe("but not by major version") {
        it("responds with not found response") {
          val otherSchema = SchemaBuilder.aSchema().withName("different-name").build()
          assert(
            schemaRepository.create(
              otherSchema.name,
              otherSchema.applicationId,
              otherSchema.majorVersion,
              otherSchema.minorVersion,
              otherSchema.fields
            )
          )

          val future = Http().singleRequest(
            HttpRequest(
              HttpMethods.GET,
              uri = apiUrl.format(otherSchema.name, 19, 20)
            )
          )

          val response = Await.result(future, 5 seconds)
          assert(response.status == StatusCodes.NotFound)
          assert(
            response.entity == HttpEntity(
              ContentTypes.`application/json`,
              JsObject(
                "code" -> JsNumber(404),
                "type" -> JsString("Not Found"),
                "message" -> JsString("The requested resource could not be found but may be available again in the future.")
              ).prettyPrint
            )
          )
        }
      }

      describe("and matching by major version") {
        describe("but not matching by minor version")  {
          it("responds with not found response") {
            val otherSchema = SchemaBuilder.aSchema().withName("different-name").build()
            assert(
              schemaRepository.create(
                otherSchema.name,
                otherSchema.applicationId,
                otherSchema.majorVersion,
                otherSchema.minorVersion,
                otherSchema.fields
              )
            )

            val future = Http().singleRequest(
              HttpRequest(
                HttpMethods.GET,
                uri = apiUrl.format(otherSchema.name, otherSchema.majorVersion, 20)
              )
            )

            val response = Await.result(future, 5 seconds)
            assert(response.status == StatusCodes.NotFound)
            assert(
              response.entity == HttpEntity(
                ContentTypes.`application/json`,
                JsObject(
                  "code" -> JsNumber(404),
                  "type" -> JsString("Not Found"),
                  "message" -> JsString("The requested resource could not be found but may be available again in the future.")
                ).prettyPrint
              )
            )
          }
        }

        describe("and matching by minor version") {
          it("returns the schema matched") {
            val field = LongField("example-field")
            val schema = SchemaBuilder.aSchema().withFields(List(field)).build()
            assert(
              schemaRepository.create(
                schema.name,
                schema.applicationId,
                schema.majorVersion,
                schema.minorVersion,
                schema.fields
              )
            )

            val future = Http().singleRequest(
              HttpRequest(
                HttpMethods.GET,
                uri = apiUrl.format(schema.name, schema.majorVersion, schema.minorVersion)
              )
            )

            val response = Await.result(future, 5 seconds)
            assert(response.status == StatusCodes.OK)
            assert(
              response.entity == HttpEntity(
                ContentTypes.`application/json`,
                JsObject(
                  "name" -> JsString(schema.name),
                  "applicationId" -> JsString(schema.applicationId),
                  "majorVersion" -> JsNumber(schema.majorVersion),
                  "_id" -> ShortObjectIdFormat.write(
                    schemaRepository.getByNameAndVersion(
                      schema.name, schema.majorVersion, schema.minorVersion
                    )._id.get
                  ),
                  "fields" -> JsArray(
                    JsObject(
                      "name" -> JsString(field.name),
                      "type" -> JsString(field.`type`.toString),
                      "isIdentity" -> JsBoolean(field.isIdentity)
                    )
                  ),
                  "minorVersion" -> JsNumber(schema.minorVersion)
                ).prettyPrint
              )
            )
          }
        }
      }
    }
  }

  describe("DELETE /api/schema/{name}/major_version/{major_version}/minor_version/{minor_version}") {
    val apiUrl = s"$serverAddress/api/schema/%s/major_version/%d/minor_version/%d"

    describe("with schemas not matching by name") {
      it("responds with not found response and does not delete the schema") {
        val otherSchema = SchemaBuilder.aSchema().withName("different-name").build()
        assert(
          schemaRepository.create(
            otherSchema.name,
            otherSchema.applicationId,
            otherSchema.majorVersion,
            otherSchema.minorVersion,
            otherSchema.fields
          )
        )

        val future = Http().singleRequest(
          HttpRequest(
            HttpMethods.DELETE,
            uri = apiUrl.format("not-your-schema", 19, 20)
          )
        )

        val response = Await.result(future, 5 seconds)
        assert(response.status == StatusCodes.NotFound)
        assert(
          response.entity == HttpEntity(
            ContentTypes.`application/json`,
            JsObject(
              "code" -> JsNumber(404),
              "type" -> JsString("Not Found"),
              "message" -> JsString("The requested resource could not be found but may be available again in the future.")
            ).prettyPrint
          )
        )

        assert(
          schemaRepository.getByNameAndVersion(otherSchema.name, otherSchema.majorVersion, otherSchema.minorVersion) != null
        )
      }
    }

    describe("with schemas matching by name") {
      describe("but not by major version") {
        it("responds with not found response and does not delete the schema") {
          val otherSchema = SchemaBuilder.aSchema().withName("different-name").build()
          assert(
            schemaRepository.create(
              otherSchema.name,
              otherSchema.applicationId,
              otherSchema.majorVersion,
              otherSchema.minorVersion,
              otherSchema.fields
            )
          )

          val future = Http().singleRequest(
            HttpRequest(
              HttpMethods.DELETE,
              uri = apiUrl.format(otherSchema.name, 19, 20)
            )
          )

          val response = Await.result(future, 5 seconds)
          assert(response.status == StatusCodes.NotFound)
          assert(
            response.entity == HttpEntity(
              ContentTypes.`application/json`,
              JsObject(
                "code" -> JsNumber(404),
                "type" -> JsString("Not Found"),
                "message" -> JsString("The requested resource could not be found but may be available again in the future.")
              ).prettyPrint
            )
          )
          assert(
            schemaRepository.getByNameAndVersion(otherSchema.name, otherSchema.majorVersion, otherSchema.minorVersion) != null
          )
        }
      }

      describe("and matching by major version") {
        describe("but not matching by minor version")  {
          it("responds with not found response and does not delete the schema") {
            val otherSchema = SchemaBuilder.aSchema().withName("different-name").build()
            assert(
              schemaRepository.create(
                otherSchema.name,
                otherSchema.applicationId,
                otherSchema.majorVersion,
                otherSchema.minorVersion,
                otherSchema.fields
              )
            )

            val future = Http().singleRequest(
              HttpRequest(
                HttpMethods.DELETE,
                uri = apiUrl.format(otherSchema.name, otherSchema.majorVersion, 20)
              )
            )

            val response = Await.result(future, 5 seconds)
            assert(response.status == StatusCodes.NotFound)
            assert(
              response.entity == HttpEntity(
                ContentTypes.`application/json`,
                JsObject(
                  "code" -> JsNumber(404),
                  "type" -> JsString("Not Found"),
                  "message" -> JsString("The requested resource could not be found but may be available again in the future.")
                ).prettyPrint
              )
            )

            assert(
              schemaRepository.getByNameAndVersion(otherSchema.name, otherSchema.majorVersion, otherSchema.minorVersion) != null
            )
          }
        }

        describe("and matching by minor version") {
          it("deletes the schema matched") {
            val field = LongField("example-field")
            var schema = SchemaBuilder.aSchema().withFields(List(field)).build()
            assert(
              schemaRepository.create(
                schema.name,
                schema.applicationId,
                schema.majorVersion,
                schema.minorVersion,
                schema.fields
              )
            )
            schema = schemaRepository.getByNameAndVersion(schema.name, schema.majorVersion, schema.minorVersion)

            val future = Http().singleRequest(
              HttpRequest(
                HttpMethods.DELETE,
                uri = apiUrl.format(schema.name, schema.majorVersion, schema.minorVersion)
              )
            )

            val response = Await.result(future, 5 seconds)
            assert(response.status == StatusCodes.OK)
            assert(
              schemaRepository.getByNameAndVersion(schema.name, schema.majorVersion, schema.minorVersion) == null
            )

            assert(
              response.entity ==
              HttpEntity(
                ContentTypes.`application/json`,
                JsObject(
                  "name" -> JsString(schema.name),
                  "applicationId" -> JsString(schema.applicationId),
                  "majorVersion" -> JsNumber(schema.majorVersion),
                  "_id" -> ShortObjectIdFormat.write(schema._id.get),
                  "fields" -> JsArray(
                    JsObject(
                      "name" -> JsString(field.name),
                      "type" -> JsString(field.`type`.toString),
                      "isIdentity" -> JsBoolean(field.isIdentity)
                    )
                  ),
                  "minorVersion" -> JsNumber(schema.minorVersion)
                ).prettyPrint
              )
            )
          }
        }
      }
    }
  }
}
