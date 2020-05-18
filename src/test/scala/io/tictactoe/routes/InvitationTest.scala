package io.tictactoe.routes

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import cats.data.NonEmptyList
import io.circe.generic.auto._
import io.tictactoe.modules.authentication.model.{Credentials, User}
import cats.implicits._
import io.tictactoe.implicits._
import io.tictactoe.errors.ErrorView
import io.tictactoe.events.model.game.GameInvitationAccepted
import io.tictactoe.modules.game.model.GameInvitationStatus.Pending
import io.tictactoe.modules.game.model.{AcceptedGameInvitation, CancelledGameInvitation, EmailInvitationRequest, GameInvitation, GameInvitationStatus, InvitationResult, RejectedGameInvitation, UserInvitationRequest}
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.testutils.{EqMatcher, Fixture, TestAppData}
import io.tictactoe.values.{GameId, _}
import org.http4s.{Header, Headers, Request}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.implicits._
import org.scalatest.{FlatSpec, Inspectors, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import io.tictactoe.utilities.emails.model.EmailMessage
import io.tictactoe.utilities.emails.values.{EmailMessageText, EmailMessageTitle}
import io.tictactoe.utilities.tokens.values.ConfirmationToken
import org.scalatest.prop.TableDrivenPropertyChecks

class InvitationTest extends FlatSpec with ScalaCheckDrivenPropertyChecks with Matchers with EqMatcher with TableDrivenPropertyChecks with Inspectors {

  it should "allow inviting users by email" in new Fixture {

    val inputData = TestAppData(
      uuids = List(UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000003")),
      tokens = List(ConfirmationToken("token")),
      users = List(
        User(
          UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          Confirmed,
          None,
          None
        )
      )
    )

    val token = authenticate(Credentials(Email("email@user.pl"), Password("userpass"))).runA(inputData).unsafeRunSync()

    val request = Request[TestAppState](
      method = POST,
      uri = uri"games/invitation",
      headers = Headers(List(Header("Authorization", s"Bearer $token")))
    ).withEntity(EmailInvitationRequest(Email("email@email.com")))

    val (outputData, Some(response)) = gameModule.router.routes
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    val email = EmailMessage(
      NonEmptyList.one(Email("email@email.com")),
      Email("no-reply@tictactoe.io"),
      EmailMessageText(
        """You have been invited to play game of tic tac toe by user.
          |
          |To start playing game click on link:
          |http://localhost:8082/games/invitation?token=token&id=00000000-0000-0000-0000-000000000002""".stripMargin
      ),
      EmailMessageTitle("Hello!")
    )

    outputData.infoMessages should contain(
      "Sending invitation mail to email@email.com from user with id 00000000-0000-0000-0000-000000000001."
    )

    outputData.sentEmails should contain(email)
    outputData.savedEmails should contain(email)

    response.status.code shouldEq 200

    response.as[InvitationResult].runA(inputData).unsafeRunSync() shouldEq InvitationResult(
      GameId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
      None,
      UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
      Pending
    )
  }

  it should "allow inviting users directly" in new Fixture {

    val inputData = TestAppData(
      uuids = List(UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000003")),
      tokens = List(ConfirmationToken("token")),
      users = List(
        User(
          UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          Confirmed,
          None,
          None
        ),
        User(
          UserId.unsafeFromString("00000000-0000-0000-0000-000000000004"),
          Username("guest"),
          Hash("guestpass"),
          Email("email@guest.pl"),
          Confirmed,
          None,
          None
        )
      )
    )

    val token = authenticate(Credentials(Email("email@user.pl"), Password("userpass"))).runA(inputData).unsafeRunSync()

    val request = Request[TestAppState](
      method = POST,
      uri = uri"games",
      headers = Headers(List(Header("Authorization", s"Bearer $token")))
    ).withEntity(UserInvitationRequest(UserId.unsafeFromString("00000000-0000-0000-0000-000000000004")))

    val (outputData, Some(response)) = gameModule.router.routes
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    val email = EmailMessage(
      NonEmptyList.one(Email("email@guest.pl")),
      Email("no-reply@tictactoe.io"),
      EmailMessageText(
        """You have been invited to play game of tic tac toe by user.
          |
          |To start playing game click on link:
          |http://localhost:8082/games/invitation?&id=00000000-0000-0000-0000-000000000002""".stripMargin
      ),
      EmailMessageTitle("Hello, guest!")
    )

    outputData.infoMessages should contain(
      "Sending invitation notification mail to user with id 00000000-0000-0000-0000-000000000004 from user with id 00000000-0000-0000-0000-000000000001."
    )

    outputData.sentEmails should contain(email)
    outputData.savedEmails should contain(email)

    response.status.code shouldEq 200

    response.as[InvitationResult].runA(inputData).unsafeRunSync() shouldEq InvitationResult(
      GameId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
      UserId.unsafeFromString("00000000-0000-0000-0000-000000000004").some,
      UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
      Pending
    )
  }

  it should "send notification email if guest mail is already registered" in new Fixture {

    val inputData = TestAppData(
      uuids = List(UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000003")),
      tokens = List(ConfirmationToken("token")),
      users = List(
        User(
          UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          Confirmed,
          None,
          None
        ),
        User(
          UserId.unsafeFromString("00000000-0000-0000-0000-000000000004"),
          Username("guest"),
          Hash("guestpass"),
          Email("guest@email.pl"),
          Confirmed,
          None,
          None
        )
      )
    )

    val token = authenticate(Credentials(Email("email@user.pl"), Password("userpass"))).runA(inputData).unsafeRunSync()

    val request = Request[TestAppState](
      method = POST,
      uri = uri"games/invitation",
      headers = Headers(List(Header("Authorization", s"Bearer $token")))
    ).withEntity(EmailInvitationRequest(Email("guest@email.pl")))

    val (outputData, Some(response)) = gameModule.router.routes
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    outputData.infoMessages should contain allOf (
      "Sending invitation notification mail to user with id 00000000-0000-0000-0000-000000000004 from user with id 00000000-0000-0000-0000-000000000001.",
      "User with email guest@email.pl was already registered, sending notification email."
    )

    response.status.code shouldEq 200

    response.as[InvitationResult].runA(inputData).unsafeRunSync() shouldEq InvitationResult(
      GameId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
      UserId.unsafeFromString("00000000-0000-0000-0000-000000000004").some,
      UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
      Pending
    )
  }

  it should "reject invitation if user wants to invite itself by email" in new Fixture {

    val inputData = TestAppData(
      uuids = List(UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000003")),
      tokens = List(ConfirmationToken("token")),
      users = List(
        User(
          UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          Confirmed,
          None,
          None
        )
      )
    )

    val token = authenticate(Credentials(Email("email@user.pl"), Password("userpass"))).runA(inputData).unsafeRunSync()

    val request = Request[TestAppState](
      method = POST,
      uri = uri"games/invitation",
      headers = Headers(List(Header("Authorization", s"Bearer $token")))
    ).withEntity(EmailInvitationRequest(Email("email@user.pl")))

    val Some(response) = gameModule.router.routes
      .run(request)
      .value
      .runA(inputData)
      .unsafeRunSync()

    response.status.code shouldEq 400

    response.as[ErrorView].runA(inputData).unsafeRunSync() shouldEq ErrorView("Can't invite self.")
  }

  it should "reject invitation if user wants to invite itself by id" in new Fixture {

    val inputData = TestAppData(
      uuids = List(UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000003")),
      tokens = List(ConfirmationToken("token")),
      users = List(
        User(
          UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          Confirmed,
          None,
          None
        )
      )
    )

    val token = authenticate(Credentials(Email("email@user.pl"), Password("userpass"))).runA(inputData).unsafeRunSync()

    val request = Request[TestAppState](
      method = POST,
      uri = uri"games",
      headers = Headers(List(Header("Authorization", s"Bearer $token")))
    ).withEntity(UserInvitationRequest(UserId.unsafeFromString("00000000-0000-0000-0000-000000000001")))

    val Some(response) = gameModule.router.routes
      .run(request)
      .value
      .runA(inputData)
      .unsafeRunSync()

    response.status.code shouldEq 400

    response.as[ErrorView].runA(inputData).unsafeRunSync() shouldEq ErrorView("Can't invite self.")
  }

  it should "reject invitation if invitee doesn't exist" in new Fixture {

    val inputData = TestAppData(
      uuids = List(UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000003")),
      tokens = List(ConfirmationToken("token")),
      users = List(
        User(
          UserId.unsafeFromString("00000000-0000-0000-0000-000000000001"),
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          Confirmed,
          None,
          None
        )
      )
    )

    val token = authenticate(Credentials(Email("email@user.pl"), Password("userpass"))).runA(inputData).unsafeRunSync()

    val request = Request[TestAppState](
      method = POST,
      uri = uri"games",
      headers = Headers(List(Header("Authorization", s"Bearer $token")))
    ).withEntity(UserInvitationRequest(UserId.unsafeFromString("00000000-0000-0000-0000-000000000009")))

    val Some(response) = gameModule.router.routes
      .run(request)
      .value
      .runA(inputData)
      .unsafeRunSync()

    response.status.code shouldEq 404

    response.as[ErrorView].runA(inputData).unsafeRunSync() shouldEq ErrorView("Can't find resource.")
  }

  it should "allow accepting invitations" in new Fixture {

    val ownerId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000001")
    val guestId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000002")
    val gameId = GameId.unsafeFromString("00000000-0000-0000-0000-000000000004")
    val now = LocalDateTime.parse("2019-02-02T11:12:10")
    val eventTimestamp = Instant.parse("2019-02-03T12:30:30.775935Z")

    val inputData = TestAppData(
      uuids = List(UUID.fromString("00000000-0000-0000-0000-000000000010")),
      invitations = List(
        GameInvitation.withGuestId(
          id = gameId,
          ownerId = ownerId,
          guestId = guestId
        )
      ),
      instants = List(eventTimestamp),
      dates = List(now),
      tokens = List(ConfirmationToken("token")),
      users = List(
        User(
          ownerId,
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          Confirmed,
          None,
          None
        ),
        User(
          guestId,
          Username("user2"),
          Hash("userpass"),
          Email("email2@user.pl"),
          Confirmed,
          None,
          None
        )
      )
    )

    val token = authenticate(Credentials(Email("email2@user.pl"), Password("userpass"))).runA(inputData).unsafeRunSync()

    val request = Request[TestAppState](
      method = POST,
      uri = uri(show"games/$gameId"),
      headers = Headers(List(Header("Authorization", s"Bearer $token")))
    )

    val (outputData, Some(response)) = gameModule.router.routes
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldEq 200

    response.as[InvitationResult].runA(inputData).unsafeRunSync() shouldEq InvitationResult(
      gameId,
      guestId.some,
      ownerId,
      GameInvitationStatus.Accepted
    )

    outputData.invitations should contain(
      AcceptedGameInvitation(
        gameId,
        ownerId,
        guestId,
        None,
        None,
        now
      )
    )

    outputData.events should contain(
      GameInvitationAccepted(
        EventId.unsafeFromString("00000000-0000-0000-0000-000000000010"),
        EventTimestamp(eventTimestamp),
        gameId,
        ownerId,
        guestId
      )
    )

  }

  it should "reject accepting invitations by wrong user" in new Fixture {

    val ownerId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000001")
    val guestId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000002")
    val gameId = GameId.unsafeFromString("00000000-0000-0000-0000-000000000004")
    val now = LocalDateTime.parse("2019-02-02T11:12:10")

    val cases = Table(
      "email",
      Email("email2@user.pl"),
      Email("email3@user.pl")
    )

    forAll(cases) { email =>
      val inputData = TestAppData(
        invitations = List(
          GameInvitation.withGuestId(
            id = gameId,
            ownerId = ownerId,
            guestId = guestId
          )
        ),
        dates = List(now),
        tokens = List(ConfirmationToken("token")),
        users = List(
          User(
            guestId,
            Username("user"),
            Hash("userpass"),
            Email("email@user.pl"),
            Confirmed,
            None,
            None
          ),
          User(
            ownerId,
            Username("user2"),
            Hash("userpass"),
            Email("email2@user.pl"),
            Confirmed,
            None,
            None
          ),
          User(
            UserId.unsafeFromString("00000000-0000-0000-0000-000000000005"),
            Username("user3"),
            Hash("userpass"),
            Email("email3@user.pl"),
            Confirmed,
            None,
            None
          )
        )
      )

      val token = authenticate(Credentials(email, Password("userpass"))).runA(inputData).unsafeRunSync()

      val request = Request[TestAppState](
        method = POST,
        uri = uri(show"games/$gameId"),
        headers = Headers(List(Header("Authorization", s"Bearer $token")))
      )

      val Some(response) = gameModule.router.routes
        .run(request)
        .value
        .runA(inputData)
        .unsafeRunSync()

      response.status.code shouldEq 403

      response.as[ErrorView].runA(inputData).unsafeRunSync() shouldEq ErrorView("Access to resource is forbidden!")
    }

  }

  it should "allow cancelling invitations" in new Fixture {

    val ownerId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000001")
    val guestId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000002")
    val gameId = GameId.unsafeFromString("00000000-0000-0000-0000-000000000004")
    val now = LocalDateTime.parse("2019-02-02T11:12:10")

    val inputData = TestAppData(
      invitations = List(
        GameInvitation.withGuestId(
          id = gameId,
          ownerId = ownerId,
          guestId = guestId
        )
      ),
      dates = List(now),
      tokens = List(ConfirmationToken("token")),
      users = List(
        User(
          ownerId,
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          Confirmed,
          None,
          None
        ),
        User(
          guestId,
          Username("user2"),
          Hash("userpass"),
          Email("email2@user.pl"),
          Confirmed,
          None,
          None
        )
      )
    )

    val token = authenticate(Credentials(Email("email@user.pl"), Password("userpass"))).runA(inputData).unsafeRunSync()

    val request = Request[TestAppState](
      method = DELETE,
      uri = uri(show"games/$gameId"),
      headers = Headers(List(Header("Authorization", s"Bearer $token")))
    )

    val (outputData, Some(response)) = gameModule.router.routes
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldEq 200

    response.as[InvitationResult].runA(inputData).unsafeRunSync() shouldEq InvitationResult(
      gameId,
      guestId.some,
      ownerId,
      GameInvitationStatus.Cancelled
    )

    outputData.invitations should contain(
      CancelledGameInvitation(
        gameId,
        ownerId,
        guestId.some,
        None,
        None,
        now
      )
    )
  }

  it should "allow rejecting invitations" in new Fixture {

    val ownerId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000001")
    val guestId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000002")
    val gameId = GameId.unsafeFromString("00000000-0000-0000-0000-000000000004")
    val now = LocalDateTime.parse("2019-02-02T11:12:10")

    val inputData = TestAppData(
      invitations = List(
        GameInvitation.withGuestId(
          id = gameId,
          ownerId = ownerId,
          guestId = guestId
        )
      ),
      dates = List(now),
      tokens = List(ConfirmationToken("token")),
      users = List(
        User(
          ownerId,
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          Confirmed,
          None,
          None
        ),
        User(
          guestId,
          Username("user2"),
          Hash("userpass"),
          Email("email2@user.pl"),
          Confirmed,
          None,
          None
        )
      )
    )

    val token = authenticate(Credentials(Email("email2@user.pl"), Password("userpass"))).runA(inputData).unsafeRunSync()

    val request = Request[TestAppState](
      method = DELETE,
      uri = uri(show"games/$gameId"),
      headers = Headers(List(Header("Authorization", s"Bearer $token")))
    )

    val (outputData, Some(response)) =gameModule.router.routes
      .run(request)
      .value
      .run(inputData)
      .unsafeRunSync()

    response.status.code shouldEq 200

    response.as[InvitationResult].runA(inputData).unsafeRunSync() shouldEq InvitationResult(
      gameId,
      guestId.some,
      ownerId,
      GameInvitationStatus.Rejected
    )

    outputData.invitations should contain(
      RejectedGameInvitation(
        gameId,
        ownerId,
        guestId.some,
        None,
        None,
        now
      )
    )
  }

  it should "reject cancelling or rejecting invitations by wrong user" in new Fixture {

    val gameId = GameId.unsafeFromString("00000000-0000-0000-0000-000000000004")
    val now = LocalDateTime.parse("2019-02-02T11:12:10")

    val inputData = TestAppData(
      invitations = List(
        GameInvitation.withGuestId(
          id = gameId,
          ownerId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000005"),
          guestId = UserId.unsafeFromString("00000000-0000-0000-0000-000000000006")
        )
      ),
      dates = List(now),
      tokens = List(ConfirmationToken("token")),
      users = List(
        User(
          UserId.unsafeFromString("00000000-0000-0000-0000-000000000005"),
          Username("user"),
          Hash("userpass"),
          Email("email@user.pl"),
          Confirmed,
          None,
          None
        ),
        User(
          UserId.unsafeFromString("00000000-0000-0000-0000-000000000006"),
          Username("user2"),
          Hash("userpass"),
          Email("email2@user.pl"),
          Confirmed,
          None,
          None
        ),
        User(
          UserId.unsafeFromString("00000000-0000-0000-0000-000000000007"),
          Username("user3"),
          Hash("userpass"),
          Email("email3@user.pl"),
          Confirmed,
          None,
          None
        )
      )
    )

    val token = authenticate(Credentials(Email("email3@user.pl"), Password("userpass"))).runA(inputData).unsafeRunSync()

    val request = Request[TestAppState](
      method = DELETE,
      uri = uri(show"games/$gameId"),
      headers = Headers(List(Header("Authorization", s"Bearer $token")))
    )

    val Some(response) = gameModule.router.routes
      .run(request)
      .value
      .runA(inputData)
      .unsafeRunSync()

    response.status.code shouldEq 403

    response.as[ErrorView].runA(inputData).unsafeRunSync() shouldEq ErrorView("Access to resource is forbidden!")
  }

}
