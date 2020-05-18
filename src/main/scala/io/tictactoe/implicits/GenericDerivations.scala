package io.tictactoe.implicits

import cats.{Eq, Order, Show}
import io.circe.generic.extras.decoding.UnwrappedDecoder
import io.circe.generic.extras.encoding.UnwrappedEncoder
import io.circe.generic.extras.semiauto.{deriveUnwrappedDecoder, deriveUnwrappedEncoder}
import io.circe.{Decoder, Encoder}
import shapeless.{Generic, HList, HNil}
import shapeless.ops.hlist.IsHCons
import sttp.tapir.Codec.PlainCodec

trait GenericDerivations {

  implicit def genericValueClassShow[T <: AnyVal, TRepr <: HList, V](
      implicit
      tRepr: Generic.Aux[T, TRepr],
      isHCons: IsHCons.Aux[TRepr, V, HNil],
      vShow: Show[V]
  ): Show[T] = Show.show(t => vShow.show(tRepr.to(t).head))

  implicit def genericValueClassEq[T <: AnyVal, TRepr <: HList, V](
      implicit
      tRepr: Generic.Aux[T, TRepr],
      isHCons: IsHCons.Aux[TRepr, V, HNil],
      vEq: Eq[V]
  ): Eq[T] = Eq.by(tRepr.to(_).head)

  implicit def genericValueClassOrder[T <: AnyVal, TRepr <: HList, V](
      implicit
      tRepr: Generic.Aux[T, TRepr],
      isHCons: IsHCons.Aux[TRepr, V, HNil],
      vEq: Order[V]
  ): Order[T] = Order.by(tRepr.to(_).head)

  implicit def genericValueClassEncoder[T <: AnyVal: UnwrappedEncoder]: Encoder[T] = deriveUnwrappedEncoder[T]

  implicit def genericValueClassDecoder[T <: AnyVal: UnwrappedDecoder]: Decoder[T] = deriveUnwrappedDecoder[T]

  implicit def genericValueClassCodec[T <: AnyVal, TRepr <: HList, V](
      implicit
      tRepr: Generic.Aux[T, TRepr],
      isHCons: IsHCons.Aux[TRepr, V, HNil],
      vCodec: PlainCodec[V]
  ): PlainCodec[T] = vCodec.map(v => tRepr.from(isHCons.cons(v, HNil)))(tRepr.to(_).head)

}
