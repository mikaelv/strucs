package strucs

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FlatSpec, Matchers}
import strucs.ComposeCodec._
import strucs.EncoderSpec._
import strucs.Struct.Nil

/**
 *
 */
class EncoderSpec extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  "An EncodeCommaSeparated[Struct[Name with Age with City]]" should "be created with a macro" in {
    // TODO add an enum
    val person = Struct.empty + Name("Bart") + Age(10) + City("Springfield")

    val encoder: EncodeCommaSeparated[Struct[Name with Age with City with Nil]] = ComposeCodec.makeCodec[EncodeCommaSeparated, Name with Age with City with Nil]
    encoder.encode(person) should === ("Bart, 10, Springfield")
  }
}

object EncoderSpec {
  /**
  * Dummy codec: outputs the content of each field, separated by commas
  */
  trait EncodeCommaSeparated[A] {
    def encode(a: A): String
  }


  object EncodeCommaSeparated {

    implicit val monoid = new Monoid[String] {
      override def zero: String = ""

      override def append(f1: String, f2: String): String =
        if (f1 == "") f2
        else if (f2 == "") f1
        else f1 + ", " + f2
    }

    implicit val trans = new TransformEncode[EncodeCommaSeparated, String] {
      override def fromFunc[A](_encode: (A) => String) = new EncodeCommaSeparated[A] {
        override def encode(a: A): String = _encode(a)
      }

      override def toFunc[A](enc: EncodeCommaSeparated[A]) = enc.encode
    }
    implicit val composeEncode: ComposeCodec[EncodeCommaSeparated] = ComposeCodec.makeComposeCodec[EncodeCommaSeparated, String]

  }


  case class Name(v: String) extends AnyVal
  case class Age(v: Int) extends AnyVal
  case class City(v: String) extends AnyVal

  implicit val nameEncoder: EncodeCommaSeparated[Name] = new EncodeCommaSeparated[Name] {
    override def encode(t: Name): String = t.v
  }
  implicit val ageEncoder: EncodeCommaSeparated[Age] = new EncodeCommaSeparated[Age] {
    override def encode(t: Age): String = t.v.toString
  }
  implicit val cityEncoder: EncodeCommaSeparated[City] = new EncodeCommaSeparated[City] {
    override def encode(t: City): String = t.v.toString
  }
}
