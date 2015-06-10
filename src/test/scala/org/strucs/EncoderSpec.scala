package org.strucs

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Matchers, FlatSpec}
import EncoderSpec._

/**
 *
 */
class EncoderSpec extends FlatSpec with Matchers with TypeCheckedTripleEquals {



  "An Encoder[Name with Age with City]" should "be materialized with a macro" in {

    implicit val nameEncoder: EncodeCommaSeparated[Name] = new EncodeCommaSeparated[Name] {
      override def encode(t: Name): String = t.v
    }
    implicit val ageEncoder: EncodeCommaSeparated[Age] = new EncodeCommaSeparated[Age] {
      override def encode(t: Age): String = t.v.toString
    }
    implicit val cityEncoder: EncodeCommaSeparated[City] = new EncodeCommaSeparated[City] {
      override def encode(t: City): String = t.v.toString
    }

    val person = Struct.empty + Name("Bart") + Age(10) + City("Springfield")


    // TODO use implicit materializer :-D
    val encoder = EncodeCommaSeparated.make[person.tpe]
    encoder.encode(person) should === ("Bart, 10, Springfield")
  }

  // TODO try with N args
}

object EncoderSpec {
  case class Name(v: String) extends AnyVal
  case class Age(v: Int) extends AnyVal
  case class City(v: String) extends AnyVal

}
