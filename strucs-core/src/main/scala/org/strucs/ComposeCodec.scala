package org.strucs

import org.strucs.Struct.Nil

import scala.language.experimental.macros
import scala.reflect.macros.whitebox



/** Defines how a Codec[Struct[_]] can be built using the codecs of its fields */
trait ComposeCodec[Codec[_]] {

  /** Build a Codec for an empty Struct */
  def zero: Codec[Struct[Nil]]

  /** Build a Codec using a field codec a and a codec b for the rest of the Struct */
  def prepend[A : StructKeyProvider, B](a: Codec[A], b: => Codec[Struct[B]]): Codec[Struct[A with B]]
}


object ComposeCodec {

  def macroImpl[Codec: c.WeakTypeTag, T : c.WeakTypeTag](c: whitebox.Context) = {
    import c.universe._

    val typeTag = implicitly[c.WeakTypeTag[T]]
    // Detect constituents using the fact that they are all value classes
    val symbols = typeTag.tpe.baseClasses.filter { sbl: Symbol =>
      sbl.isClass && sbl.asClass.isDerivedValueClass
    }

    val codecTypeTag = implicitly[c.WeakTypeTag[Codec]]
    val codecSymbol = codecTypeTag.tpe.typeSymbol

    // TODO make generic for any Codec
    def implicitCodec(typeSymbol: Symbol): Tree = q"implicitly[$codecSymbol[$typeSymbol]]"


    val composed = symbols.foldLeft[Tree](q"comp.zero"){ case (tree, sbl) =>
      q"comp.prepend(${implicitCodec(sbl)}, $tree)"
    }
    q"val comp = implicitly[ComposeCodec[$codecSymbol]]; $composed"
  }


  /** Make a Codec for a Struct[T], by calling the ComposeCodec for each constituent of T */
  def makeCodec[Codec[_], T]: Codec[Struct[T]] = macro macroImpl[Codec[_], T]
}

