package io.tictactoe.testutils.generators

import io.tictactoe.authentication.events.UserRegisteredEvent
import io.tictactoe.authentication.model.{ConfirmationToken, User}
import io.tictactoe.authentication.services.{ConfirmationTokenGenerator, Hash}
import io.tictactoe.values.{Email, EventId, EventTimestamp, No, UserId, Username, Yes}
import org.scalacheck.Gen
import mouse.all._
import cats.implicits._

object Generators {

  def email(): Gen[Email] =
    for {
      addressSize <- Gen.choose(2, 100)
      domainSize <- Gen.choose(2, 100)
      address <- Gen.listOfN(addressSize, Gen.alphaChar).map(_.mkString)
      domain <- Gen.listOfN(domainSize, Gen.alphaChar).map(_.mkString)
    } yield Email.fromString(address + "@" + domain).toOption.get

  def hash(): Gen[Hash] =
    for {
      addressSize <- Gen.choose(6, 100)
      address <- Gen.listOfN(addressSize, Gen.alphaChar).map(_.mkString)
    } yield Hash(address)

  def userId(): Gen[UserId] =
    for {
      id <- Gen.uuid
    } yield UserId(id)

  def eventId(): Gen[EventId] =
    for {
      id <- Gen.uuid
    } yield EventId(id)

  def username(): Gen[Username] =
    for {
      usernameSize <- Gen.choose(2, 100)
      username <- Gen.listOfN(usernameSize, Gen.alphaChar).map(_.mkString)
    } yield Username.fromString(username).toOption.get

  def confirmationToken(): Gen[ConfirmationToken] =
    for {
      token <- Gen.listOfN(ConfirmationTokenGenerator.TokenSize.toInt, Gen.oneOf(ConfirmationTokenGenerator.AllowedCharacters))
    } yield ConfirmationToken(token.mkString)

  def user(confirmed: Boolean = false): Gen[User] =
    for {
      id <- userId()
      hash <- hash()
      email <- email()
      username <- username()
      confirmationToken <- confirmationToken()
    } yield User(id, username, hash, email, confirmed.fold(Yes, No), confirmed.fold(none, confirmationToken.some))

  def users(from: Int = 200, to: Int = 1000): Gen[List[User]] =
    for {
      numberOfUsers <- Gen.choose(from, to)
      users <- Gen.listOfN(numberOfUsers, Generators.user(true))
    } yield users

  def userRegisteredEvent(): Gen[UserRegisteredEvent] =
    for {
      id <- eventId()
      userId <- userId()
      timestamp <- Gen.calendar.map(c => EventTimestamp(c.toInstant))
      username <- username()
      email <- email()
      token <- confirmationToken()
    } yield UserRegisteredEvent(id, timestamp, userId, username, email, token)

}
