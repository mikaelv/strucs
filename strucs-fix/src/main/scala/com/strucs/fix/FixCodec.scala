package com.strucs.fix

import org.strucs.Struct.Nil
import org.strucs.{StructKeyProvider, ComposeCodec, Wrapper, Struct}

import scala.util.Try

/**
 * typeclass. Defines how a Struct can be encoded/decoded to/from FIX.
 */
trait FixCodec[A] {
  def encode(a: A): FixElement
}



object FixCodec {

  class FixTagCodec[W, V](tag: Int)(implicit wrapper: Wrapper[W, V], valueCodec: FixValueCodec[V]) extends FixCodec[W] {
    override def encode(a: W): FixElement = FixTagValue(tag, valueCodec.encode(wrapper.value(a)))
  }


  // TODO generalize with a codec that returns a B : Monoid
  implicit object ComposeFixCodec extends ComposeCodec[FixCodec] {
    /** Build a Codec using a field codec a and a codec b for the rest of the Struct */
    override def prepend[A: StructKeyProvider, B](ca: FixCodec[A], cb: => FixCodec[Struct[B]]): FixCodec[Struct[A with B]] = new FixCodec[Struct[A with B]] {
      override def encode(a: Struct[A with B]): FixElement = {
        val bfix = cb.encode(a.shrink[B])
        val afix = ca.encode(a.get[A])
        afix + bfix
      }

    }

    /** Build a Codec for an empty Struct */
    override def zero: FixCodec[Struct[Nil]] = new FixCodec[Struct[Nil]] {
      override def encode(a: Struct[Nil]): FixElement = FixGroup.empty
    }

  }

  /** Automatically create a FixCodec for any Struct[A]
    * @tparam A mixin, each type of the mixin having a FixCodec */
  implicit def makeFixCodec[A]: FixCodec[Struct[A]] = ComposeCodec.makeCodec[FixCodec, A]

  /** Pimp Struct with helpful methods */
  implicit class FixCodecOps[A](struct: Struct[A])(implicit codec: FixCodec[Struct[A]]) {
    def toFixMessage: FixMessage = {
      // TODO extract 8 and 35 from A
      FixMessage("FIX.4.2", "D", codec.encode(struct).toGroup)
    }

    def toFixMessageString: String = toFixMessage.toFixString
  }
}