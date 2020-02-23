package io.tictactoe.authorization.errors

import io.tictactoe.error.BaseError

case object AccessForbidden extends BaseError("Access to resource is forbidden");
