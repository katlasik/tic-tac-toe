package io.tictactoe.users.infrastructure.routes

import cats.data.NonEmptyList
import cats.effect.Sync
import io.tictactoe.errors.{ErrorView, ResourceNotFound}
import io.tictactoe.users.infrastructure.services.UserService
import io.tictactoe.users.model.{DetailedUser, SimpleUser}
import io.tictactoe.utilities.authentication.Authentication
import io.tictactoe.utilities.routes.Router
import io.tictactoe.values.UserId
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{auth, endpoint, path, statusCode}
import cats.implicits._
import io.tictactoe.utilities.authorization.Authorized.canRead
import io.tictactoe.utilities.errors._
import io.tictactoe.utilities.syntax._
import io.circe.generic.auto._
import sttp.tapir._
import io.tictactoe.utilities.logging.Logging

final class UserRouter[F[_]: Authentication: Sync: Logging](
    userService: UserService[F]
) extends Router[F] {

  override protected def serverEndpoints: NonEmptyList[ServerEndpoint[_, _, _, Nothing, F]] = {

    val getUsers = endpoint
      .description("Endpoint for getting users' details.")
      .get
      .in("users")
      .in(auth.bearer)
      .out(jsonBody[List[SimpleUser]].description("List of users."))
      .errorOut(jsonBody[ErrorView].description("Error message."))
      .errorOut(statusCode)
      .serverLogic { token =>
        val result = for {
          _ <- Authentication[F].verify(token)
          result <- userService.confirmedUsers()
        } yield result

        result.mapErrors
      }

    val getUser = endpoint
      .description("Endpoint for getting user's details.")
      .get
      .in("users" / path[UserId].name("id").description("The id of the user."))
      .in(auth.bearer)
      .out(jsonBody[DetailedUser].description("Details of single user."))
      .errorOut(jsonBody[ErrorView].description("Error message."))
      .errorOut(statusCode)
      .serverLogic {
        case (id, token) =>
          val result = for {
            user <- Authentication[F].verify(token)
            resource <- userService.get(id).throwIfEmpty(ResourceNotFound)
            _ <- canRead(user, resource)
          } yield resource

          result.mapErrors
      }

    NonEmptyList.of(getUsers, getUser)
  }
}
