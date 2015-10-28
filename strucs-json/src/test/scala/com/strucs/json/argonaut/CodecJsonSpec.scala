package com.strucs.json.argonaut

import argonaut.{DecodeJson, EncodeJson}
import com.strucs.json.argonaut.CodecJsonSpec._
import com.strucs.json.argonaut.StrucsEncodeJson._
import com.strucs.json.argonaut.StrucsDecodeJson._
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FlatSpec, Matchers}
import org.strucs.Struct

/**
 */
class CodecJsonSpec  extends FlatSpec with Matchers with TypeCheckedTripleEquals {
  val person = Struct.empty + Name("Albert") + Age(76) + City("Princeton")
  
  "an EncodeJson" should "encode a Person" in {
    val json = person.toJsonString
    json should === ("""{"name":"Albert","age":76,"city":"Princeton"}""")
  }

  "a DecodeJson" should "decode a json string into a Person" in {
    val json = """{"name":"Albert","age":76,"city":"Princeton"}"""
    import argonaut._, Argonaut._
    implicit val decodePerson = implicitly[DecodeJson[Person]]
    json.decode[Person] shouldBe DecodeResult.ok(person)
  }

}

object CodecJsonSpec {
  type Person = Struct[Name with Age with City]

  case class Name(v: String) extends AnyVal
  case class Age(v: Int) extends AnyVal
  case class City(v: String) extends AnyVal


  implicit val nameEncode: EncodeJson[Name] = StrucsEncodeJson.fromWrapper[Name, String]("name")
  implicit val ageEncode: EncodeJson[Age] = StrucsEncodeJson.fromWrapper[Age, Int]("age")
  implicit val cityEncode: EncodeJson[City] = StrucsEncodeJson.fromWrapper[City, String]("city")

  implicit val nameDecode: DecodeJson[Name] = StrucsDecodeJson.fromWrapper[Name, String]("name")
  implicit val ageDecode: DecodeJson[Age] = StrucsDecodeJson.fromWrapper[Age, Int]("age")
  implicit val cityDecode: DecodeJson[City] = StrucsDecodeJson.fromWrapper[City, String]("city")


}
