package com.sumup

import akka.actor.{ActorRef, ActorSystem, OneForOneStrategy, SupervisorStrategy}
import akka.http.scaladsl.Http
import akka.routing.SmallestMailboxPool
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.sumup.actors.compatibility.CompatibilityActor
import com.sumup.actors.consumer.ConsumerActor
import com.sumup.actors.schema.SchemaActor
import com.sumup.actors.schemas.SchemasActor
import com.sumup.creation.SchemaCreationService
import com.sumup.creation.exceptions.SchemaCreationException
import com.sumup.diff.DiffProcessingService
import com.sumup.http.HttpRouter
import com.sumup.serde.SerializationService
import com.sumup.storage.MongoClientWrapper
import com.sumup.storage.repositories.{ConsumerRepository, SchemaRepository}
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object ApplicationMain extends App with Configuration with HttpRouter {
  val log = Logger(getClass)

  implicit val system: ActorSystem = ActorSystem("schema-registry")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val mongoClientWrapper: MongoClientWrapper = new MongoClientWrapper()
  implicit val utilsService: UtilsService = new UtilsService()
  implicit val serializationService: SerializationService = new SerializationService()
  implicit val diffProcessingService: DiffProcessingService = new DiffProcessingService()
  implicit val consumerRepository: ConsumerRepository = new ConsumerRepository()
  implicit val schemaRepository: SchemaRepository = new SchemaRepository()
  implicit val schemaCreationService: SchemaCreationService = new SchemaCreationService()

  implicit val compatibilityActorPool: ActorRef = system.actorOf(
    CompatibilityActor.props().withRouter(
      SmallestMailboxPool(
        config.getInt("application.actors.compatibility.count")
      ),
    ),
    name = "compatibility-actor"
  )

  implicit val consumerActorPool: ActorRef = system.actorOf(
    ConsumerActor.props().withRouter(
      SmallestMailboxPool(
        config.getInt("application.actors.consumer.count")
      )
    ),
    name = "consumer-actor"
  )

  implicit val schemaActorPool: ActorRef = system.actorOf(
    SchemaActor.props().withRouter(
      SmallestMailboxPool(
        config.getInt("application.actors.schema.count")
      )
    ),
    name = "schema-actor"
  )

  implicit val schemasActorPool: ActorRef = system.actorOf(
    SchemasActor.props().withRouter(
      SmallestMailboxPool(
        config.getInt("application.actors.schemas.count")
      )
    ),
    name = "schemas-actor",
  )

  implicit val timeout = Timeout(5 seconds)

  startHttpServer()

  def startHttpServer() = {
    val address = config.getString("application.http.address")
    val port = config.getInt("application.http.port")
    val bindFuture = Http()
      .bindAndHandle(
        routes,
        address,
        port
      )

    log.info(s"Starting HTTP server at $address:$port...")
    sys.addShutdownHook(
      bindFuture
        .flatMap(_.unbind()) // NOTE: Trigger unbinding from the port
        .onComplete(_ => {
        log.info("Stopping HTTP server.")
        system.terminate()
      }) // NOTE: And shutdown when done
    )
  }
}
