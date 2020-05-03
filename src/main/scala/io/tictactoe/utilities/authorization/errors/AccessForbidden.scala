package io.tictactoe.utilities.authorization.errors

import io.tictactoe.errors.BaseError

case object AccessForbidden extends BaseError("Access to resource is forbidden");
