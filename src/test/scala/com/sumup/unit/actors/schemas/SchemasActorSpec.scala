package com.sumup.unit.actors.schemas

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import com.sumup.UtilsService
import com.sumup.actors.schemas.SchemasActor
import com.sumup.actors.schemas.messages.GetSchemas
import com.sumup.serde.SerializationService
import com.sumup.storage.repositories.SchemaRepository
import com.sumup.testutils.ObjectMother
import org.mockito.Mockito
import org.scalatest._
import org.scalatest.mockito.MockitoSugar

class SchemasActorSpec extends TestKit(ActorSystem("SchemasActorSpec"))
  with ImplicitSender
  with FunSpecLike
  with BeforeAndAfterAll
  with DefaultTimeout
  with MockitoSugar {
  override def afterAll: Unit = TestKit.shutdownActorSystem(system)
  implicit val serializationService: SerializationService = new SerializationService()(utilsService = new UtilsService)

  describe("#receive") {
    describe("with message `GetSchemas`") {
      it("handles the message") {
        implicit val schemaRepository: SchemaRepository = mock[SchemaRepository]
        val schemasDouble = Seq(ObjectMother.defaultSchema())
        Mockito.when(schemaRepository.getAll).thenReturn(schemasDouble)

        val completeFn = (resp: HttpResponse) => {
          assert(resp.status == StatusCodes.OK)
          assert(
            resp.entity == HttpEntity(ContentTypes.`application/json`, serializationService.serialize(schemasDouble))
          )
          ()
        }

        val actorRef = TestActorRef[SchemasActor](SchemasActor.props())
        actorRef.receive(GetSchemas(completeFn))

        Mockito.verify(schemaRepository).getAll
      }
    }
  }
}
