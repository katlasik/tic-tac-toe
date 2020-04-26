package io.tictactoe.testutils

import mouse.boolean._
import com.softwaremill.sttp._

import com.softwaremill.sttp.Uri

import scala.util.Try

trait Server { _: ItTest with Http =>

  def baseUrl(url: String) = s"http://${config.server.host}:${config.server.port}/$url"

  def waitForServer(): Unit = repeatUntil(5, "Couldn't connect to server.") {
    val response = Try(sttp.get(Uri(host = config.server.host, port = config.server.port)).send())
    response.isSuccess.option(())
  }

}
