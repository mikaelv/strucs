package com.strucs.json.argonaut

import argonaut.{DecodeJson, EncodeJson}
import com.strucs.json.argonaut.CodecJsonSpec.Gender.Male
import com.strucs.json.argonaut.CodecJsonSpec._
import com.strucs.json.argonaut.StrucsEncodeJson._
import com.strucs.json.argonaut.StrucsDecodeJson._
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FlatSpec, Matchers}
import org.strucs.Struct.Nil
import org.strucs.{Wrapper, Struct}
import argonaut._, Argonaut._
import scalaz.{-\/, \/-}

/**
 */
class CodecJsonSpec  extends FlatSpec with Matchers with TypeCheckedTripleEquals {
  val person = Struct.empty + Name("Albert") + Age(76) + City("Princeton") + (Male: Gender)
  
  "an EncodeJson" should "encode a Person" in {
    val json = person.toJsonString
    json should === ("""{"name":"Albert","age":76,"city":"Princeton","gender":"M"}""")
  }

  "a DecodeJson" should "decode a json string into a Person" in {
    val json = """{"name":"Albert","age":76,"city":"Princeton","gender":"M"}"""
    val dperson = json.decode[Person]
    dperson shouldBe \/-(person)
  }

  "a DecodeJson" should "return an error when a field is missing" in {
    val json = """{"name":"Albert","age":76,"gender":"M"}"""
    val dperson = json.decodeEither[Person]
    dperson should === (-\/("Attempt to decode value on failed cursor.: [*.--\\(city)]"))
  }

  "a DecodeJson" should "return an error when an enumeration has an invalid value" in {
    val json = """{"name":"Albert","age":76,"city":"Princeton","gender":"Z"}"""
    val dperson = json.decodeEither[Person]
    dperson should === (-\/("Invalid value Z: [--\\(gender)]"))
  }

  "an EncodeJson" should "encode a Person with a nested Address" in {
    val address = Struct.empty + Line1("52 Upper Street") + PostCode("N1 0QH")
    val personAdr = person + address
    // TODO macro should declare implicitly[EncodeJson[Struct[...]]
    implicitly[EncodeJson[Struct[Nil with Line1 with PostCode]]].encode(address)
    //val json = personAdr.toJsonString

    //json should === ("""{"name":"Albert","age":76,"city":"Princeton","gender":"M","address":{"line1":"52 Upper Street","postCode":"N1 0QH"}}""")
  }
}

object CodecJsonSpec {
  type Person = Struct[Name with Age with City with Gender]
  type Address = Struct[Line1 with PostCode]

  case class Name(v: String) extends AnyVal
  case class Age(v: Int) extends AnyVal
  case class City(v: String) extends AnyVal
  case class Line1(v: String) extends AnyVal
  case class PostCode(v: String) extends AnyVal

  sealed abstract class Gender(val v: String)
  object Gender {
    case object Male extends Gender("M")
    case object Female extends Gender("F")

    val all = Seq(Male, Female)
    def make(value: String): Option[Gender] = all.find(_.v == value)
  }




  // Defines how to encode/decode Name to/from a Struct
  implicit val nameCodec: CodecJson[Name] = StrucsCodecJson.fromWrapper[Name, String]("name")

  implicit val ageCodec: CodecJson[Age] = StrucsCodecJson.fromWrapper[Age, Int]("age")
  // We can also declare encode and decode separately
  implicit val cityEncode: EncodeJson[City] = StrucsEncodeJson.fromWrapper[City, String]("city")
  implicit val cityDecode: DecodeJson[City] = StrucsDecodeJson.fromWrapper[City, String]("city")

  implicit val genderWrapper: Wrapper[Gender, String] = Wrapper(Gender.make, _.v)
  implicit val genderCodec: CodecJson[Gender] = StrucsCodecJson.fromWrapper[Gender, String]("gender")

  implicit val line1Codec: CodecJson[Line1] = StrucsCodecJson.fromWrapper[Line1, String]("line1")
  implicit val postCodeCodec: CodecJson[PostCode] = StrucsCodecJson.fromWrapper[PostCode, String]("postCode")

}
