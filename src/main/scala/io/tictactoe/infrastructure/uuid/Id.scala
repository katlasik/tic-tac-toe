package io.tictactoe.infrastructure.uuid

import java.util.UUID

import cats.Functor
import shapeless.{::, Generic, HNil}
import cats.syntax.functor._

object Id {

  def create[G <: AnyVal](id: UUID)(implicit repr: Generic.Aux[G, UUID :: HNil]): G = repr.from(id :: HNil)

  def next[F[_]: Functor: UUIDGenerator, G <: AnyVal](implicit repr: Generic.Aux[G, UUID :: HNil]): F[G] =
    for {
      id <- UUIDGenerator[F].next()
    } yield repr.from(id :: HNil)

  def unsafeFromString[G <: AnyVal](value: String)(implicit repr: Generic.Aux[G, UUID :: HNil]): G =
    repr.from(UUID.fromString(value) :: HNil)

}

trait Id[G <: AnyVal] {

  def next[F[_]: Functor: UUIDGenerator](implicit repr: Generic.Aux[G, UUID :: HNil]): F[G] = Id.next[F, G]

  def unsafeFromString(value: String)(implicit repr: Generic.Aux[G, UUID :: HNil]): G = Id.unsafeFromString[G](value)

}
