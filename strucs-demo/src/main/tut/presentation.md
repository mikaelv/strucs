class: center, middle
# strucs
![logo](logo.png)

Flexible data structures in scala

https://github.com/mikaelv/strucs
---
# Plan
1. Why ?
2. Adding fields
3. Getting fields
4. Composing Structs
5. Structural typing
6. Under the hood
7. Future 
---
# Why ?
Case classes are not composable

```tut:invisible
case class Address(street: String)
```
```tut:silent
case class CreatePersonJsonPayload(name: String, age: Int)
case class PersonModel(name: String, age: Int, address: Option[Address])
case class PersonDatabaseRow(id: String, name: String, age: Int, addressId: String)
```

* How can I define the common fields only once ?
* Alternative: shapeless records
* Strucs behaves like a HSet
---
# Adding fields

```tut:silent
case class Name(v: String) extends AnyVal
case class Age(v: Int) extends AnyVal
```
```tut:invisible
import strucs._
```
```tut
val person = Struct.empty + Name("Mikael") + Age(39)
person.update(Name("Albert"))
```
```tut:fail
person + Name("Robert")
```
???
Each field of the struct must have its own type. Referred to as Wrapper type.
Inside a Struct, each field is uniquely identified by its type
We will have a look at the internal structure later on
---
# Getting fields
```tut
person.get[Name]
```
```tut:fail
person.get[Street]
```
---
# Composing Structs
```tut:silent
type PersonData = Name with Age with Nil
type Person = Struct[PersonData]
val person: Person = Struct.empty + Name("Mikael") + Age(39)
```
```tut:invisible
case class Street(v: String) extends AnyVal
case class City(v: String) extends AnyVal
```
```tut:silent
type AddressData = Street with City with Nil
type Address = Struct[AddressData]
val address: Address = Struct(City("London")) + Street("52 Upper Street")
```
```tut
type PersonAddress = Struct[PersonData with AddressData]
val personAddress: PersonAddress = person ++ address 
```
---
# Structural typing
```tut
def adult[T <: Age with Name](struct: Struct[T]): String = {
  struct.get[Name].v + 
  (if (struct.get[Age].v >= 18) " is an adult" else " is a child")
}
adult(person)
```
---
# Encoding/Decoding

```tut:invisible
import strucs.json._
import strucs.fix._
import strucs.fix.dict.fix42._ // defines common FIX 4.2 tags with their codec
import CodecFix._
import StrucsCodecJson._
import StrucsEncodeJson._
import StrucsDecodeJson._
import argonaut._
import Argonaut._
implicit val symbolCodecJson: CodecJson[Symbol] = StrucsCodecJson.fromWrapper[Symbol, String]("symbol")
implicit val orderQtyCodecJson: CodecJson[OrderQty] = StrucsCodecJson.fromWrapper[OrderQty, BigDecimal]("quantity")
```
```tut:silent
type MyOrder = Struct[OrderQty with Symbol with Nil]
val json = """{"quantity":10,"symbol":"^FTSE"}"""
```
```tut
val order = json.decodeOption[MyOrder]
```
```tut
val fixOrder = order.get + BeginString.Fix42 + MsgType.OrderSingle
val fix = fixOrder.toFixMessageString
```

---
# Under the hood: Struct
```tut:silent
case class Struct[F](private val fields: Map[StructKey, Any]) {

  def +[T](value: T)(implicit k: StructKeyProvider[T], ev: F <:!< T ): 
  Struct[F with T] = 
    new Struct[F with T](fields + (k.key -> value))
    
  def get[T](implicit k: StructKeyProvider[T], ev: F <:< T): T = 
    fields(k.key).asInstanceOf[T]
    
  /** Get a subset of the fields */
  def shrink[F2](implicit ev: F <:< F2): Struct[F2] =
    this.asInstanceOf[Struct[F2]]

}

object Struct {
  def empty: Struct[Nil] = new Struct[Nil](Map.empty)
}
```
---
# Under the hood: CodecFix
```tut:invisible
import scala.util.{Failure, Success, Try}
import scala.language.experimental.macros
import strucs.fix.{CodecFix => _}
```
```scala
trait CodecFix[A] {
  def encode(a: A): FixElement
  def decode(fix: FixElement): Try[A]
}
```
Sample implementations:
```scala
case class OrderQty(v: BigDecimal) extends AnyVal
object OrderQty {
  implicit val codec: CodecFix[OrderQty] = 
    new TagCodecFix[OrderQty, BigDecimal](38)
}

case class Symbol(v: String) extends AnyVal
object Symbol {
  implicit val codec: CodecFix[Symbol] = 
    new TagCodecFix[Symbol, String](55)
}
```
---
# Under the hood: ComposeCodec
```tut:invisible
import strucs.fix.CodecFix
```
```tut:silent
/** Defines how a Codec[Struct[_]] can be built using the codecs of its fields */
trait ComposeCodec[Codec[_]] {

  /** Build a Codec for an empty Struct */
  def zero: Codec[Struct[Nil]]

  /** Build a Codec using a field codec a and a codec b for the rest */
  def prepend[A : StructKeyProvider, B](
                ca: Codec[A], 
                cb: Codec[Struct[B]]): Codec[Struct[A with B]]
}
```
```tut:silent
def composeCodec: ComposeCodec[CodecFix] = ???

def codec1: CodecFix[Struct[Symbol with Nil]] =  
    composeCodec.prepend[Symbol, Nil](Symbol.codec, composeCodec.zero)
    
def codec2: CodecFix[Struct[OrderQty with Symbol with Nil]] =  
    composeCodec.prepend[OrderQty, Symbol with Nil](OrderQty.codec, codec1)
```

---
```tut:silent
object CodecFix {
  /** Automatically create a CodecFix for any Struct[A]
    * @tparam T mixin, each type M in the mixin 
                must have an implicit CodecFix[M] in scope */
  implicit def makeCodecFix[T]: CodecFix[Struct[T]] = 
    macro ComposeCodec.macroImpl[CodecFix[_], T]


  implicit object ComposeCodecFix extends ComposeCodec[CodecFix] {
    /** Build a Codec for an empty Struct */
    def zero: CodecFix[Struct[Nil]] = new CodecFix[Struct[Nil]] {
      override def encode(a: Struct[Nil]): FixElement = FixGroup.empty
      override def decode(fix: FixElement): Try[Struct[Nil]] = Success(Struct.empty)
    }
    
    /** Build a Codec using a field codec a and a codec b for the rest */
    def prepend[A: StructKeyProvider, B](
                ca: CodecFix[A], 
                cb: CodecFix[Struct[B]]) = new CodecFix[Struct[A with B]] {
      def encode(a: Struct[A with B]): FixElement = {
        val bfix = cb.encode(a.shrink[B])
        val afix = ca.encode(a.get[A])
        afix + bfix
      }

      def decode(fix: FixElement): Try[Struct[A with B]] = {
        for {
          structb <- cb.decode(fix)
          a <- ca.decode(fix)
        } yield structb.+[A](a)
      }
    }
  }
}

```
---
# Under the hood: argonaut.DecodeJson

The implementation of ComposeCodec is very similar
```tut:silent
object StrucsDecodeJson {
  implicit def makeDecodeJson[T]: DecodeJson[Struct[T]] = 
    macro ComposeCodec.macroImpl[DecodeJson[_], T]

  implicit object ComposeDecodeJson extends ComposeCodec[DecodeJson] {
    /** Build a Codec for an empty Struct */
    def zero = new DecodeJson[Struct[Nil]] {
      def decode(c: HCursor): DecodeResult[Struct[Nil]] = 
        DecodeResult.ok(Struct.empty)
    }

    /** Build a Codec using a field codec a and a codec b for the rest of the Struct */
    def prepend[A: StructKeyProvider, B](
                ca: DecodeJson[A], 
                cb: DecodeJson[Struct[B]]) = new DecodeJson[Struct[A with B]] {
      def decode(c: HCursor): DecodeResult[Struct[A with B]] = {
        for {
          structb <- cb.decode(c)
          a <- ca.decode(c)
        } yield structb.+[A](a)
      }
    }
  }  
}
```
---
# Future developments
* Benchmarks & Optimizations
* Struct <==> case class
* Struct <==> Avro
* Struct <==> Protobuf
* Typed Spark DataFrame ?

### Contributions are welcome !
---
class: center, middle
# Questions ?


Mikael Valot

twitter: @leakimav


https://github.com/mikaelv/strucs


---