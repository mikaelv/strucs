package com.strucs.fix

/** typeclass. defines how basic types (String, Int, ...) can be encoded/decoded to/from FIX */
trait FixValueCodec[A] {
  def encode(a: A): String
}

object FixValueCodec {
  implicit object StringValueCodec extends FixValueCodec[String] {
    override def encode(a: String): String = a
  }

  implicit object IntValueCodec extends FixValueCodec[Int] {
    override def encode(a: Int): String = a.toString
  }

}