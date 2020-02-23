package io.tictactoe.testutils.generators

import io.tictactoe.authentication.model.User
import io.tictactoe.authentication.services.Hash
import io.tictactoe.values.{Email, UserId, Username}
import org.scalacheck.Gen

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

  def userId(): Gen[UserId] = for {
    id <- Gen.uuid
  } yield UserId(id)

  def username(): Gen[Username] =
    for {
      usernameSize <- Gen.choose(2, 100)
      username <- Gen.listOfN(usernameSize, Gen.alphaChar).map(_.mkString)
    } yield Username.fromString(username).toOption.get

  def user(): Gen[User] =
    for {
      id <- userId()
      hash <- hash()
      email <- email()
      username <- username()
    } yield User(id, username, hash, email)

  def users(from: Int = 200, to: Int = 1000): Gen[List[User]] = for {
    numberOfUsers <- Gen.choose(from, to)
    users <- Gen.listOfN(numberOfUsers, Generators.user())
  } yield users

}
