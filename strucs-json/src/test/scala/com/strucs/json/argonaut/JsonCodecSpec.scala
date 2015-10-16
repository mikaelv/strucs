package com.strucs.json.argonaut

import argonaut.EncodeJson
import com.strucs.json.argonaut.JsonCodecSpec._
import com.strucs.json.argonaut.EncodeJson._
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FlatSpec, Matchers}
import org.strucs.Struct

/**
 */
class JsonCodecSpec  extends FlatSpec with Matchers with TypeCheckedTripleEquals {
  "a JsonEncode" should "encode a Person" in {
    val struct = Struct.empty + Name("Albert") + Age(76) + City("Princeton")
    val json = struct.toJsonString
    json should === ("""{"name":"Albert","age":76,"city":"Princeton"}""")
  }
}

object JsonCodecSpec {
  case class Name(v: String) extends AnyVal
  case class Age(v: Int) extends AnyVal
  case class City(v: String) extends AnyVal

  implicit val nameEncode: EncodeJson[Name] = EncodeJson.fromWrapper[Name, String]("name")
  implicit val ageEncode: EncodeJson[Age] = EncodeJson.fromWrapper[Age, Int]("age")
  implicit val cityEncode: EncodeJson[City] = EncodeJson.fromWrapper[City, String]("city")

}
