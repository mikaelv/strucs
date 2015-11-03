package strucs

import strucs.Struct.Nil

import scala.language.experimental.macros
import scala.languageFeature.higherKinds
import scala.reflect.macros.blackbox



/** Defines how a Codec[Struct[_]] can be built using the codecs of its fields */
trait ComposeCodec[Codec[_]] {

  /** Build a Codec for an empty Struct */
  def zero: Codec[Struct[Nil]]

  /** Build a Codec using a field codec a and a codec b for the rest of the Struct */
  def prepend[A : StructKeyProvider, B](ca: Codec[A], cb: => Codec[Struct[B]]): Codec[Struct[A with B]]

}

trait Monoid[F] {
  def zero: F
  def prepend(a: F, b: F): F
}



object ComposeCodec {
  /** Conversion from/to a function A=>T to/from an Encode[A] */ 
  trait ConvertEncode[Encode[_], T] {
    def fromFunc[A](encode: A => T): Encode[A]
    def toFunc[A](enc: Encode[A]): A => T
  }

  /** If we know how to compose the encoded values T (using Monoid[T]),
    * we can define how to compose an Encoder to T
    */
  def makeComposeCodec[Enc[_], T](implicit tMonoid: Monoid[T], conv: ConvertEncode[Enc, T]) = new ComposeCodec[Enc] {

    /** Build a Codec for an empty Struct */
    override def zero: Enc[Struct[Nil]] = conv.fromFunc {a: Struct[Nil] => tMonoid.zero}

    /** Build a Codec using a field codec a and a codec b for the rest of the Struct */
    override def prepend[A: StructKeyProvider, B](ca: Enc[A], 
                                                  cb: => Enc[Struct[B]]): Enc[Struct[A with B]] = conv.fromFunc { s: Struct[A with B] =>
      val bencoded = conv.toFunc[Struct[B]](cb)(s.shrink[B])
      val aencoded = conv.toFunc[A](ca)(s.get[A])
      tMonoid.prepend(aencoded, bencoded)
    }
  }


  /** Make a Codec for a Struct[T], by calling the ComposeCodec for each constituent of T */
  def makeCodec[Codec[_], T]: Codec[Struct[T]] = macro macroImpl[Codec[_], T]



  def macroImpl[Codec: c.WeakTypeTag, T : c.WeakTypeTag](c: blackbox.Context) = {
    def info(msg: String) = c.info(c.enclosingPosition, "strucs.ComposeCodec - "+msg, true)

    // Extract constituents
    import c.universe._
    val typeTag: c.universe.WeakTypeTag[T] = implicitly[WeakTypeTag[T]]
    info("creating codec for type: "+typeTag.tpe.toString)

    val types = FieldExtractor.extractTypes(typeTag.tpe)(c.universe)
    //info("extracted types: "+types.mkString(", "))

    val codecTypeTag = implicitly[WeakTypeTag[Codec]]
    val codecSymbol = codecTypeTag.tpe.typeSymbol

    def implicitCodec(tpe: Type): Tree = q"implicitly[$codecSymbol[$tpe]]"

    val composed = types.foldLeft[Tree](q"comp.zero"){ case (tree, tpe) =>
      q"comp.prepend(${implicitCodec(tpe.asInstanceOf[Type])}, $tree)"
    }
    val codec = q"val comp = implicitly[strucs.ComposeCodec[$codecSymbol]]; $composed.asInstanceOf[$codecSymbol[Struct[${typeTag.tpe}]]]"
    info("codec = "+codec.toString)
    codec
  }



}


