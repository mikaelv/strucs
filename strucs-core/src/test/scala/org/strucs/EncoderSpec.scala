package org.strucs

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Matchers, FlatSpec}
import EncoderSpec._
import org.strucs.Struct.Nil

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
  trait EncodeCommaSeparated[T] {
    def encode(t: T): String
  }

  object EncodeCommaSeparated {

    implicit val composeEncode: ComposeCodec[EncodeCommaSeparated] = new ComposeCodec[EncodeCommaSeparated] {


      /** Build a Codec for an empty Struct */
      override def zero: EncodeCommaSeparated[Struct[Nil]] = new EncodeCommaSeparated[Struct[Nil]] {
        override def encode(t: Struct[Nil]): String = ""
      }
      /** Build a Codec using a field codec ea and a codec eb for the rest of the Struct */
      override def prepend[A: StructKeyProvider, B](ea: EncodeCommaSeparated[A],
                                                    eb: => EncodeCommaSeparated[Struct[B]]): EncodeCommaSeparated[Struct[A with B]] = new EncodeCommaSeparated[Struct[A with B]] {
        override def encode(t: Struct[A with B]): String = {
          val encodea = ea.encode(t.get[A])
          val encodeb = eb.encode(t.shrink[B])
          if (encodeb != zero.encode(Struct.empty))
            encodea + ", " + encodeb
          else
            encodea

        }
      }
    }
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
