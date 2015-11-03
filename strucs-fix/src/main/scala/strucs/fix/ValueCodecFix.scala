package strucs.fix

import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatterBuilder, DateTimeFormatter, ISODateTimeFormat}

import scala.util.{Success, Try}

/** typeclass. defines how basic types (String, Int, ...) can be encoded/decoded to/from FIX */
trait ValueCodecFix[A] {
  def encode(a: A): String
  def decode(s: String): Try[A]
}

object ValueCodecFix {
  implicit object StringValueCodec extends ValueCodecFix[String] {
    override def encode(a: String): String = a

    override def decode(s: String): Try[String] = Success(s)
  }

  implicit object IntValueCodec extends ValueCodecFix[Int] {
    override def encode(a: Int): String = a.toString

    override def decode(s: String): Try[Int] = Try { s.toInt }
  }


  implicit object BigDecimalValueCodec extends ValueCodecFix[BigDecimal] {
    override def encode(a: BigDecimal): String = a.toString()

    override def decode(s: String): Try[BigDecimal] = Try { BigDecimal(s) }
  }

  implicit object DateTimeValueCodec extends ValueCodecFix[DateTime] {
    private val formatter = DateTimeFormat.forPattern("yyyyMMdd-HH:mm:ss").withZoneUTC()
    override def encode(a: DateTime): String = formatter.print(a)

    override def decode(s: String): Try[DateTime] = Try { formatter.parseDateTime(s) }
  }

}