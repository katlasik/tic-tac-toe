package io.tictactoe.testutils

import cats.effect.{ContextShift, IO}
import io.tictactoe.modules.authentication.AuthenticationModule
import io.tictactoe.modules.authentication.api.{AuthEmail, AuthRepository, Authenticator, PasswordChanger, Registration}
import io.tictactoe.modules.authentication.domain.{LiveAuthEmail, LiveAuthenticator, LiveRegistration, LivePasswordChanger}
import io.tictactoe.modules.authentication.infrastructure.effects.PasswordHasher
import io.tictactoe.modules.authentication.model.Credentials
import io.tictactoe.modules.authentication.infrastructure.routes.AuthenticationRouter
import io.tictactoe.modules.game.GameModule
import io.tictactoe.utilities.events.EventBus
import io.tictactoe.utilities.logging.Logging
import io.tictactoe.utilities.templates.TemplateRenderer
import io.tictactoe.utilities.tokens.TokenGenerator
import io.tictactoe.utilities.uuid.UUIDGenerator
import io.tictactoe.utilities.calendar.Calendar
import io.tictactoe.utilities.emails.services.LiveEmailSender
import io.tictactoe.modules.game.infrastructure.emails.InvitationEmail
import io.tictactoe.modules.game.domain.services.{LiveGameInvitationService, LiveGameService}
import io.tictactoe.modules.game.infrastructure.routes.GameRouter
import io.tictactoe.modules.game.infrastructure.services.{GameInvitationService, GameService}
import io.tictactoe.utilities.configuration.Configuration
import io.tictactoe.utilities.emails.{EmailRepository, EmailSender, EmailTransport}
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.testutils.mocks.{BypassingPasswordHasher, FixedCalendar, FixedConfirmationTokenGenerator, FixedRandomInt, FixedUUIDGenerator, InMemoryAuthRepository, InMemoryEmailRepository, InMemoryEventBus, InMemoryGameRepository, InMemoryInvitationRepository, InMemoryUserRepository, MemoryLogging, MockedEmailTransport}
import io.tictactoe.modules.users.UserModule
import io.tictactoe.modules.users.domain.services.LiveUserService
import io.tictactoe.modules.users.infrastructure.routes.UserRouter
import io.tictactoe.modules.users.infrastructure.services.UserService
import io.tictactoe.utilities.authentication.Authentication
import io.tictactoe.utilities.random.{RandomInt, RandomPicker}
import io.tictactoe.utilities.routes.Router
import org.http4s.Uri
import org.http4s.dsl.Http4sDsl

import scala.concurrent.ExecutionContext

trait Fixture extends Http4sDsl[TestAppState]{

  implicit val cs: ContextShift[IO]  = IO.contextShift(ExecutionContext.global)

  implicit val uuidGenerator: UUIDGenerator[TestAppState] = FixedUUIDGenerator.fixed
  implicit val eventBus: EventBus[TestAppState] = InMemoryEventBus.inMemory
  implicit val logging: Logging[TestAppState] = MemoryLogging.memory
  implicit val calendar: Calendar[TestAppState] = FixedCalendar.fixed
  implicit val passwordHasher: PasswordHasher[TestAppState] = BypassingPasswordHasher.bypassing
  implicit val confirmationTokenGenerator: TokenGenerator[TestAppState] = FixedConfirmationTokenGenerator.fixed
  implicit val randomInt: RandomInt[TestAppState] = FixedRandomInt.fixed
  implicit val randomPicker: RandomPicker[TestAppState] = RandomPicker.live[TestAppState]

  val authRepository: AuthRepository[TestAppState] = InMemoryAuthRepository.inMemory
  val mockedEmailTransport: EmailTransport[TestAppState] = MockedEmailTransport.mocked
  val inMemoryEmailRepository: EmailRepository[TestAppState] = InMemoryEmailRepository.inMemory

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
    r = InMemoryGameRepository.inMemory
  } yield new GameModule[TestAppState] {
    override def gameService: GameService[TestAppState] = LiveGameService.live(r)
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
