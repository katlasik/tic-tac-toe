package io.tictactoe.utilities.templates.model

import cats.Show
import shapeless.labelled.{FieldType, field}
import shapeless.ops.hlist.Mapper
import shapeless.ops.record.ToMap
import shapeless.{HList, LabelledGeneric, Poly1}

import scala.annotation.implicitNotFound
import scala.util.chaining._

final case class TemplateDataValues protected (values: Map[String, String], templatePath: String)

/**
  * Every member field of class extending TemplateData has to have Show in scope.
  */
trait TemplateData { self: Product =>
  val path: String
}

object TemplateData {

  private def values[A <: TemplateData, ARepr <: HList, BRepr <: HList, K <: Symbol](a: A)(
      implicit
      genIn: LabelledGeneric.Aux[A, ARepr],
      mapper: Mapper.Aux[showMapper.type, ARepr, BRepr],
      toMap: ToMap.Aux[BRepr, K, String]
  ): TemplateDataValues =
    toMap(genIn.to(a).map(showMapper))
      .map {
        case (k: Symbol, v) => k.name -> v
      }
      .pipe(TemplateDataValues(_, a.path))

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitConversion"))
  implicit def dataToValues[A <: TemplateData, ARepr <: HList, BRepr <: HList, K <: Symbol](data: A)(
      implicit
      genIn: LabelledGeneric.Aux[A, ARepr],
      mapper: Mapper.Aux[showMapper.type, ARepr, BRepr],
      toMap: ToMap.Aux[BRepr, K, String]
  ): TemplateDataValues = values[A, ARepr, BRepr, K](data)

  private object showMapper extends Poly1 {
    @implicitNotFound("Every member of class has to have Show instance in scope!")
    implicit def caseShow[K <: Symbol, V](implicit show: Show[V]): Case.Aux[FieldType[K, V], FieldType[K, String]] =
      at[FieldType[K, V]](s => field[K](show.show(s)))
  }

}
