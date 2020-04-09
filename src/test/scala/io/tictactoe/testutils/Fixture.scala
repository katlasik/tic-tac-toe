package io.tictactoe.testutils

import cats.effect.{ContextShift, IO}
import io.tictactoe.authentication.model.Credentials
import io.tictactoe.authentication.repositories.AuthRepository
import io.tictactoe.authentication.services.{AuthEmail, Authentication, PasswordChanger, PasswordHasher, Registration}
import io.tictactoe.infrastructure.events.EventBus
import io.tictactoe.infrastructure.logging.Logging
import io.tictactoe.infrastructure.templates.TemplateRenderer
import io.tictactoe.infrastructure.tokens.TokenGenerator
import io.tictactoe.infrastructure.uuid.UUIDGenerator
import io.tictactoe.infrastructure.calendar.Calendar
import io.tictactoe.emails.services.{EmailRepository, EmailSender}
import io.tictactoe.game.infrastructure.emails.InvitationEmail
import io.tictactoe.game.infrastructure.repositories.InvitationRepository
import io.tictactoe.game.services.GameInvitationService
import io.tictactoe.infrastructure.configuration.Configuration
import io.tictactoe.infrastructure.emails.EmailTransport
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.testutils.mocks.{BypassingPasswordHasher, FixedCalendar, FixedConfirmationTokenGenerator, FixedUUIDGenerator, InMemoryAuthRepository, InMemoryEmailRepository, InMemoryEventBus, InMemoryInvitationRepository, InMemoryUserRepository, MemoryLogging, MockedEmailTransport}
import io.tictactoe.users.repositories.UserRepository
import io.tictactoe.users.services.UserService
import org.http4s.Uri
import org.http4s.dsl.Http4sDsl

import scala.concurrent.ExecutionContext

trait Fixture {

  val dsl = new Http4sDsl[TestAppState] {}

  implicit val cs: ContextShift[IO]  = IO.contextShift(ExecutionContext.global)

  val emptyData = TestAppData()

  lazy implicit val configuration: Configuration[TestAppState] = Configuration.load[TestAppState].runA(emptyData).unsafeRunSync()

  lazy implicit val logging: Logging[TestAppState] = MemoryLogging.memory
  lazy implicit val uuidGenerator: UUIDGenerator[TestAppState] = FixedUUIDGenerator.fixed
  lazy implicit val passwordHasher: PasswordHasher[TestAppState] = BypassingPasswordHasher.bypassing
  lazy implicit val authRepository: AuthRepository[TestAppState] = InMemoryAuthRepository.inMemory
  lazy implicit val userRepository: UserRepository[TestAppState] = InMemoryUserRepository.inMemory
  lazy implicit val eventBus: EventBus[TestAppState] = InMemoryEventBus.inMemory
  lazy implicit val confirmationTokenGenerator: TokenGenerator[TestAppState] = FixedConfirmationTokenGenerator.fixed
  lazy implicit val mockedEmailTransport: EmailTransport[TestAppState] = MockedEmailTransport.mocked
  lazy implicit val emailRepository: EmailRepository[TestAppState] = InMemoryEmailRepository.inMemory
  lazy implicit val invitationRepository: InvitationRepository[TestAppState] = InMemoryInvitationRepository.inMemory

  lazy implicit val emailSender: EmailSender[TestAppState] = EmailSender.live[TestAppState].runA(emptyData).unsafeRunSync()
  lazy implicit val passwordChanger: PasswordChanger[TestAppState] = PasswordChanger.live[TestAppState].runA(emptyData).unsafeRunSync()
  lazy implicit val templateRenderer: TemplateRenderer[TestAppState] = TemplateRenderer.live[TestAppState].runA(emptyData).unsafeRunSync()
  lazy implicit val calendar: Calendar[TestAppState] = FixedCalendar.fixed
  lazy implicit val registration: Registration[TestAppState] = Registration.live.runA(emptyData).unsafeRunSync()
  lazy implicit val authentication: Authentication[TestAppState] = Authentication.live.runA(emptyData).unsafeRunSync()
  lazy implicit val userService: UserService[TestAppState] = UserService.live
  lazy implicit val registrationEmail: AuthEmail[TestAppState] = AuthEmail.live[TestAppState].runA(emptyData).unsafeRunSync()
  lazy implicit val invitationEmail: InvitationEmail[TestAppState] = InvitationEmail.live[TestAppState].runA(emptyData).unsafeRunSync()
  lazy implicit val gameInvitationService: GameInvitationService[TestAppState] = GameInvitationService.live[TestAppState].runA(emptyData).unsafeRunSync()

  def authenticate(credentials: Credentials): TestAppState[String] = authentication.authenticate(credentials).map(_.jwt.toEncodedString)

  def uri(str: String): Uri = Uri.fromString(str).toOption.get

}
