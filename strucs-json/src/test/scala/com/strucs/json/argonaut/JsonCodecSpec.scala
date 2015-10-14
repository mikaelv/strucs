package com.strucs.json.argonaut

import com.strucs.json.argonaut.JsonCodecSpec._
import com.strucs.json.argonaut.JsonEncode._
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FlatSpec, Matchers}
import org.strucs.Struct

/**
 */
class JsonCodecSpec  extends FlatSpec with Matchers with TypeCheckedTripleEquals {
  "a JsonEncode" should "encode a Person" in {
    val struct = Struct.empty + Name("Albert")
    val json = struct.toJsonString
    json should === ("""{"name":"Albert"}""")
  }
}

object JsonCodecSpec {
  case class Name(v: String) extends AnyVal
  case class Age(v: Int) extends AnyVal
  case class City(v: String) extends AnyVal

  implicit val nameEncode: JsonEncode[Name] = JsonEncode.single[Name, String]("name")

}
