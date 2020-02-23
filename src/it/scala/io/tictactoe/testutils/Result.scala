package io.tictactoe.testutils

import com.softwaremill.sttp._
import io.circe.Decoder
import io.circe.parser.decode

case class Result(private val response: Id[Response[String]]) {

  lazy val status: Int = response.code
  lazy val headers: Map[String, String] = response.headers.toMap
  lazy val success: Body = new Body(response.body)
  lazy val error: Body = new Body(response.body.swap)
}

class Body(private val value: Either[String, String]) {
  def plain: String = value.fold(m => throw new AssertionError(m), identity)
  def json[H: Decoder]: H = value.flatMap(decode[H]).fold(m => throw new AssertionError(m), identity)
}
