package io.tictactoe.authentication.model

import io.tictactoe.values.{Email, Password}

final case class Credentials(email: Email, password: Password)
