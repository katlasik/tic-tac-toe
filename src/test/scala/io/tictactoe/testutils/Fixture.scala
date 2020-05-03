package io.tictactoe.testutils

import cats.effect.{ContextShift, IO}
import io.tictactoe.authentication.AuthenticationModule
import io.tictactoe.authentication.infrastructure.effects.PasswordHasher
import io.tictactoe.authentication.model.Credentials
import io.tictactoe.authentication.infrastructure.repositories.AuthRepository
import io.tictactoe.authentication.domain.services.{LiveAuthEmail, LiveAuthenticator, LivePasswordChanger, LiveRegistration}
import io.tictactoe.authentication.infrastructure.routes.AuthenticationRouter
import io.tictactoe.authentication.infrastructure.services.{AuthEmail, Authenticator, PasswordChanger, Registration}
import io.tictactoe.game.GameModule
import io.tictactoe.utilities.events.EventBus
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.templates.TemplateRenderer
import io.tictactoe.utilities.tokens.TokenGenerator
import io.tictactoe.utilities.uuid.UUIDGenerator
import io.tictactoe.utilities.calendar.Calendar
import io.tictactoe.utilities.emails.services.LiveEmailSender
import io.tictactoe.game.infrastructure.emails.InvitationEmail
import io.tictactoe.game.domain.services.LiveGameInvitationService
import io.tictactoe.game.infrastructure.routes.GameRouter
import io.tictactoe.game.infrastructure.services.GameInvitationService
import io.tictactoe.utilities.configuration.Configuration
import io.tictactoe.utilities.emails.{EmailRepository, EmailSender, EmailTransport}
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.testutils.mocks.{BypassingPasswordHasher, FixedCalendar, FixedConfirmationTokenGenerator, FixedUUIDGenerator, InMemoryAuthRepository, InMemoryEmailRepository, InMemoryEventBus, InMemoryInvitationRepository, InMemoryUserRepository, MemoryLogging, MockedEmailTransport}
import io.tictactoe.users.UserModule
import io.tictactoe.users.domain.services.LiveUserService
import io.tictactoe.users.infrastructure.routes.UserRouter
import io.tictactoe.users.infrastructure.services.UserService
import io.tictactoe.utilities.authentication.Authentication
import io.tictactoe.utilities.routes.Router
import org.http4s.Uri
import org.http4s.dsl.Http4sDsl

import scala.concurrent.ExecutionContext

trait Fixture extends Http4sDsl[TestAppState]{

  implicit val cs: ContextShift[IO]  = IO.contextShift(ExecutionContext.global)

  lazy implicit val uuidGenerator: UUIDGenerator[TestAppState] = FixedUUIDGenerator.fixed
  lazy implicit val eventBus: EventBus[TestAppState] = InMemoryEventBus.inMemory
  lazy implicit val logging: Logging[TestAppState] = MemoryLogging.memory

  lazy implicit val calendar: Calendar[TestAppState] = FixedCalendar.fixed
  lazy implicit val passwordHasher: PasswordHasher[TestAppState] = BypassingPasswordHasher.bypassing
  lazy implicit val confirmationTokenGenerator: TokenGenerator[TestAppState] = FixedConfirmationTokenGenerator.fixed

  lazy val authRepository: AuthRepository[TestAppState] = InMemoryAuthRepository.inMemory
  lazy val mockedEmailTransport: EmailTransport[TestAppState] = MockedEmailTransport.mocked
  lazy val inMemoryEmailRepository: EmailRepository[TestAppState] = InMemoryEmailRepository.inMemory

  implicit val fixtures = for {
    implicit0(configuration: Configuration[TestAppState]) <- Configuration.load[TestAppState]
    implicit0(authentication: Authentication[TestAppState]) <- Authentication.live[TestAppState]
    templateRenderer: TemplateRenderer[TestAppState] <- TemplateRenderer.live[TestAppState]
  } yield (configuration, authentication, templateRenderer)

  implicit val (configuration, authentication, templateRenderer) = fixtures.runEmptyA.unsafeRunSync()

  implicit val emailSender: EmailSender[TestAppState] = LiveEmailSender.create(inMemoryEmailRepository, mockedEmailTransport).runEmptyA.unsafeRunSync()

  val userModule = new UserModule[TestAppState] {
    override def userService: UserService[TestAppState] = LiveUserService.live(InMemoryUserRepository.inMemory)
    override def router: Router[TestAppState] = new UserRouter[TestAppState](userService)
  }

  val gameModule = (for {
    ie <- InvitationEmail.live[TestAppState]
    is <- LiveGameInvitationService.live(InMemoryInvitationRepository.inMemory, ie, userModule.userService)
  } yield new GameModule[TestAppState] {
    override def invitationService: GameInvitationService[TestAppState] = is
    override def router: GameRouter[TestAppState] = new GameRouter[TestAppState](is)
  }).runEmptyA.unsafeRunSync()

  val authModule = (for {
    ae: AuthEmail[TestAppState] <- LiveAuthEmail.live[TestAppState]
    pc: PasswordChanger[TestAppState] <- LivePasswordChanger.live[TestAppState](ae, authRepository)
    r: Registration[TestAppState] <- LiveRegistration.live(ae, authRepository, gameModule.invitationService)
    a: Authenticator[TestAppState] <- LiveAuthenticator.live(authRepository)
  } yield  new AuthenticationModule[TestAppState] {
    override def authenticator: Authenticator[TestAppState] = a
    override def passwordChanger: PasswordChanger[TestAppState] = pc
    override def registration: Registration[TestAppState] = r
    override def router: AuthenticationRouter[TestAppState] = new AuthenticationRouter[TestAppState](r, a, pc)
  }).runEmptyA.unsafeRunSync()

  def authenticate(credentials: Credentials): TestAppState[String] = authModule.authenticator.authenticate(credentials).map(_.jwt.toEncodedString)

  def uri(str: String): Uri = Uri.fromString(str).toOption.get

}
