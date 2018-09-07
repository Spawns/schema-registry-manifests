package com.sumup.integration.http.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.sumup.UtilsService
import com.sumup.dto.fields.IntField
import com.sumup.json.SchemaRegistryJsonProtocol.ShortObjectIdFormat
import com.sumup.serde.SerializationService
import com.sumup.storage.repositories.SchemaRepository
import com.sumup.testutils.DatabaseSpec
import com.sumup.testutils.builders.SchemaBuilder
import org.scalatest.{BeforeAndAfterEach, DoNotDiscover, FunSpec}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

@DoNotDiscover
class SchemasHttpRouteSpec extends FunSpec with DatabaseSpec with BeforeAndAfterEach {
  import spray.json._
  implicit val system: ActorSystem = ActorSystem("SchemasHttpRouteSpec")
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

  describe("GET /api/schemas") {
    val apiUrl = s"$serverAddress/api/schemas"

    describe("with no stored schemas") {
      it("returns an empty JSON array") {
        val future = Http().singleRequest(
          HttpRequest(
            HttpMethods.GET,
            uri = apiUrl
          )
        )

        val response = Await.result(future, 5 seconds)
        assert(response.status == StatusCodes.OK)
        assert(
          response.entity == HttpEntity(
            ContentTypes.`application/json`,
            JsArray().prettyPrint
          )
        )
      }
    }

    describe("with stored schemas") {
      it("returns all stored schemas") {
        val field = IntField("example-field")
        val schemaOne = SchemaBuilder.aSchema().withName("schema-one").withFields(List(field)).build()
        val schemaTwo = SchemaBuilder.aSchema().withName("schema-two").withFields(List(field)).build()
        val schemaThree = SchemaBuilder.aSchema().withName("schema-three").withFields(List(field)).build()
        for (schema <- List(schemaOne, schemaTwo, schemaThree)) {
          val isCreated = schemaRepository.create(
            schema.name,
            schema.applicationId,
            schema.majorVersion,
            schema.minorVersion,
            schema.fields
          )

          assert(isCreated)
        }

        val future = Http().singleRequest(
          HttpRequest(
            HttpMethods.GET,
            uri = apiUrl
          )
        )

        val responseEntity = HttpEntity(
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

        val response = Await.result(future, 5 seconds)
        assert(response.status == StatusCodes.OK)
        assert(response.entity == responseEntity)
      }
    }
  }
}
