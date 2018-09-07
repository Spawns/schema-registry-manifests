package com.sumup.testutils

import com.sumup.storage.MongoClientWrapper
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import org.mongodb.scala.MongoDatabase

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

trait DatabaseSpec {
  implicit var ec: ExecutionContext
  implicit val log = Logger(getClass)

  implicit val config: Config = ConfigFactory.load()
  implicit val mongoClientWrapper: MongoClientWrapper = new MongoClientWrapper()
  val database: MongoDatabase = mongoClientWrapper.getDatabase

  def cleanDatabase(): Unit = {
    log.debug("Starting database cleaning")
    val dropConsumers = Await.result(database.getCollection("consumers").drop().toFuture(), 3 seconds)
    val dropSchemas = Await.result(database.getCollection("schemas").drop().toFuture(), 3 seconds)
    log.debug(s"Drop consumers result: $dropConsumers")
    log.debug(s"Drop schemas result: $dropSchemas")
    log.debug("Finished database cleaning")
  }
}
