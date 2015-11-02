package org.strucs

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Matchers, FlatSpec}
import org.strucs.FieldExtractorSpec.Field1

class FieldExtractorSpec extends FlatSpec with Matchers with TypeCheckedTripleEquals {
  implicit val universe = scala.reflect.runtime.universe
  import universe._


  "a FieldExtractor" should "extract the scalar fields from a mixin" in {
    val tpe = typeOf[Field1 with Int]
    val symbols = FieldExtractor.extractTypes(tpe)
    symbols shouldBe List(typeOf[Int], typeOf[Field1])
  }

  it should "extract the scalar fields from a type reference" in {
    val struct = Struct.empty + "Hello" + 1
    val tpe = typeOf[struct.Mixin]
    val symbols = FieldExtractor.extractTypes(tpe)
    symbols shouldBe List(typeOf[Int], typeOf[java.lang.String])

  }

  it should "extract the Struct fields from a mixin" in {
    val types = FieldExtractor.extractTypes(typeOf[String with Struct[String with Int]])
    val expected = List(typeOf[org.strucs.Struct[String with Int]], typeOf[String])
    types.map(_.toString) shouldBe expected.map(_.toString)
  }


}

object FieldExtractorSpec {
  case class Field1(v: String) extends AnyVal
}
