package com.sumup.integration.http.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.sumup.UtilsService
import com.sumup.dto.responses.standard.NotFoundResponse
import com.sumup.json.SchemaRegistryJsonProtocol.ShortObjectIdFormat
import com.sumup.serde.SerializationService
import com.sumup.storage.repositories.{ConsumerRepository, SchemaRepository}
import com.sumup.testutils.builders.{ConsumerBuilder, SchemaBuilder}
import com.sumup.testutils.{DatabaseSpec, ObjectMother}
import org.scalatest.{BeforeAndAfterEach, DoNotDiscover, FunSpec}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

@DoNotDiscover
class ConsumerHttpRouteSpec extends FunSpec with DatabaseSpec with BeforeAndAfterEach {

  import spray.json._

  implicit val system: ActorSystem = ActorSystem("ConsumerHttpRouteSpec")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  override implicit var ec: ExecutionContext = system.dispatcher
  val serverAddress = s"http://${config.getString("application.http.address")}:${config.getInt("application.http.port")}"

  override def beforeEach(): Unit = {
    cleanDatabase()
    super.beforeEach()
  }

  override def afterEach: Unit = cleanDatabase()

  val schemaRepository = new SchemaRepository()(mongoClientWrapper = mongoClientWrapper)
  val consumerRepository = new ConsumerRepository()(mongoClientWrapper = mongoClientWrapper)
  val utilsService = new UtilsService
  val serializationService = new SerializationService()(utilsService = utilsService)

  describe("GET /api/consumer/{name}") {
    val apiUrl = s"$serverAddress/api/consumer/%s"

    describe("with no consumers matching `name`") {
      it("returns a not found response") {
        val otherConsumer = ObjectMother.defaultConsumer()
        assert(
          consumerRepository.create(
            otherConsumer.name,
            otherConsumer.schemaName,
            otherConsumer.schemaMajorVersion,
            otherConsumer.schemaMinorVersion
          )
        )

        val future = Http().singleRequest(
          HttpRequest(
            HttpMethods.GET,
            uri = apiUrl.format("unicorn-consumer")
          )
        )

        val response = Await.result(future, 5 seconds)
        assert(response.status == StatusCodes.NotFound)
        assert(
          response.entity == HttpEntity(
            ContentTypes.`application/json`,
            serializationService.serialize(NotFoundResponse())
          )
        )
      }
    }

    describe("with consumers matching `name") {
      it("returns all matching consumers") {
        val otherConsumer = ObjectMother.defaultConsumer()
        val consumerOne = ConsumerBuilder.aConsumer().withName("consumer-1-to-be-found").build()
        for (consumer <- List(otherConsumer, consumerOne)) {
          assert(
            consumerRepository.create(
              consumer.name,
              consumer.schemaName,
              consumer.schemaMajorVersion,
              consumer.schemaMinorVersion
            )
          )
        }

        val future = Http().singleRequest(
          HttpRequest(
            HttpMethods.GET,
            uri = apiUrl.format(consumerOne.name)
          )
        )

        val dbConsumer = consumerRepository.getByNameAndSchema(
          consumerOne.name,
          consumerOne.schemaName,
          consumerOne.schemaMajorVersion,
          consumerOne.schemaMinorVersion
        )

        val responseEntity = HttpEntity(
          ContentTypes.`application/json`,
          JsArray(
            JsObject(
              "_id" -> ShortObjectIdFormat.write(dbConsumer._id.get),
              "name" -> JsString(consumerOne.name),
              "schemaName" -> JsString(consumerOne.schemaName),
              "schemaMajorVersion" -> JsNumber(consumerOne.schemaMajorVersion),
              "schemaMinorVersion" -> JsNumber(consumerOne.schemaMinorVersion)
            )
          ).prettyPrint
        )

        val response = Await.result(future, 5 seconds)
        assert(response.status == StatusCodes.OK)
        assert(response.entity == responseEntity)
      }
    }
  }

  describe("GET /api/consumer/{name}/schema_name/{schemaName}/schema_major_version/{schemaMajorVersion}/schema_minor_version/{schemaMinorVersion}") {
    val apiUrl = s"$serverAddress/api/consumer/%s/schema_name/%s/schema_major_version/%s/schema_minor_version/%s"

    describe("with consumer not matching by `name`") {
      it("returns not found response") {
        assert(
          consumerRepository.create(
            "some-name",
            "some-schema-name",
            10,
            11
          )
        )

        val future = Http().singleRequest(
          HttpRequest(
            HttpMethods.GET,
            uri = apiUrl.format("non-existing-name", "non-existing-schema-name", 1, 1)
          )
        )

        val responseEntity = HttpEntity(
          ContentTypes.`application/json`,
          JsObject(
            "code" -> JsNumber(404),
            "type" -> JsString("Not Found"),
            "message" -> JsString(
              "The requested resource could not be found but may be available again in the future."
            )
          ).prettyPrint
        )

        val response = Await.result(future, 5 seconds)
        assert(response.status == StatusCodes.NotFound)
        assert(response.entity == responseEntity)
      }
    }

    describe("with consumer matching by `name`") {
      describe("but not matching by `schema_name`") {
        it("returns not found response") {
          val name = "found-consumer"
          val schemaName = "found-consumer-schema"

          assert(
            consumerRepository.create(
              name,
              schemaName,
              10,
              11
            )
          )

          val future = Http().singleRequest(
            HttpRequest(
              HttpMethods.GET,
              uri = apiUrl.format(name, schemaName, 1, 1)
            )
          )

          val responseEntity = HttpEntity(
            ContentTypes.`application/json`,
            JsObject(
              "code" -> JsNumber(404),
              "type" -> JsString("Not Found"),
              "message" -> JsString(
                "The requested resource could not be found but may be available again in the future."
              )
            ).prettyPrint
          )

          val response = Await.result(future, 5 seconds)
          assert(response.status == StatusCodes.NotFound)
          assert(response.entity == responseEntity)
        }
      }

      describe("and matching by `schema_name`") {
        describe("but not matching by `schema_major_version`") {
          it("returns not found response") {
            val name = "found-consumer"
            val schemaName = "found-consumer-schema"
            val schemaMajorVersion = 1

            assert(
              consumerRepository.create(
                name,
                schemaName,
                schemaMajorVersion,
                11
              )
            )

            val future = Http().singleRequest(
              HttpRequest(
                HttpMethods.GET,
                uri = apiUrl.format(name, schemaName, schemaMajorVersion + 1, 1)
              )
            )

            val responseEntity = HttpEntity(
              ContentTypes.`application/json`,
              JsObject(
                "code" -> JsNumber(404),
                "type" -> JsString("Not Found"),
                "message" -> JsString(
                  "The requested resource could not be found but may be available again in the future."
                )
              ).prettyPrint
            )

            val response = Await.result(future, 5 seconds)
            assert(response.status == StatusCodes.NotFound)
            assert(response.entity == responseEntity)
          }
        }

        describe("and matching by `schema_major_version") {
          describe("but not matching by `schema_minor_version`") {
            it("returns not found response") {
              val name = "found-consumer"
              val schemaName = "found-consumer-schema"
              val schemaMajorVersion = 1
              val schemaMinorVersion = 1

              assert(
                consumerRepository.create(
                  name,
                  schemaName,
                  schemaMajorVersion,
                  schemaMinorVersion
                )
              )

              val future = Http().singleRequest(
                HttpRequest(
                  HttpMethods.GET,
                  uri = apiUrl.format(name, schemaName, schemaMajorVersion, schemaMinorVersion + 1)
                )
              )

              val responseEntity = HttpEntity(
                ContentTypes.`application/json`,
                JsObject(
                  "code" -> JsNumber(404),
                  "type" -> JsString("Not Found"),
                  "message" -> JsString(
                    "The requested resource could not be found but may be available again in the future."
                  )
                ).prettyPrint
              )

              val response = Await.result(future, 5 seconds)
              assert(response.status == StatusCodes.NotFound)
              assert(response.entity == responseEntity)
            }
          }

          describe("and matching by `schema_minor_version`") {
            it("returns the stored consumer") {
              val name = "found-consumer"
              val schemaName = "found-consumer-schema"
              val schemaMajorVersion = 1
              val schemaMinorVersion = 1

              assert(
                consumerRepository.create(
                  name,
                  schemaName,
                  schemaMajorVersion,
                  schemaMinorVersion
                )
              )

              val dbConsumer = consumerRepository.getByNameAndSchema(
                name,
                schemaName,
                schemaMajorVersion,
                schemaMinorVersion
              )

              val future = Http().singleRequest(
                HttpRequest(
                  HttpMethods.GET,
                  uri = apiUrl.format(name, schemaName, schemaMajorVersion, schemaMinorVersion)
                )
              )

              val responseEntity = HttpEntity(
                ContentTypes.`application/json`,
                JsObject(
                  "_id" -> ShortObjectIdFormat.write(dbConsumer._id.get),
                  "name" -> JsString(dbConsumer.name),
                  "schemaName" -> JsString(dbConsumer.schemaName),
                  "schemaMajorVersion" -> JsNumber(dbConsumer.schemaMajorVersion),
                  "schemaMinorVersion" -> JsNumber(dbConsumer.schemaMinorVersion)
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

  describe("POST /api/consumers") {
    val apiUrl = s"$serverAddress/api/consumer"

    describe("with incomplete `ConsumerRequest` body") {
      it("returns a js error") {
        val entity = HttpEntity(
          ContentTypes.`application/json`,
          JsObject(
            "name" -> JsString("example-name"),
            "schemaName" -> JsString("example-schema-name"),
            "schemaMajorVersion" -> JsNumber(3)
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
            HttpEntity(
              ContentTypes.`text/plain(UTF-8)`,
              "The request content was malformed:\nObject is missing required member 'schemaMinorVersion'"
            )
        )
      }
    }

    describe("with complete `ConsumerRequest` body") {
      describe("and already existing `consumer` matching request body") {
        it("returns `Consumer Already Exists` bad request response") {
          val consumerRequest = ObjectMother.defaultConsumerRequest()

          assert(
            consumerRepository.create(
              consumerRequest.name,
              consumerRequest.schemaName,
              consumerRequest.schemaMajorVersion,
              consumerRequest.schemaMinorVersion
            )
          )

          val entity = HttpEntity(
            ContentTypes.`application/json`,
            serializationService.serialize(consumerRequest)
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
                  "message" -> JsString("Consumer already exists.")
                ).prettyPrint
              )
          )
        }
      }

      describe("and not already existing `consumer` matching request body") {
        describe("and schema not matching `consumer` request body") {
          it("returns a `Schema specified in consumer request body does not exist.` bad request response") {
            val consumerRequest = ObjectMother.defaultConsumerRequest()

            val entity = HttpEntity(
              ContentTypes.`application/json`,
              serializationService.serialize(consumerRequest)
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
                    "message" -> JsString("Schema specified in consumer request body does not exist.")
                  ).prettyPrint
                )
            )
          }
        }

        describe("and schema matching `consumer` request body") {
          it("creates a consumer") {
            val consumerRequest = ObjectMother.defaultConsumerRequest()
            val schema = SchemaBuilder
              .aSchema()
              .withName(consumerRequest.schemaName)
              .withMajorVersion(consumerRequest.schemaMajorVersion)
              .withMinorVersion(consumerRequest.schemaMinorVersion)
              .build()

            assert(
              schemaRepository.create(
                schema.name,
                schema.applicationId,
                schema.majorVersion,
                schema.minorVersion,
                schema.fields
              )
            )

            val entity = HttpEntity(
              ContentTypes.`application/json`,
              serializationService.serialize(consumerRequest)
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
          }
        }
      }
    }
  }
}
