package com.strucs.json.argonaut

import argonaut.{DecodeJson, EncodeJson}
import com.strucs.json.argonaut.CodecJsonSpec.Gender.Male
import com.strucs.json.argonaut.CodecJsonSpec._
import com.strucs.json.argonaut.StrucsEncodeJson._
import com.strucs.json.argonaut.StrucsDecodeJson._
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FlatSpec, Matchers}
import org.strucs.{Wrapper, Struct}

import scalaz.\/-

/**
 */
class CodecJsonSpec  extends FlatSpec with Matchers with TypeCheckedTripleEquals {
  val person = Struct.empty + Name("Albert") + Age(76) + City("Princeton") + Male
  
  "an EncodeJson" should "encode a Person" in {
    val json = person.toJsonString
    json should === ("""{"name":"Albert","age":76,"city":"Princeton","gender":"M"}""")
  }

  "a DecodeJson" should "decode a json string into a Person" in {
    val json = """{"name":"Albert","age":76,"city":"Princeton","gender":"M"}"""
    import argonaut._, Argonaut._
    val dperson = json.decode[Person]
    dperson shouldBe \/-(person)
  }

}

object CodecJsonSpec {
  type Person = Struct[Name with Age with City with Gender]

  case class Name(v: String) extends AnyVal
  case class Age(v: Int) extends AnyVal
  case class City(v: String) extends AnyVal

  sealed abstract class Gender(val v: String)
  object Gender {
    // TODO is this better than case objects ? What about equals, hashcode, pattern matching ?
    val Male = new Gender("M") {}
    val Female = new Gender("F") {}

    val all = Seq(Male, Female)
    def make(value: String): Option[Gender] = all.find(_.v == value)
  }


  implicit val nameEncode: EncodeJson[Name] = StrucsEncodeJson.fromWrapper[Name, String]("name")
  implicit val nameDecode: DecodeJson[Name] = StrucsDecodeJson.fromWrapper[Name, String]("name")

  implicit val ageEncode: EncodeJson[Age] = StrucsEncodeJson.fromWrapper[Age, Int]("age")
  implicit val ageDecode: DecodeJson[Age] = StrucsDecodeJson.fromWrapper[Age, Int]("age")

  implicit val cityEncode: EncodeJson[City] = StrucsEncodeJson.fromWrapper[City, String]("city")
  implicit val cityDecode: DecodeJson[City] = StrucsDecodeJson.fromWrapper[City, String]("city")

  implicit val genderWrapper: Wrapper[Gender, String] = Wrapper(Gender.make, _.v)
  implicit val genderEncode: EncodeJson[Gender] = StrucsEncodeJson.fromWrapper[Gender, String]("gender")
  implicit val genderDecode: DecodeJson[Gender] = StrucsDecodeJson.fromWrapper[Gender, String]("gender")


}
