package io.tictactoe.testutils

import java.net.URI
import com.softwaremill.sttp._

trait Http { _: ItTest =>

  protected implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  def get(uri: String, headers: Map[String, String] = Map.empty): Result = {
    Result(
      sttp
        .get(Uri.apply(URI.create(uri)))
        .headers(headers)
        .send()
    )
  }

  def post(uri: String, json: String = "", headers: Map[String, String] = Map.empty): Result = {
    Result(
      sttp
        .post(Uri.apply(URI.create(uri)))
        .headers(headers)
        .body(json)
        .send()
    )
  }

  def put(uri: String, json: String = "", headers: Map[String, String] = Map.empty): Result = {
    Result(
      sttp
        .put(Uri.apply(URI.create(uri)))
        .headers(headers)
        .body(json)
        .send()
    )
  }

}
