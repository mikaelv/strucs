package org.strucs

import org.strucs.Struct.Nil

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
  /** Build a Codec for an empty Struct */
  def zero: F[Struct[Nil]]
  /** Build a Codec using a field codec and a codec for the rest of the Struct */
  def prepend[A : StructKeyProvider, B](a: F[A], b: => F[Struct[B]]): F[Struct[A with B]]
}


object EncodeCommaSeparated {

  implicit val composeEncode: ComposeCodec[EncodeCommaSeparated] = new ComposeCodec[EncodeCommaSeparated] {


    /** Build a Codec for an empty Struct */
    override def zero: EncodeCommaSeparated[Struct[Nil]] = new EncodeCommaSeparated[Struct[Nil]] {
      override def encode(t: Struct[Nil]): String = ""
    }

    override def prepend[A : StructKeyProvider, B](ea: EncodeCommaSeparated[A],
                                                   eb: => EncodeCommaSeparated[Struct[B]]): EncodeCommaSeparated[Struct[A with B]] = new EncodeCommaSeparated[Struct[A with B]] {
      override def encode(t: Struct[A with B]): String = {
        val encodea = ea.encode(t.get[A]) 
        val encodeb = eb.encode(t.asInstanceOf[Struct[B]]) // TODO implement shrink ?
        if (encodeb != zero.encode(Struct.empty))
          encodea +", "+encodeb
        else
          encodea

      }
    }
  }


  def macroImpl[T : c.WeakTypeTag](c: whitebox.Context) = {
    import c.universe._

    val typeTag = implicitly[c.WeakTypeTag[T]]
    // Detect constituents using the fact that they are all value classes
    val symbols = typeTag.tpe.baseClasses.filter { sbl: Symbol =>
      sbl.isClass && sbl.asClass.isDerivedValueClass
    }

    // TODO make generic for any Codec
    def implicitCodec(typeSymbol: Symbol): Tree = q"implicitly[EncodeCommaSeparated[$typeSymbol]]"


    val composed = symbols.foldLeft[Tree](q"comp.zero"){ case (tree, sbl) =>
      q"comp.prepend(${implicitCodec(sbl)}, $tree)"
    }
    q"val comp = implicitly[ComposeCodec[EncodeCommaSeparated]]; $composed"
  }


  def make[T]: EncodeCommaSeparated[Struct[T]] = macro macroImpl[T]
}


