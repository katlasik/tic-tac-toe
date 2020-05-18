package io.tictactoe.testutils.generators

import java.util.UUID

import cats.data.NonEmptyList
import io.tictactoe.modules.authentication.model.User
import io.tictactoe.values.{Confirmed, Email, EventId, EventTimestamp, GameId, Hash, Unconfirmed, UserId, Username}
import org.scalacheck.Gen
import mouse.all._
import cats.implicits._
import io.tictactoe.events.model.authentication.UserRegisteredEvent
import io.tictactoe.events.model.game.GameInvitationAccepted
import io.tictactoe.utilities.emails.model
import io.tictactoe.utilities.emails.model.MissingEmail
import io.tictactoe.utilities.tokens.TokenGenerator
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import io.tictactoe.utilities.emails.values.{EmailMessageText, EmailMessageTitle, MailId}
import io.tictactoe.utilities.uuid.Id
import shapeless.{::, Generic, HNil}

object Generators {

  def missingEmail(): Gen[MissingEmail] =
    for {
      sender <- email()
      recipients <- emails(1, 10)
      id <- id[MailId]
      title <- Gen.alphaNumStr
      text <- Gen.alphaNumStr
    } yield model.MissingEmail(id, NonEmptyList.fromListUnsafe(recipients), sender, EmailMessageText(text), EmailMessageTitle(title))

  def missingEmails(from: Int = 10, to: Int = 20): Gen[List[MissingEmail]] =
    for {
      numberOfEmails <- Gen.choose(from, to)
      emails <- Gen.listOfN(numberOfEmails, Generators.missingEmail())
    } yield emails

  def email(): Gen[Email] =
    for {
      addressSize <- Gen.choose(2, 100)
      domainSize <- Gen.choose(2, 100)
      address <- Gen.listOfN(addressSize, Gen.alphaChar).map(_.mkString)
      domain <- Gen.listOfN(domainSize, Gen.alphaChar).map(_.mkString)
    } yield Email.fromString(address + "@" + domain).toOption.get

  def emails(from: Int = 200, to: Int = 1000): Gen[List[Email]] =
    for {
      numberOfEmails <- Gen.choose(from, to)
      emails <- Gen.listOfN(numberOfEmails, Generators.email())
    } yield emails

  def hash(): Gen[Hash] =
    for {
      addressSize <- Gen.choose(6, 100)
      address <- Gen.listOfN(addressSize, Gen.alphaChar).map(_.mkString)
    } yield Hash(address)

  def id[I <: AnyVal](implicit repr: Generic.Aux[I, UUID :: HNil]): Gen[I] =
    for {
      id <- Gen.uuid
    } yield Id.create[I](id)

  def username(): Gen[Username] =
    for {
      usernameSize <- Gen.choose(2, 100)
      username <- Gen.listOfN(usernameSize, Gen.alphaChar).map(_.mkString)
    } yield Username.fromString(username).toOption.get

  def confirmationToken(): Gen[ConfirmationToken] =
    for {
      token <- Gen.listOfN(TokenGenerator.TokenSize.toInt, Gen.oneOf(TokenGenerator.AllowedCharacters))
    } yield ConfirmationToken(token.mkString)

  def user(confirmed: Boolean = false): Gen[User] =
    for {
      id <- id[UserId]
      hash <- hash()
      email <- email()
      username <- username()
      confirmationToken <- confirmationToken()
    } yield User(id, username, hash, email, confirmed.fold(Confirmed, Unconfirmed), confirmed.fold(none, confirmationToken.some), None)

  def users(from: Int = 200, to: Int = 1000): Gen[List[User]] =
    for {
      numberOfUsers <- Gen.choose(from, to)
      users <- Gen.listOfN(numberOfUsers, Generators.user(true))
    } yield users

  def userRegisteredEvent(confirmed: Boolean = false): Gen[UserRegisteredEvent] =
    for {
      eventId <- id[EventId]
      timestamp <- Gen.calendar.map(c => EventTimestamp(c.toInstant))
      user <- user(confirmed)
    } yield UserRegisteredEvent(eventId, timestamp, user.id, user.username, user.email, user.registrationConfirmationToken, user.isConfirmed)

  def gameInvitationAccepted(): Gen[GameInvitationAccepted] =
    for {
      eventId <- id[EventId]
      timestamp <- Gen.calendar.map(c => EventTimestamp(c.toInstant))
      gameId <- id[GameId]
      hostId <- id[UserId]
      guestId <- id[UserId]
    } yield GameInvitationAccepted(eventId, timestamp, gameId, hostId, guestId)

}
