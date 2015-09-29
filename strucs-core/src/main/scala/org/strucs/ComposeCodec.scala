package org.strucs

import org.strucs.Struct.Nil

import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import languageFeature.higherKinds



/** Defines how a Codec[Struct[_]] can be built using the codecs of its fields */
trait ComposeCodec[Codec[_]] {

  /** Build a Codec for an empty Struct */
  def zero: Codec[Struct[Nil]]

  /** Build a Codec using a field codec a and a codec b for the rest of the Struct */
  def prepend[A : StructKeyProvider, B](a: Codec[A], b: => Codec[Struct[B]]): Codec[Struct[A with B]]
}


object ComposeCodec {

  /** Make a Codec for a Struct[T], by calling the ComposeCodec for each constituent of T */
  def makeCodec[Codec[_], T]: Codec[Struct[T]] = macro macroImpl[Codec[_], T]



  def macroImpl[Codec: c.WeakTypeTag, T : c.WeakTypeTag](c: blackbox.Context) = {
    import c.universe._
    def info(msg: String) = c.info(c.enclosingPosition, "org.structs.ComposeCodec - "+msg, true)

    val nilSymbol = typeOf[Nil].typeSymbol
    def extractFieldsSymbols(mixin: Type, acc: List[Symbol] = List.empty): List[Symbol] = mixin match {
      case RefinedType(types, _) => types.foldLeft(acc){ (symbols, tpe) => extractFieldsSymbols(tpe, symbols) }
      case TypeRef(pre, sym, args) => extractFieldsSymbols(sym.typeSignatureIn(pre), acc)
      case t if t.typeSymbol == nilSymbol => acc
      case _ =>
        val v = mixin.typeSymbol +: acc
        //info("extracted symbol: "+mixin)
        v
    }


    val typeTag = implicitly[c.WeakTypeTag[T]]
    // Extract constituents
    //info("creating codec for type: "+typeTag.tpe.toString)

    val symbols = extractFieldsSymbols(typeTag.tpe)
    //info("extracted symbols: "+symbols.mkString(", "))

    val codecTypeTag = implicitly[c.WeakTypeTag[Codec]]
    val codecSymbol = codecTypeTag.tpe.typeSymbol

    def implicitCodec(typeSymbol: Symbol): Tree = q"implicitly[$codecSymbol[$typeSymbol]]"


    val composed = symbols.foldLeft[Tree](q"comp.zero"){ case (tree, sbl) =>
      q"comp.prepend(${implicitCodec(sbl)}, $tree)"
    }
    val codec = q"val comp = implicitly[ComposeCodec[$codecSymbol]]; $composed.asInstanceOf[$codecSymbol[Struct[${typeTag.tpe}]]]"
    //info("codec = "+codec.toString)
    codec
  }



}


