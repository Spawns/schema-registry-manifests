package com.sumup.unit.actors.consumer

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import com.sumup.UtilsService
import com.sumup.actors.consumer.ConsumerActor
import com.sumup.actors.consumer.messages.{GetConsumer, GetConsumerBySchema, RegisterConsumer}
import com.sumup.serde.SerializationService
import com.sumup.storage.repositories.{ConsumerRepository, SchemaRepository}
import com.sumup.testutils.ObjectMother
import org.mockito.Mockito
import org.scalatest._
import org.scalatest.mockito.MockitoSugar

class ConsumerActorSpec extends TestKit(ActorSystem("ConsumerActorSpec"))
  with ImplicitSender
  with FunSpecLike
  with BeforeAndAfterAll
  with DefaultTimeout
  with MockitoSugar {
  override def afterAll: Unit = TestKit.shutdownActorSystem(system)
  implicit val serializationService: SerializationService = new SerializationService()(utilsService = new UtilsService)

  describe("#receive") {
    describe("with message `GetConsumer`") {
      it("handles the message") {
        implicit val schemaRepository: SchemaRepository = mock[SchemaRepository]
        implicit val consumerRepository: ConsumerRepository = mock[ConsumerRepository]

        val consumerDouble = Seq(ObjectMother.defaultConsumer())

        val completeFn = (resp: HttpResponse) => {
          assert(resp.status == StatusCodes.OK)
          assert(resp.entity == HttpEntity(ContentTypes.`application/json`, serializationService.serialize(consumerDouble)))
          ()
        }

        val name = "example-consumer"

        Mockito.when(consumerRepository.getByName(name)).thenReturn(consumerDouble)
        val actorRef = TestActorRef[ConsumerActor](ConsumerActor.props())
        actorRef.receive(GetConsumer(completeFn, name))
        Mockito.verify(consumerRepository).getByName(name)
      }

      describe("with message `GetConsumerBySchema`") {
        it("handles the message") {
          implicit val schemaRepository: SchemaRepository = mock[SchemaRepository]
          implicit val consumerRepository: ConsumerRepository = mock[ConsumerRepository]

          val consumerDouble = ObjectMother.defaultConsumer()
          val completeFn = (resp: HttpResponse) => {
            assert(resp.status == StatusCodes.OK)
            assert(resp.entity == HttpEntity(ContentTypes.`application/json`, serializationService.serialize(consumerDouble)))
            ()
          }

          Mockito.when(
            consumerRepository.getByNameAndSchema(
              consumerDouble.name,
              consumerDouble.schemaName,
              consumerDouble.schemaMajorVersion,
              consumerDouble.schemaMinorVersion
            )
          ).thenReturn(consumerDouble)

          val actorRef = TestActorRef[ConsumerActor](ConsumerActor.props())
          actorRef.receive(
            GetConsumerBySchema(
              completeFn,
              consumerDouble.name,
              consumerDouble.schemaName,
              consumerDouble.schemaMajorVersion,
              consumerDouble.schemaMinorVersion
            )
          )

          Mockito
            .verify(consumerRepository)
            .getByNameAndSchema(
              consumerDouble.name,
              consumerDouble.schemaName,
              consumerDouble.schemaMajorVersion,
              consumerDouble.schemaMinorVersion
            )
        }
      }

      describe("with message `RegisterConsumer`") {
        it("handles the message") {
          implicit val schemaRepository: SchemaRepository = mock[SchemaRepository]
          implicit val consumerRepository: ConsumerRepository = mock[ConsumerRepository]

          val isCreated = true

          val completeFn = (resp: HttpResponse) => {
            assert(resp.status == StatusCodes.Created)
            assert(resp.entity ==
              HttpEntity(ContentTypes.`application/json`, serializationService.serializeIsCreated(isCreated))
            )
            ()
          }

          val consumerRequestDouble = ObjectMother.defaultConsumerRequest()

          Mockito.when(
            schemaRepository.getByNameAndVersion(
              consumerRequestDouble.schemaName,
              consumerRequestDouble.schemaMajorVersion,
              consumerRequestDouble.schemaMinorVersion
            )
          ).thenReturn(ObjectMother.defaultSchema())

          Mockito.when(
            consumerRepository.create(
              consumerRequestDouble.name,
              consumerRequestDouble.schemaName,
              consumerRequestDouble.schemaMajorVersion,
              consumerRequestDouble.schemaMinorVersion
            )
          ).thenReturn(isCreated)

          val actorRef = TestActorRef[ConsumerActor](ConsumerActor.props())

          actorRef.receive(
            RegisterConsumer(completeFn, consumerRequestDouble)
          )
        }
      }
    }
  }
}
