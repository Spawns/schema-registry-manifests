package com.sumup.testutils

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import com.sumup.ApplicationMain
import org.scalatest.Suites

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, TimeoutException}

trait ServerSpec extends DatabaseSpec {
  this: Suites =>

  implicit val system: ActorSystem = ActorSystem("ServerSpec")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  override implicit var ec: ExecutionContext = system.dispatcher

  val serverAddress = s"http://${config.getString("application.http.address")}:${config.getInt("application.http.port")}"

  val serverThread = new Thread(() => {
    val args = new Array[String](1)
    ApplicationMain.main(args)
  })

  def startServer(): Unit = {
    serverThread.start()
  }

  def stopServer(): Unit = {
    serverThread.interrupt()
  }

  def waitServerBind(currentRetry: Int = 0, maxRetries: Int = 5): Unit = {
    val future = Http().singleRequest(HttpRequest(uri = serverAddress))
    if (currentRetry >= maxRetries) {
      fail(s"HTTP server can't be started within $maxRetries retries.")
    }

    try {
      Await.result(future, 5 seconds)
    } catch {
      case _: TimeoutException =>
        waitServerBind(currentRetry + 1)
      // NOTE: Since akka-http's client API is built by people who probably don't use it,
      // there's no way to differentiate a "Connection refused" from e.g a "Connection timed out" programmatically.
      // Foolishly retry until the retry limit is reached.
      case _: akka.stream.StreamTcpException =>
        Thread.sleep(2000)
        waitServerBind(currentRetry + 1)
    }
  }
}
