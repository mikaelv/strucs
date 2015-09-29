package com.strucs.fix

import com.strucs.fix.dict.fix42.{MsgType, BeginString}
import org.strucs.Struct.Nil
import org.strucs.{StructKeyProvider, ComposeCodec, Wrapper, Struct}
import scala.language.experimental.macros

import scala.util.{Failure, Success, Try}

/**
 * typeclass. Defines how a Struct can be encoded/decoded to/from FIX.
 */
trait FixCodec[A] {
  def encode(a: A): FixElement
  def decode(fix: FixElement): Try[A]
}




object FixCodec {
  /** Automatically create a FixCodec for any Struct[A]
    * @tparam T mixin, each type of the mixin must have a FixCodec */
  implicit def makeFixCodec[T]: FixCodec[Struct[T]] = macro ComposeCodec.macroImpl[FixCodec[_], T]


  /** Encodes a single tag */
  class FixTagCodec[W, V](val tag: Int)(implicit wrapper: Wrapper[W, V], valueCodec: FixValueCodec[V]) extends FixCodec[W] {
    override def encode(a: W): FixTagValue = FixTagValue(tag, valueCodec.encode(wrapper.value(a)))

    /** @param fix is always a FixGroup when called from outside */
    override def decode(fix: FixElement): Try[W] = fix match {
      case FixTagValue(t, value) if t == tag => valueCodec.decode(value) flatMap {
        wrapper.make(_).map(Success(_)).getOrElse(Failure(new FixDecodeException(s"Wrapper: $wrapper cannot parse $value in tag $t")))
      }

      // get the FixTagValue and call decode again
      case g@FixGroup(pairs) => g.get(tag).map(decode).getOrElse(Failure(new FixDecodeException(s"Cannot find tag $tag in $fix")))
      case m@FixMessage(beginStr, msgType, body) =>
        if (tag == MsgType.Tag)
          decode(msgType)
        else if (tag == BeginString.Tag)
          decode(beginStr)
        else
          body.get(tag).map(decode).getOrElse(Failure(new FixDecodeException(s"Cannot find tag $tag in $fix")))

      case _ => Failure(new FixDecodeException(s"Cannot decode $fix"))
    }
  }


  // TODO generalize with a codec that returns a B : Monoid ?
  implicit object ComposeFixCodec extends ComposeCodec[FixCodec] {
    /** Build a Codec using a field codec a and a codec b for the rest of the Struct */
    override def prepend[A: StructKeyProvider, B](ca: FixCodec[A], cb: => FixCodec[Struct[B]]): FixCodec[Struct[A with B]] = new FixCodec[Struct[A with B]] {
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
    override def zero: FixCodec[Struct[Nil]] = new FixCodec[Struct[Nil]] {
      override def encode(a: Struct[Nil]): FixElement = FixGroup.empty

      override def decode(fix: FixElement): Try[Struct[Nil]] = Success(Struct.empty)
    }

  }


  /** Pimp Struct with helpful methods */
  implicit class FixEncodeOps[A <: MsgType with BeginString](struct: Struct[A])(implicit codec: FixCodec[Struct[A]]) {
    def toFixMessage: FixMessage = {
      FixMessage(
        BeginString.codec.encode(struct.get[BeginString]),
        MsgType.codec.encode(struct.get[MsgType]),
        codec.encode(struct).toGroup.remove(Set(MsgType.codec.tag, BeginString.codec.tag)))
    }

    def toFixMessageString: String = toFixMessage.toFixString
  }

  implicit class FixDecodeOps(fixString: String) {
    def toStruct[A](implicit codec: FixCodec[Struct[A]]): Try[Struct[A]] = {
      for {
        fixMsg <- FixMessage.decode(fixString)
        struct <- codec.decode(fixMsg)
      } yield struct
    }
  }

}