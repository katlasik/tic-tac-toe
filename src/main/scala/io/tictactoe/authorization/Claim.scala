package io.tictactoe.authorization

sealed trait Claim
case object Read extends Claim
case object ReadAndWrite extends Claim
