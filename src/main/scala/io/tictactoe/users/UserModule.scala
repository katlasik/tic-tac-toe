package io.tictactoe.users

import cats.effect.Sync
import io.tictactoe.users.domain.services.LiveUserService
import io.tictactoe.users.infrastructure.repositories.UserRepository
import io.tictactoe.users.infrastructure.routes.UserRouter
import io.tictactoe.users.infrastructure.services.UserService
import io.tictactoe.utilities.authentication.Authentication
import io.tictactoe.utilities.database.Database
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.routes.{Router, RoutingModule}

trait UserModule[F[_]] extends RoutingModule[F] {
  def userService: UserService[F]

  def router: Router[F]
}

object UserModule {

  def live[F[_]: Sync: Database: Authentication: Logging]: UserModule[F] = new UserModule[F] {

    val userRepository: UserRepository[F] = UserRepository.postgresql

    override def userService: UserService[F] = LiveUserService.live(userRepository)

    override def router: Router[F] = new UserRouter[F](userService)
  }

}




