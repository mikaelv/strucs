package com.strucs.fix

import org.strucs.{Wrapper, Struct}

/**
 * typeclass. Defines how a type can be encoded/decoded to/from FIX.
 */
trait FixCodec[A] {
  def encode(a: A): FixElement
}


object FixCodec {

  class FixTag[W, V](tag: Int)(implicit wrapper: Wrapper[W, V], valueCodec: FixValueCodec[V]) extends FixCodec[W] {
    override def encode(a: W): FixElement = FixTagValue(tag, valueCodec.encode(wrapper.value(a)))
  }
}