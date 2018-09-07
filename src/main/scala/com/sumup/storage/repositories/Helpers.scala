package com.sumup.storage.repositories

import java.util.concurrent.TimeUnit
import org.bson.Document
import org.mongodb.scala.{Completed, Observable}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try

abstract class Repository {
  implicit class DocumentObservable[C](val observable: Observable[Document]) extends ImplicitObservable[Document] {
    override val converter: (Document) => String = (doc) => doc.toJson
  }

  implicit class GenericObservable[C](val observable: Observable[C]) extends ImplicitObservable[C] {
    override val converter: (C) => String = (doc) => doc.toString
  }

  trait ImplicitObservable[C] {
    val observable: Observable[C]
    val converter: (C) => String

    def results(): Seq[C] = {
      Await.result(observable.toFuture(), Duration(10, TimeUnit.SECONDS))
    }

    def wasSuccess(): Boolean = {
      val actualResults = results()

      if (actualResults.isEmpty) {
        false
      } else {
        actualResults.forall(s => s.isInstanceOf[Completed])
      }
    }

    def headResult(): C = Await.result(observable.head(), Duration(10, TimeUnit.SECONDS))
  }
}
