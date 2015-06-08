package org.strucs

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Matchers, FlatSpec}
import EncoderSpec._

/**
 *
 */
class EncoderSpec extends FlatSpec with Matchers with TypeCheckedTripleEquals {



  "An Encoder[Name with Age]" should "be materialized with a macro" in {

    implicit val nameEncoder: EncodeCommaSeparated[Name] = new EncodeCommaSeparated[Name] {
      override def encode(t: Name): String = t.v
    }
    implicit val ageEncoder: EncodeCommaSeparated[Age] = new EncodeCommaSeparated[Age] {
      override def encode(t: Age): String = t.v.toString
    }
    val person = Struct(Name("Bart")).add(Age(10))


    // TODO use implicit materializer :-D
    val encoder = EncodeCommaSeparated.make[person.tpe]
    encoder.encode(person) should === ("Bart, 10")
  }

  // TODO try with N args
}

object EncoderSpec {
  case class Name(v: String) extends AnyVal
  case class Age(v: Int) extends AnyVal

}
