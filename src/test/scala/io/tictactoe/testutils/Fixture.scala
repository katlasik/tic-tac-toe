package io.tictactoe.testutils

import cats.effect.{ContextShift, IO}
import io.tictactoe.authentication.model.Credentials
import io.tictactoe.authentication.repositories.AuthRepository
import io.tictactoe.authentication.services.{Authentication, PasswordHasher, Registration}
import io.tictactoe.base.logging.Logging
import io.tictactoe.base.uuid.UUIDGenerator
import io.tictactoe.calendar.Calendar
import io.tictactoe.events.bus.EventBus
import io.tictactoe.testutils.TestAppData.TestAppState
import io.tictactoe.testutils.mocks.{BypassingPasswordHasher, FixedCalendar, FixedUUIDGenerator, InMemoryAuthRepository, InMemoryEventBus, InMemoryUserRepository, MemoryLogging}
import io.tictactoe.users.repositories.UserRepository
import io.tictactoe.users.services.UserService
import org.http4s.Uri
import org.http4s.dsl.Http4sDsl

import scala.concurrent.ExecutionContext

trait Fixture {

  val dsl = new Http4sDsl[TestAppState] {}

  implicit val cs: ContextShift[IO]  = IO.contextShift(ExecutionContext.global)

  val emptyData = TestAppData()

  lazy implicit val logging: Logging[TestAppState] = MemoryLogging.memory
  lazy implicit val uuidGenerator: UUIDGenerator[TestAppState] = FixedUUIDGenerator.fixed
  lazy implicit val passwordHasher: PasswordHasher[TestAppState] = BypassingPasswordHasher.bypassing
  lazy implicit val authRepository: AuthRepository[TestAppState] = InMemoryAuthRepository.inMemory
  lazy implicit val userRepository: UserRepository[TestAppState] = InMemoryUserRepository.inMemory
  lazy implicit val eventBus: EventBus[TestAppState] = InMemoryEventBus.inMemory


  lazy implicit val calendar: Calendar[TestAppState] = FixedCalendar.fixed
  lazy implicit val registration: Registration[TestAppState] = Registration.live
  lazy implicit val authentication: Authentication[TestAppState] = Authentication.live.runA(emptyData).unsafeRunSync()
  lazy implicit val userService: UserService[TestAppState] = UserService.live

  def authenticate(credentials: Credentials): TestAppState[String] = authentication.authenticate(credentials).map(_.jwt.toEncodedString)

  def uri(str: String): Uri = Uri.fromString(str).toOption.get

}
