package strucs

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FlatSpec, Matchers}

import scala.reflect.runtime.universe._

/**
 *
 */
class StructSpec extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  case class Name(v: String)
  case class Age(v: Int)
  case class City(v: String)

  val name = Name("Omer")
  val city = City("Springfield")
  val name1 = Name("Marge")
  val age = Age(45)

  def baseStruct = Struct(name).add(age)

  "A Struct" should "add new fields" in {
    val s = baseStruct
    implicitly[s.type <:< Struct[Name with Age with Nil]]
  }

  it should "get an added field" in {
    val s = baseStruct
    s.get[Name] should ===(name)
    s.get[Age] should ===(age)
  }

  it should "forbid to add an existing field" in {
    val s = baseStruct
    assertTypeError("s.add(Age(10))")
  }

  it should "forbid to get or update a non-added field" in {
    val s = baseStruct
    assertTypeError("s.get[City]")
    assertTypeError("s.update(City(\"Paris\"))")
  }

  it should "update a field" in {
    val s = baseStruct.update(Age(10))
    s.get[Age].v should ===(10)
  }

  it should "allow to write functions that accept a Struct with more Fields than the type parameter" in {
    def fn[T <: Name with Age](struct: Struct[T]): Name = {
      struct.get[Name]
    }

    fn(baseStruct.add(city)) should be(name)
  }
}
