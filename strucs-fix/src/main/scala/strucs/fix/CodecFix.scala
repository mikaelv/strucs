package strucs.fix

import strucs.{StructKeyProvider, Struct, ComposeCodec}
import strucs.StructKeyProvider
import Struct.Nil
import strucs.Struct
import strucs.fix.dict.fix42.{MsgType, BeginString}
import scala.language.experimental.macros

import scala.util.{Failure, Success, Try}

/**
 * typeclass. Defines how a Struct can be encoded/decoded to/from FIX.
 */
trait CodecFix[A] {
  def encode(a: A): FixElement
  def decode(fix: FixElement): Try[A]
}




object CodecFix {
  /** Automatically create a FixCodec for any Struct[A]
    * @tparam T mixin, each type M in the mixin must have an implicit FixCodec[M] in scope */
  implicit def makeFixCodec[T]: CodecFix[Struct[T]] = macro ComposeCodec.macroImpl[CodecFix[_], T]



  // TODO generalize with a codec that returns a B : Monoid ?
  implicit object ComposeFixCodec extends ComposeCodec[CodecFix] {
    /** Build a Codec using a field codec a and a codec b for the rest of the Struct */
    override def prepend[A: StructKeyProvider, B](ca: CodecFix[A], cb: => CodecFix[Struct[B]]): CodecFix[Struct[A with B]] = new CodecFix[Struct[A with B]] {
      override def encode(a: Struct[A with B]): FixElement = {
        val bfix = cb.encode(a.shrink[B])
        val afix = ca.encode(a.get[A])
        afix + bfix
      }

      override def decode(fix: FixElement): Try[Struct[A with B]] = {
        for {
          structb <- cb.decode(fix)
          a <- ca.decode(fix)

        } yield structb.add[A](a)
      }
    }

    /** Build a Codec for an empty Struct */
    override def zero: CodecFix[Struct[Nil]] = new CodecFix[Struct[Nil]] {
      override def encode(a: Struct[Nil]): FixElement = FixGroup.empty

      override def decode(fix: FixElement): Try[Struct[Nil]] = Success(Struct.empty)
    }

  }


  /** Pimp Struct with helpful methods */
  implicit class EncodeFixOps[A <: MsgType with BeginString](struct: Struct[A])(implicit codec: CodecFix[Struct[A]]) {
    def toFixMessage: FixMessage = {
      FixMessage(
        BeginString.codec.encode(struct.get[BeginString]),
        MsgType.codec.encode(struct.get[MsgType]),
        codec.encode(struct).toGroup.remove(Set(MsgType.codec.tag, BeginString.codec.tag)))
    }

    def toFixMessageString: String = toFixMessage.toFixString
  }

  implicit class DecodeFixOps(fixString: String) {
    def toStruct[A](implicit codec: CodecFix[Struct[A]]): Try[Struct[A]] = {
      for {
        fixMsg <- FixMessage.decode(fixString)
        struct <- codec.decode(fixMsg)
      } yield struct
    }
  }

}