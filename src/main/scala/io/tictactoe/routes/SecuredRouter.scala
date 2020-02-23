package io.tictactoe.routes

import cats.effect.{ContextShift, Sync}
import io.tictactoe.authentication.services.{Authentication, Registration}
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._
import io.tictactoe.base.utils.Syntax._
import io.tictactoe.base.errors.ErrorMapper._
import cats.implicits._
import io.tictactoe.authentication.errors.ResourceNotFound
import io.tictactoe.authorization.Authorized._
import io.tictactoe.base.logging.Logging
import io.tictactoe.users.services.UserService

object SecuredRouter {

  def routes[F[_]: Sync: ContextShift: Registration: Authentication: UserService: Logging]: HttpRoutes[F] = {

    val getUsers = Endpoints.getUsers.toRoutes(token => {
      val result = for {
        _ <- Authentication[F].verify(token)
        result <- UserService[F].all()
      } yield result

      result.mapErrors
    })

    val getUser = Endpoints.getUser.toRoutes {
      case (id, token) =>
        val result = for {
          user <- Authentication[F].verify(token)
          resource <- UserService[F].get(id).throwIfEmpty(ResourceNotFound)
          _ <- canRead(user, resource)
        } yield resource

        result.mapErrors
    }

    getUsers <+> getUser
  }

}
