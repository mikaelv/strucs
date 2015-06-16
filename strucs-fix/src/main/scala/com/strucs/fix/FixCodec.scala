package com.strucs.fix

import org.strucs.Struct.Nil
import org.strucs.{StructKeyProvider, ComposeCodec, Wrapper, Struct}

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
      override def encode(a: Struct[Nil]): FixElement = FixNil
    }

  }
}