package com.sumup.unit.actors.compatibility

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import com.sumup.UtilsService
import com.sumup.actors.compatibility.CompatibilityActor
import com.sumup.actors.compatibility.messages.CompatibilityCheck
import com.sumup.diff.DiffProcessingService
import com.sumup.diff.entities.{Operation, SchemaDiffResult}
import com.sumup.dto.responses.standard.NotFoundResponse
import com.sumup.serde.SerializationService
import com.sumup.storage.repositories.{ConsumerRepository, SchemaRepository}
import com.sumup.testutils.ObjectMother
import com.sumup.testutils.builders.SchemaBuilder
import gnieh.diffson.sprayJson
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest._
import org.scalatest.mockito.MockitoSugar

class CompatibilityActorSpec extends TestKit(ActorSystem("CompatibilityActorSpec"))
  with ImplicitSender
  with FunSpecLike
  with BeforeAndAfterAll
  with DefaultTimeout
  with MockitoSugar {
  override def afterAll: Unit = TestKit.shutdownActorSystem(system)
  implicit val serializationService: SerializationService = new SerializationService()(utilsService = new UtilsService)

  describe("#receive") {
    describe("with message `CompatibilityCheck`") {
      describe("and non-existing schema based on `schemaRequest`") {
        describe("and a schema existing prior 1 minor version") {
          it("handles the message") {
            implicit val schemaRepository: SchemaRepository = mock[SchemaRepository]
            implicit val consumerRepository: ConsumerRepository = mock[ConsumerRepository]
            implicit val diffProcessingService: DiffProcessingService = mock[DiffProcessingService]
            val schemaRequestDouble = ObjectMother.defaultSchemaRequest()

            Mockito
              .when(
                schemaRepository.getByNameAndVersion(
                  schemaRequestDouble.name,
                  schemaRequestDouble.majorVersion,
                  schemaRequestDouble.minorVersion
                )
              )
              .thenReturn(null)

            val schemaDouble = SchemaBuilder
              .aSchema()
              .withName(schemaRequestDouble.name)
              .withMajorVersion(schemaRequestDouble.majorVersion)
              .withMinorVersion(schemaRequestDouble.minorVersion - 1)
              .build()

            Mockito
              .when(
                schemaRepository
                  .getByNameAndVersion(
                    schemaRequestDouble.name,
                    schemaRequestDouble.majorVersion,
                    schemaRequestDouble.minorVersion - 1
                  )
              )
              .thenReturn(schemaDouble)

            val jsonPatchDouble = sprayJson.JsonPatch()
            Mockito
              .when(
                diffProcessingService.diff(schemaRequestDouble, schemaDouble)
              )
              .thenReturn(jsonPatchDouble)

            val isMinorSchemaUpgrade = true
            val isMajorSchemaUpgrade = false
            val schemaDiffResultDouble = SchemaDiffResult(
              isSameSchema = false,
              isMajorUpgradable = true,
              isMinorUpgradable = true,
              isMajorSchemaUpgrade,
              isMinorSchemaUpgrade,
              List[Operation]()
            )

            val completeFn = (resp: HttpResponse) => {
              assert(resp.status == StatusCodes.OK)
              assert(
                resp.entity ==
                  HttpEntity(ContentTypes.`application/json`, serializationService.serialize(schemaDiffResultDouble))
              )
              ()
            }

            Mockito
              .when(
                diffProcessingService.fullProcess(
                  ArgumentMatchers.same(schemaRequestDouble),
                  ArgumentMatchers.same(schemaDouble),
                  ArgumentMatchers.same(isMajorSchemaUpgrade),
                  ArgumentMatchers.same(isMinorSchemaUpgrade)
                )
              )
              .thenReturn(schemaDiffResultDouble)


            val actorRef = TestActorRef[CompatibilityActor](CompatibilityActor.props())
            actorRef.receive(CompatibilityCheck(completeFn, schemaRequestDouble))

            Mockito
              .verify(schemaRepository)
              .getByNameAndVersion(
                schemaRequestDouble.name,
                schemaRequestDouble.majorVersion,
                schemaRequestDouble.minorVersion
              )

            Mockito
              .verify(diffProcessingService)
              .fullProcess(
                ArgumentMatchers.same(schemaRequestDouble),
                ArgumentMatchers.same(schemaDouble),
                ArgumentMatchers.same(isMajorSchemaUpgrade),
                ArgumentMatchers.same(isMinorSchemaUpgrade)
              )
          }
        }

        describe("and no schema existing prior 1 minor version") {
          it("handles the message") {
            implicit val schemaRepository: SchemaRepository = mock[SchemaRepository]
            implicit val consumerRepository: ConsumerRepository = mock[ConsumerRepository]
            implicit val diffProcessingService: DiffProcessingService = mock[DiffProcessingService]

            val completeFn = (resp: HttpResponse) => {
              assert(
                resp.entity ==
                  HttpEntity(
                    ContentTypes.`application/json`, serializationService.serialize(NotFoundResponse())
                  )
              )

              ()
            }

            val schemaRequestDouble = ObjectMother.defaultSchemaRequest()

            Mockito
              .when(
                schemaRepository.getByNameAndVersion(
                  schemaRequestDouble.name,
                  schemaRequestDouble.majorVersion,
                  schemaRequestDouble.minorVersion
                )
              )
              .thenReturn(null)

            Mockito
              .when(
                schemaRepository.getByNameAndVersion(
                  schemaRequestDouble.name,
                  schemaRequestDouble.majorVersion,
                  schemaRequestDouble.minorVersion - 1
                )
              )
              .thenReturn(null)

            val actorRef = TestActorRef[CompatibilityActor](CompatibilityActor.props())
            actorRef.receive(CompatibilityCheck(completeFn, schemaRequestDouble))
          }
        }
      }

      describe("and existing schema based on `schemaRequest`") {
        it("handles the message") {
          implicit val schemaRepository: SchemaRepository = mock[SchemaRepository]
          implicit val consumerRepository: ConsumerRepository = mock[ConsumerRepository]
          implicit val diffProcessingService: DiffProcessingService = mock[DiffProcessingService]

          val schemaRequestDouble = ObjectMother.defaultSchemaRequest()
          val schemaDouble = SchemaBuilder
            .aSchema()
            .withName(schemaRequestDouble.name)
            .withMajorVersion(schemaRequestDouble.majorVersion)
            .withMinorVersion(schemaRequestDouble.minorVersion)
            .build()

          Mockito
            .when(
              schemaRepository.getByNameAndVersion(
                schemaRequestDouble.name,
                schemaRequestDouble.majorVersion,
                schemaRequestDouble.minorVersion
              )
            )
            .thenReturn(schemaDouble)

          val schemaDiffResultDouble = ObjectMother.defaultSchemaDiffResultForUpgradableSchema()

          val completeFn = (resp: HttpResponse) => {
            assert(resp.status == StatusCodes.OK)
            assert(
              resp.entity ==
                HttpEntity(ContentTypes.`application/json`, serializationService.serialize(schemaDiffResultDouble))
            )
            ()
          }

          Mockito.when(
            diffProcessingService.fullProcess(
              ArgumentMatchers.same(schemaRequestDouble),
              ArgumentMatchers.same(schemaDouble),
              ArgumentMatchers.same(false),
              ArgumentMatchers.same(false)
            )
          ).thenReturn(schemaDiffResultDouble)


          val actorRef = TestActorRef[CompatibilityActor](CompatibilityActor.props())
          actorRef.receive(CompatibilityCheck(completeFn, schemaRequestDouble))

          Mockito
            .verify(schemaRepository)
            .getByNameAndVersion(
              schemaRequestDouble.name,
              schemaRequestDouble.majorVersion,
              schemaRequestDouble.minorVersion
            )

          Mockito.verify(diffProcessingService).fullProcess(
            ArgumentMatchers.same(schemaRequestDouble),
            ArgumentMatchers.same(schemaDouble),
            ArgumentMatchers.same(false),
            ArgumentMatchers.same(false)
          )
        }
      }
    }
  }
}
