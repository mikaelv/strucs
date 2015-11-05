# Strucs - Flexible data structures in Scala

Strucs is a lightweight library that allows to manipulate, encode and decode flexible data structures while maintaining immutability and type safety.

A Struct is analogous to a case class that can accept new fields dynamically.

Using the strucs extensions, a single struc instance can be easily serialized/deserialized to various formats, such as JSON, FIX protocol, Protobuf, ...
  
## Quick start

### Create/Add/Update
The following code snippet can be pasted in a REPL session.
Check out the unit tests for more.

```scala
import strucs._

case class Symbol(v: String) extends AnyVal
case class Quantity(v: BigDecimal) extends AnyVal
case class Price(v: BigDecimal) extends AnyVal
```
```scala
scala> val order = Struct(Symbol("^FTSE"))
order: strucs.Struct[Symbol with strucs.Nil] = Struct(Map(StructKey(class Symbol) -> Symbol(^FTSE)))

scala> val order2 = order.add(Quantity(5))
order2: strucs.Struct[Symbol with strucs.Nil with Quantity] = Struct(Map(StructKey(class Symbol) -> Symbol(^FTSE), StructKey(class Quantity) -> Quantity(5)))

scala> order2.get[Symbol]
res1: Symbol = Symbol(^FTSE)

scala> val order3 = order2.update(Symbol("^FCHI"))
order3: strucs.Struct[Symbol with strucs.Nil with Quantity] = Struct(Map(StructKey(class Symbol) -> Symbol(^FCHI), StructKey(class Quantity) -> Quantity(5)))

scala> order3.get[Symbol]
res2: Symbol = Symbol(^FCHI)
```
```scala
scala> order3.get[Price]
<console>:21: error: Cannot prove that Symbol with strucs.Nil with Quantity <:< Price.
              order3.get[Price]
                        ^
```

### Structural typing
*When I see a bird that walks like a duck and swims like a duck and quacks like a duck, I call that bird a duck.*

Let's define a function that accept any Struct that has one or more specific fields.
```scala
scala> def totalPrice[T <: Quantity with Price](struct: Struct[T]): BigDecimal = {
     |   struct.get[Quantity].v * struct.get[Price].v
     | }
totalPrice: [T <: Quantity with Price](struct: strucs.Struct[T])BigDecimal
```
```scala
scala> totalPrice(order3)
<console>:22: error: inferred type arguments [Symbol with strucs.Nil with Quantity] do not conform to method totalPrice's type parameter bounds [T <: Quantity with Price]
              totalPrice(order3)
              ^
<console>:22: error: type mismatch;
 found   : strucs.Struct[Symbol with strucs.Nil with Quantity]
 required: strucs.Struct[T]
              totalPrice(order3)
                         ^
```
```scala
scala> totalPrice(order3.add(Price(10)))
res5: BigDecimal = 50
```



### Encoding/Decoding
```scala
```

## Motivation
Let's say you want to manipulate Orders in your system.
The common approach would be: 
```scala
scala> case class SimpleOrder(symbol: String, quantity: BigDecimal, price: BigDecimal)
defined class SimpleOrder
```
Using simple types such as String, Int, BigDecimal, ... everywhere can rapidly make the code confusing and fragile.
Imagine we have to extract the price and quantity of all the FTSE orders 
```scala
scala> def simpleFootsieOrders(orders: List[SimpleOrder]): List[(BigDecimal, BigDecimal)] = orders collect {
     |   case SimpleOrder(sym, q, p) if sym == "^FTSE" =>  (q, p)
     | }
simpleFootsieOrders: (orders: List[SimpleOrder])List[(BigDecimal, BigDecimal)]
```
If I do not get the argument order right (or if it has been refactored), the code above will compile but will not do what I expect.
Furthermore, the return type is List[(BigDecimal, BigDecimal)], which is unclear for the users of the function. 


At this point you might decide to use [value classes](http://docs.scala-lang.org/overviews/core/value-classes.html) to enforce a better type safety
```scala
case class Symbol(v: String) extends AnyVal
val FTSE = Symbol("FTSE")
case class Quantity(v: BigDecimal) extends AnyVal
case class Price(v: BigDecimal) extends AnyVal

case class TypedOrder(symbol: Symbol, quantity: Quantity, price: Price)
```
```scala
scala> def typedFootsieOrders(orders: List[TypedOrder]): List[(Quantity, Price)] = orders.collect {
     |   case TypedOrder(sym, q, p) if sym == FTSE => (q, p)
     | }
typedFootsieOrders: (orders: List[TypedOrder])List[(Quantity, Price)]
```
Now the return type is much clearer and safer, and my matching expression is safer as well: 
I cannot inadvertently swap arguments without getting a compilation error.

However, we now observe that the names of the attributes are redundant with their types. 
It would be nicer if we could declare them only once.
Also, I cannot easily reuse a set of fields in another case class:
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
scala> type BaseOrderType = Symbol with Quantity with Nil
defined type alias BaseOrderType

scala> type StructOrder = Struct[BaseOrderType with Price]
defined type alias StructOrder

scala> type StructStopOrder = Struct[BaseOrderType with StopPrice]
defined type alias StructStopOrder

scala> def filterFootsie[T <: Symbol](orders: List[Struct[T]]) = orders.filter(_.get[Symbol] == FTSE)
filterFootsie: [T <: Symbol](orders: List[strucs.Struct[T]])List[strucs.Struct[T]]
```
The "order" types are now **composable**. I can define an abstraction BaseOrder, and reuse it to define other Order types. 
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
res7: List[strucs.Struct[Symbol with Quantity with strucs.Nil with Price]] = List(Struct(Map(StructKey(class Symbol) -> Symbol(FTSE), StructKey(class Quantity) -> Quantity(100), StructKey(class Price) -> Price(30))))
```  

