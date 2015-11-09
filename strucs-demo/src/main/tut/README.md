# Strucs - Flexible data structures in Scala

Strucs is a lightweight library that allows to manipulate, encode and decode flexible data structures while maintaining immutability and type safety.

A Struct is analogous to a case class that can accept new fields dynamically.

Using the strucs extensions, a single struc instance can be easily serialized/deserialized to various formats, such as JSON, [FIX protocol](https://en.wikipedia.org/wiki/Financial_Information_eXchange), Protobuf, ...
  
## Quick start

### Create/Add/Update

```tut:silent
import strucs._

case class Ticker(v: String) extends AnyVal
case class Quantity(v: BigDecimal) extends AnyVal
case class Price(v: BigDecimal) extends AnyVal
```
```tut
val order = Struct(Ticker("^FTSE"))
val order2 = order.add(Quantity(5))
order2.get[Ticker]
val order3 = order2.update(Ticker("^FCHI"))
order3.get[Ticker]
```
order3 does not have a Price field. Any attempt to access it is rejected by the compiler.
```tut:fail
order3.get[Price]
```

### Structural typing
*When I see a bird that walks like a duck and swims like a duck and quacks like a duck, I call that bird a duck.*

Let's define a function that accepts any Struct that has two specific fields.
```tut
def totalPrice[T <: Quantity with Price](struct: Struct[T]): BigDecimal = {
  struct.get[Quantity].v * struct.get[Price].v
}
```
A call with an incompatible Struct is rejected by the compiler:  
```tut:fail
totalPrice(order3)
```
But succeeds when we add the required field:
```tut
totalPrice(order3.add(Price(10)))
```


### Encoding/Decoding
Provided that the encoders/decoders for the fields are in scope, the same struct instance can be encoded/decoded to various formats:
```tut:silent
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
```tut
val fixOrder = order + BeginString.Fix42 + MsgType.OrderSingle
val fix = fixOrder.toFixMessageString
fix.toStruct[fixOrder.Mixin]
```
If we define the [Argonaut](http://argonaut.io/) Json codecs for Symbol and OrderQty,
```tut:silent
implicit val symbolCodecJson: CodecJson[Symbol] = StrucsCodecJson.fromWrapper[Symbol, String]("symbol")
implicit val orderQtyCodecJson: CodecJson[OrderQty] = StrucsCodecJson.fromWrapper[OrderQty, BigDecimal]("quantity")
```
We can encode/decode our order to/from Json
```tut
val json = order.toJsonString
json.decodeOption[MyOrder]
```

### More examples
Please check out the unit tests for more usage examples.

## Motivation
Consider a program which manages Orders.
A common approach would be to use case classes with simple types for its fields: 
```tut
case class SimpleOrder(symbol: String, quantity: BigDecimal, price: BigDecimal)
```
However, using simple types such as String, Int, BigDecimal, ... everywhere can rapidly make the code confusing and fragile.
Imagine we have to extract the price and quantity of all the FTSE orders 
```tut
def simpleFootsieOrders(orders: List[SimpleOrder]): List[(BigDecimal, BigDecimal)] = 
  orders collect {
    case SimpleOrder(sym, q, p) if sym == "^FTSE" =>  (q, p)
  }
```
If I do not get the argument order right (or if it has been refactored), the code above will compile but will not do what I expect.
Furthermore, the return type is List[(BigDecimal, BigDecimal)], which is unclear for the users of the function. 


We need stronger types to make our code clearer and safer. You you might want to use [value classes](http://docs.scala-lang.org/overviews/core/value-classes.html) as follows:
```tut:silent
case class Symbol(v: String) extends AnyVal
val FTSE = Symbol("FTSE")
case class Quantity(v: BigDecimal) extends AnyVal
case class Price(v: BigDecimal) extends AnyVal

case class TypedOrder(symbol: Symbol, quantity: Quantity, price: Price)
```
```tut
def typedFootsieOrders(orders: List[TypedOrder]): List[(Quantity, Price)] = 
  orders.collect {
    case TypedOrder(sym, q, p) if sym == FTSE => (q, p)
  }
```
Now the return type is much clearer and safer, and my matching expression is safer as well: 
I cannot inadvertently swap arguments without getting a compilation error.

On the other hand, we now observe that the names of the attributes are redundant with their types. 
It would be nicer if we could declare them only once.
Also, I cannot easily reuse a set of fields, such as symbol and quantity, in another case class. I need to redefine the class with all its fields:
```tut
case class StopPrice(v: BigDecimal)
case class StopOrder(symbol: Symbol, quantity: Quantity, price: StopPrice)
```
If I then want to define a function that accepts StopOrder or TypedOrder, I would typically define a common trait that these classes will extend.
```tut
trait Order {
  def symbol: Symbol 
}
def filterFootsie(orders: List[Order]): List[Order] = orders.filter(_.symbol == FTSE)
```
This leads to some duplication, and it may not even be feasible if TypedOrder is defined in a third party library.
 
With strucs, we can define the same as follows:
```tut:silent
type BaseOrderType = Symbol with Quantity with Nil
type StructOrder = Struct[BaseOrderType with Price]
type StructStopOrder = Struct[BaseOrderType with StopPrice]
def filterFootsie[T <: Symbol](orders: List[Struct[T]]) = 
  orders.filter(_.get[Symbol] == FTSE)
```
The different "order" types are now **composable**. I can define an abstraction BaseOrder, and reuse it to define other Order types. 
Also, I do not have to declare field names anymore, as I use only the types of the fields to access them.  
This composition capability also applies to instances:

```tut
val baseOrder = Struct.empty + FTSE + Quantity(100)
val order: StructOrder = baseOrder + Price(30)
val stopOrder: StructStopOrder = baseOrder + StopPrice(20)
filterFootsie(List(order, order.update(Symbol("CAC40"))))

```  

