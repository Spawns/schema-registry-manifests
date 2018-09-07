package com.sumup.unit.actors.schema

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import com.sumup.UtilsService
import com.sumup.actors.schema.SchemaActor
import com.sumup.actors.schema.messages.{CreateSchema, DeleteSchemaVersion, GetSchema, GetSchemaVersion}
import com.sumup.creation.SchemaCreationService
import com.sumup.serde.SerializationService
import com.sumup.storage.repositories.SchemaRepository
import org.mockito.Mockito
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import com.sumup.testutils.ObjectMother
import com.sumup.testutils.builders.{SchemaBuilder, SchemaRequestBuilder}

import scala.util.Try

class SchemaActorSpec extends TestKit(ActorSystem("SchemaActorSpec"))
  with ImplicitSender
  with FunSpecLike
  with BeforeAndAfterAll
  with DefaultTimeout
  with MockitoSugar {
  override def afterAll: Unit = TestKit.shutdownActorSystem(system)
  implicit val serializationService: SerializationService = new SerializationService()(utilsService = new UtilsService)

  describe("#receive") {
    describe("with message `CreateSchema`") {
      it("handles the message") {
        implicit val schemaRepository: SchemaRepository = mock[SchemaRepository]
        implicit val schemaCreationService: SchemaCreationService = new SchemaCreationService()
        val schemaRequest = SchemaRequestBuilder.aSchemaRequest().withMinorVersion(1).build()
        val isCreated = true

        Mockito.when(
          schemaRepository.getByNameAndVersion(
            schemaRequest.name,
            schemaRequest.majorVersion,
            schemaRequest.minorVersion
          )
        ).thenReturn(null)

        Mockito.when(
          schemaCreationService.createSchema(
            schemaRequest.name,
            schemaRequest.applicationId,
            schemaRequest.majorVersion,
            schemaRequest.minorVersion,
            schemaRequest.fields
          )
        ).thenReturn(isCreated)


        val completeFn = (resp: HttpResponse) => {
          assert(resp.status == StatusCodes.Created)
          assert(resp.entity ==
            HttpEntity(ContentTypes.`application/json`, serializationService.serializeIsCreated(isCreated))
          )

          ()
        }

        val actorRef = TestActorRef[SchemaActor](SchemaActor.props())
        actorRef.receive(CreateSchema(completeFn, schemaRequest))

        Mockito.verify(schemaRepository).create(
          schemaRequest.name,
          schemaRequest.applicationId,
          schemaRequest.majorVersion,
          schemaRequest.minorVersion,
          schemaRequest.fields
        )
      }
    }
    describe("with message `GetSchema`") {
      it("handles the message") {
        implicit val schemaRepository: SchemaRepository = mock[SchemaRepository]
        implicit val schemaCreationService: SchemaCreationService = new SchemaCreationService()
        val schemaRequest = ObjectMother.defaultSchemaRequest()
        val schemaDouble = Seq(SchemaBuilder.aSchema().withName(schemaRequest.name).build())

        Mockito.when(
          schemaRepository.getByName(schemaRequest.name)
        ).thenReturn(schemaDouble)

        val completeFn = (resp: HttpResponse) => {
          assert(resp.status == StatusCodes.OK)
          assert(resp.entity ==
            HttpEntity(ContentTypes.`application/json`, serializationService.serialize(schemaDouble))
          )

          ()
        }

        val actorRef = TestActorRef[SchemaActor](SchemaActor.props())
        actorRef.receive(GetSchema(completeFn, schemaRequest.name))

        Mockito.verify(schemaRepository).getByName(schemaRequest.name)
      }
    }

    describe("with message `GetSchemaVersion`") {
      it("handles the message") {
        implicit val schemaRepository: SchemaRepository = mock[SchemaRepository]
        implicit val schemaCreationService: SchemaCreationService = new SchemaCreationService()
        val schemaRequest = ObjectMother.defaultSchemaRequest()
        val schemaDouble = SchemaBuilder
          .aSchema()
          .withName(schemaRequest.name)
          .withMajorVersion(schemaRequest.majorVersion)
          .withMinorVersion(schemaRequest.minorVersion)
          .build()

        Mockito.when(
          schemaRepository.getByNameAndVersion(
            schemaRequest.name,
            schemaRequest.majorVersion,
            schemaRequest.minorVersion
          )
        ).thenReturn(schemaDouble)

        val completeFn = (resp: HttpResponse) => {
          assert(resp.status == StatusCodes.OK)
          assert(resp.entity ==
            HttpEntity(ContentTypes.`application/json`, serializationService.serialize(schemaDouble))
          )

          ()
        }

        val actorRef = TestActorRef[SchemaActor](SchemaActor.props())
        actorRef.receive(
          GetSchemaVersion(completeFn, schemaRequest.name, schemaRequest.majorVersion, schemaRequest.minorVersion)
        )

        Mockito
          .verify(schemaRepository)
          .getByNameAndVersion(schemaRequest.name, schemaRequest.majorVersion, schemaRequest.minorVersion)
      }
    }

    describe("with message `DeleteSchemaVersion`") {
      it("handles the message") {
        implicit val schemaRepository: SchemaRepository = mock[SchemaRepository]
        implicit val schemaCreationService: SchemaCreationService = new SchemaCreationService()
        val schemaRequest = ObjectMother.defaultSchemaRequest()
        val schemaDouble = SchemaBuilder
          .aSchema()
          .withName(schemaRequest.name)
          .withMajorVersion(schemaRequest.majorVersion)
          .withMinorVersion(schemaRequest.minorVersion)
          .build()

        Mockito.when(
          schemaRepository.deleteSchemaByNameAndVersion(
            schemaRequest.name,
            schemaRequest.majorVersion,
            schemaRequest.minorVersion
          )
        ).thenReturn(schemaDouble)

        val completeFn = (resp: HttpResponse) => {
          assert(resp.status == StatusCodes.OK)
          assert(resp.entity ==
            HttpEntity(ContentTypes.`application/json`, serializationService.serialize(schemaDouble))
          )

          ()
        }

        val actorRef = TestActorRef[SchemaActor](SchemaActor.props())
        actorRef.receive(
          DeleteSchemaVersion(completeFn, schemaRequest.name, schemaRequest.majorVersion, schemaRequest.minorVersion)
        )

        Mockito
          .verify(schemaRepository)
          .deleteSchemaByNameAndVersion(schemaRequest.name, schemaRequest.majorVersion, schemaRequest.minorVersion)
      }
    }
  }
}
