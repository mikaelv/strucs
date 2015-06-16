package com.strucs.fix

import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatterBuilder, DateTimeFormatter, ISODateTimeFormat}

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

  implicit object DateTimeValueCodec extends FixValueCodec[DateTime] {
    private val formatter = DateTimeFormat.forPattern("yyyyMMdd-HH:mm:ss")
    override def encode(a: DateTime): String = formatter.print(a.toDateTime(DateTimeZone.UTC))
  }

}