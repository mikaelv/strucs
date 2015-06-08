package org.strucs

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

/**
 * Dummy codec
 */
trait EncodeCommaSeparated[T] {
  def encode(t: T): String
}

/** Defines how a Codec[Struct[_]] can be built using the codecs of its fields */
trait ComposeCodec[F[_]] {
  self =>
  /** Build a Codec for a single-field Struct */
  def pure[A : StructKeyProvider](a: F[A]): F[Struct[A]]
  /** Build a Codec using a field codec and a codec for the rest of the Struct */
  def compose[A : StructKeyProvider, B](a: F[A], b: => F[Struct[B]]): F[Struct[A with B]]
}


object EncodeCommaSeparated {

  implicit val composeEncode: ComposeCodec[EncodeCommaSeparated] = new ComposeCodec[EncodeCommaSeparated] {

    override def pure[A : StructKeyProvider](ea: EncodeCommaSeparated[A]): EncodeCommaSeparated[Struct[A]] = new EncodeCommaSeparated[Struct[A]] {
      override def encode(t: Struct[A]): String = ea.encode(t.get[A])
    }

    override def compose[A : StructKeyProvider, B](ea: EncodeCommaSeparated[A], eb: => EncodeCommaSeparated[Struct[B]]): EncodeCommaSeparated[Struct[A with B]] = new EncodeCommaSeparated[Struct[A with B]] {
      override def encode(t: Struct[A with B]): String = {
        ea.encode(t.get[A]) +
          ", " +
          eb.encode(t.asInstanceOf[Struct[B]]) // TODO implement shrink ?
      }
    }
  }


  def macroImpl[T : c.WeakTypeTag](c: whitebox.Context) = {
    import c.universe._

    // TODO 1 detect constituents => use asClass.isDerivedValueClass as a first trick !
    val typeTag = implicitly[c.WeakTypeTag[T]]
    val symbols = typeTag.tpe.baseClasses.filter { sbl: Symbol => sbl.isClass && sbl.asClass.isDerivedValueClass }
    val symbol1 = typeTag.tpe.baseClasses.head
    val symbol2 = symbols.head
    val symbol3 = symbols.last


    // TODO make generic for any Codec
    val t1 = q"implicitly[EncodeCommaSeparated[$symbol1]]"
    val t2 = q"implicitly[EncodeCommaSeparated[$symbol2]]"
    val t3 = q"implicitly[EncodeCommaSeparated[$symbol3]]"

    val compose = q"implicitly[ComposeCodec[EncodeCommaSeparated]]"
    q"""$compose.compose($t3, $compose.pure($t2))"""
  }


  def make[T]: EncodeCommaSeparated[Struct[T]] = macro macroImpl[T]
}


