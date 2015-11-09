# Strucs - Flexible data structures in Scala

Strucs is a lightweight library that allows to manipulate, encode and decode flexible data structures while maintaining immutability and type safety.

A Struct is analogous to a case class that can accept new fields dynamically.

Using the strucs extensions, a single struc instance can be easily serialized/deserialized to various formats, such as JSON, [FIX protocol](https://en.wikipedia.org/wiki/Financial_Information_eXchange), Protobuf, ...
  
## Quick start

### Create/Add/Update

```scala
import strucs._

case class Ticker(v: String) extends AnyVal
case class Quantity(v: BigDecimal) extends AnyVal
case class Price(v: BigDecimal) extends AnyVal
```
```scala
scala> val order = Struct(Ticker("^FTSE"))
order: strucs.Struct[Ticker with strucs.Nil] = Struct(Map(StructKey(class Ticker) -> Ticker(^FTSE)))

scala> val order2 = order.add(Quantity(5))
order2: strucs.Struct[Ticker with strucs.Nil with Quantity] = Struct(Map(StructKey(class Ticker) -> Ticker(^FTSE), StructKey(class Quantity) -> Quantity(5)))

scala> order2.get[Ticker]
res1: Ticker = Ticker(^FTSE)

scala> val order3 = order2.update(Ticker("^FCHI"))
order3: strucs.Struct[Ticker with strucs.Nil with Quantity] = Struct(Map(StructKey(class Ticker) -> Ticker(^FCHI), StructKey(class Quantity) -> Quantity(5)))

scala> order3.get[Ticker]
res2: Ticker = Ticker(^FCHI)
```
order3 does not have a Price field. Any attempt to access it is rejected by the compiler.
```scala
scala> order3.get[Price]
<console>:21: error: Cannot prove that Ticker with strucs.Nil with Quantity <:< Price.
              order3.get[Price]
                        ^
```

### Structural typing
*When I see a bird that walks like a duck and swims like a duck and quacks like a duck, I call that bird a duck.*

Let's define a function that accepts any Struct that has two specific fields.
```scala
scala> def totalPrice[T <: Quantity with Price](struct: Struct[T]): BigDecimal = {
     |   struct.get[Quantity].v * struct.get[Price].v
     | }
totalPrice: [T <: Quantity with Price](struct: strucs.Struct[T])BigDecimal
```
A call with an incompatible Struct is rejected by the compiler:  
```scala
scala> totalPrice(order3)
<console>:22: error: inferred type arguments [Ticker with strucs.Nil with Quantity] do not conform to method totalPrice's type parameter bounds [T <: Quantity with Price]
              totalPrice(order3)
              ^
<console>:22: error: type mismatch;
 found   : strucs.Struct[Ticker with strucs.Nil with Quantity]
 required: strucs.Struct[T]
              totalPrice(order3)
                         ^
```
But succeeds when we add the required field:
```scala
scala> totalPrice(order3.add(Price(10)))
res5: BigDecimal = 50
```


### Encoding/Decoding
Provided that the encoders/decoders for the fields are in scope, the same struct instance can be encoded/decoded to various formats:
```scala
import strucs.json._
import strucs.fix._
import strucs.fix.dict.fix42._ // defines common FIX 4.2 tags with their codec
import CodecFix._
import StrucsCodecJson._
import StrucsEncodeJson._
import StrucsDecodeJson._
import argonaut._
import Argonaut._

type MyOrder = Struct[OrderQty with Symbol with Nil]
val order: MyOrder = Struct.empty + OrderQty(10) + Symbol("^FTSE")
```
The order can be encoded/decoded to/from FIX if we add the required tags BeginString and MsgType. 
```scala
scala> val fixOrder = order + BeginString.Fix42 + MsgType.OrderSingle
fixOrder: strucs.Struct[strucs.fix.dict.fix42.OrderQty with strucs.fix.dict.fix42.Symbol with strucs.Nil with strucs.fix.dict.fix42.BeginString with strucs.fix.dict.fix42.MsgType] = Struct(Map(StructKey(class OrderQty) -> OrderQty(10), StructKey(class Symbol) -> Symbol(^FTSE), StructKey(class BeginString) -> BeginString(FIX.4.2), StructKey(class MsgType) -> MsgType(D)))

scala> val fix = fixOrder.toFixMessageString
fix: String = 8=FIX.4.2?9=20?35=D?38=10?55=^FTSE?10=036?

scala> fix.toStruct[fixOrder.Mixin]
res7: scala.util.Try[strucs.Struct[fixOrder.Mixin]] = Success(Struct(Map(StructKey(class MsgType) -> MsgType(D), StructKey(class BeginString) -> BeginString(FIX.4.2), StructKey(class Symbol) -> Symbol(^FTSE), StructKey(class OrderQty) -> OrderQty(10))))
```
If we define the [Argonaut](http://argonaut.io/) Json codecs for Symbol and OrderQty,
```scala
implicit val symbolCodecJson: CodecJson[Symbol] = StrucsCodecJson.fromWrapper[Symbol, String]("symbol")
implicit val orderQtyCodecJson: CodecJson[OrderQty] = StrucsCodecJson.fromWrapper[OrderQty, BigDecimal]("quantity")
```
We can encode/decode our order to/from Json
```scala
scala> val json = order.toJsonString
json: String = {"quantity":10,"symbol":"^FTSE"}

scala> json.decodeOption[MyOrder]
res8: Option[MyOrder] = Some(Struct(Map(StructKey(class Symbol) -> Symbol(^FTSE), StructKey(class OrderQty) -> OrderQty(10))))
```

### More examples
Please check out the unit tests for more usage examples.

## Motivation
Consider a program which manages Orders.
A common approach would be to use case classes with simple types for its fields: 
```scala
scala> case class SimpleOrder(symbol: String, quantity: BigDecimal, price: BigDecimal)
defined class SimpleOrder
```
However, using simple types such as String, Int, BigDecimal, ... everywhere can rapidly make the code confusing and fragile.
Imagine we have to extract the price and quantity of all the FTSE orders 
```scala
scala> def simpleFootsieOrders(orders: List[SimpleOrder]): List[(BigDecimal, BigDecimal)] = 
     |   orders collect {
     |     case SimpleOrder(sym, q, p) if sym == "^FTSE" =>  (q, p)
     |   }
simpleFootsieOrders: (orders: List[SimpleOrder])List[(BigDecimal, BigDecimal)]
```
If I do not get the argument order right (or if it has been refactored), the code above will compile but will not do what I expect.
Furthermore, the return type is List[(BigDecimal, BigDecimal)], which is unclear for the users of the function. 


We need stronger types to make our code clearer and safer. You you might want to use [value classes](http://docs.scala-lang.org/overviews/core/value-classes.html) as follows:
```scala
case class Symbol(v: String) extends AnyVal
val FTSE = Symbol("FTSE")
case class Quantity(v: BigDecimal) extends AnyVal
case class Price(v: BigDecimal) extends AnyVal

case class TypedOrder(symbol: Symbol, quantity: Quantity, price: Price)
```
```scala
scala> def typedFootsieOrders(orders: List[TypedOrder]): List[(Quantity, Price)] = 
     |   orders.collect {
     |     case TypedOrder(sym, q, p) if sym == FTSE => (q, p)
     |   }
typedFootsieOrders: (orders: List[TypedOrder])List[(Quantity, Price)]
```
Now the return type is much clearer and safer, and my matching expression is safer as well: 
I cannot inadvertently swap arguments without getting a compilation error.

On the other hand, we now observe that the names of the attributes are redundant with their types. 
It would be nicer if we could declare them only once.
Also, I cannot easily reuse a set of fields, such as symbol and quantity, in another case class. I need to redefine the class with all its fields:
```scala
scala> case class StopPrice(v: BigDecimal)
defined class StopPrice

scala> case class StopOrder(symbol: Symbol, quantity: Quantity, price: StopPrice)
defined class StopOrder
```
If I then want to define a function that accepts StopOrder or TypedOrder, I would typically define a common trait that these classes will extend.
```scala
scala> trait Order {
     |   def symbol: Symbol 
     | }
defined trait Order

scala> def filterFootsie(orders: List[Order]): List[Order] = orders.filter(_.symbol == FTSE)
filterFootsie: (orders: List[Order])List[Order]
```
This leads to some duplication, and it may not even be feasible if TypedOrder is defined in a third party library.
 
With strucs, we can define the same as follows:
```scala
type BaseOrderType = Symbol with Quantity with Nil
type StructOrder = Struct[BaseOrderType with Price]
type StructStopOrder = Struct[BaseOrderType with StopPrice]
def filterFootsie[T <: Symbol](orders: List[Struct[T]]) = 
  orders.filter(_.get[Symbol] == FTSE)
```
The different "order" types are now **composable**. I can define an abstraction BaseOrder, and reuse it to define other Order types. 
Also, I do not have to declare field names anymore, as I use only the types of the fields to access them.  
This composition capability also applies to instances:

```scala
scala> val baseOrder = Struct.empty + FTSE + Quantity(100)
baseOrder: strucs.Struct[strucs.Nil with Symbol with Quantity] = Struct(Map(StructKey(class Symbol) -> Symbol(FTSE), StructKey(class Quantity) -> Quantity(100)))

scala> val order: StructOrder = baseOrder + Price(30)
order: StructOrder = Struct(Map(StructKey(class Symbol) -> Symbol(FTSE), StructKey(class Quantity) -> Quantity(100), StructKey(class Price) -> Price(30)))

scala> val stopOrder: StructStopOrder = baseOrder + StopPrice(20)
stopOrder: StructStopOrder = Struct(Map(StructKey(class Symbol) -> Symbol(FTSE), StructKey(class Quantity) -> Quantity(100), StructKey(class StopPrice) -> StopPrice(20)))

scala> filterFootsie(List(order, order.update(Symbol("CAC40"))))
res10: List[strucs.Struct[Symbol with Quantity with strucs.Nil with Price]] = List(Struct(Map(StructKey(class Symbol) -> Symbol(FTSE), StructKey(class Quantity) -> Quantity(100), StructKey(class Price) -> Price(30))))
```  

