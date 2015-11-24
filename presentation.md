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



```scala
case class CreatePersonJsonPayload(name: String, age: Int)
case class PersonModel(name: String, age: Int, address: Option[Address])
case class PersonDatabaseRow(id: String, name: String, age: Int, addressId: String)
```

* How can I define the common fields only once ?
* Alternative: shapeless records
* Strucs behaves like a HSet
---
# Adding fields

```scala
case class Name(v: String) extends AnyVal
case class Age(v: Int) extends AnyVal
```


```scala
scala> val person = Struct.empty + Name("Mikael") + Age(39)
person: strucs.Struct[strucs.Nil with Name with Age] = Struct(Map(StructKey(class Name) -> Name(Mikael), StructKey(class Age) -> Age(39)))

scala> person.update(Name("Albert"))
res0: strucs.Struct[strucs.Nil with Name with Age] = Struct(Map(StructKey(class Name) -> Name(Albert), StructKey(class Age) -> Age(39)))
```
```scala
scala> person + Name("Robert")
<console>:17: error: Cannot prove that strucs.Nil with Name with Age <:!< Name.
              person + Name("Robert")
                     ^
```
???
Each field of the struct must have its own type. Referred to as Wrapper type.
Inside a Struct, each field is uniquely identified by its type
We will have a look at the internal structure later on
---
# Getting fields
```scala
scala> person.get[Name]
res2: Name = Name(Mikael)
```
```scala
scala> person.get[Street]
<console>:17: error: not found: type Street
              person.get[Street]
                         ^
```
---
# Composing Structs
```scala
type PersonData = Name with Age with Nil
type Person = Struct[PersonData]
val person: Person = Struct.empty + Name("Mikael") + Age(39)
```


```scala
type AddressData = Street with City with Nil
type Address = Struct[AddressData]
val address: Address = Struct(City("London")) + Street("52 Upper Street")
```
```scala
scala> type PersonAddress = Struct[PersonData with AddressData]
defined type alias PersonAddress

scala> val personAddress: PersonAddress = person ++ address 
personAddress: PersonAddress = Struct(Map(StructKey(class Name) -> Name(Mikael), StructKey(class Age) -> Age(39), StructKey(class City) -> City(London), StructKey(class Street) -> Street(52 Upper Street)))
```
---
# Structural typing
```scala
scala> def adult[T <: Age with Name](struct: Struct[T]): String = {
     |   struct.get[Name].v + 
     |   (if (struct.get[Age].v >= 18) " is an adult" else " is a child")
     | }
adult: [T <: Age with Name](struct: strucs.Struct[T])String

scala> adult(person)
res4: String = Mikael is an adult
```
---
# Encoding/Decoding



```scala
type MyOrder = Struct[OrderQty with Symbol with Nil]
val json = """{"quantity":10,"symbol":"^FTSE"}"""
```
```scala
scala> val order = json.decodeOption[MyOrder]
order: Option[MyOrder] = Some(Struct(Map(StructKey(class Symbol) -> Symbol(^FTSE), StructKey(class OrderQty) -> OrderQty(10))))
```
```scala
scala> val fixOrder = order.get + BeginString.Fix42 + MsgType.OrderSingle
fixOrder: strucs.Struct[strucs.fix.dict.fix42.OrderQty with strucs.fix.dict.fix42.Symbol with strucs.Nil with strucs.fix.dict.fix42.BeginString with strucs.fix.dict.fix42.MsgType] = Struct(Map(StructKey(class Symbol) -> Symbol(^FTSE), StructKey(class OrderQty) -> OrderQty(10), StructKey(class BeginString) -> BeginString(FIX.4.2), StructKey(class MsgType) -> MsgType(D)))

scala> val fix = fixOrder.toFixMessageString
fix: String = 8=FIX.4.2?9=20?35=D?38=10?55=^FTSE?10=036?
```

---
# Under the hood: Struct
```scala
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


```scala
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
```scala
def composeCodec: ComposeCodec[CodecFix] = ???

def codec1: CodecFix[Struct[Symbol with Nil]] =  
    composeCodec.prepend[Symbol, Nil](Symbol.codec, composeCodec.zero)
    
def codec2: CodecFix[Struct[OrderQty with Symbol with Nil]] =  
    composeCodec.prepend[OrderQty, Symbol with Nil](OrderQty.codec, codec1)
```

---
```scala
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
```scala
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
