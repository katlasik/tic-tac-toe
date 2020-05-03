package io.tictactoe.utilities.routes

trait RoutingModule[F[_]] {

  def router: Router[F]

}
