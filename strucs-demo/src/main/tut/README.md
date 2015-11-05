# Strucs - Flexible data structures in Scala

Strucs is a lightweight library that allows to manipulate, encode and decode flexible data structures while maintaining immutability and type safety.

A Struct is analogous to a case class that can accept new fields dynamically.

Using the strucs extensions, a single struc instance can be easily serialized/deserialized to various formats, such as JSON, FIX protocol, Protobuf, ...
  
## Quick start

### Create/Add/Update
The following code snippet can be pasted in a REPL session.
Check out the unit tests for more.

```tut:silent
import strucs._

case class Symbol(v: String) extends AnyVal
case class Quantity(v: BigDecimal) extends AnyVal
case class Price(v: BigDecimal) extends AnyVal
```
```tut
val order = Struct(Symbol("^FTSE"))
val order2 = order.add(Quantity(5))
order2.get[Symbol]
val order3 = order2.update(Symbol("^FCHI"))
order3.get[Symbol]
```
```tut:fail
order3.get[Price]
```

### Structural typing
*When I see a bird that walks like a duck and swims like a duck and quacks like a duck, I call that bird a duck.*

Let's define a function that accept any Struct that has one or more specific fields.
```tut
def totalPrice[T <: Quantity with Price](struct: Struct[T]): BigDecimal = {
  struct.get[Quantity].v * struct.get[Price].v
}
```
```tut:fail
totalPrice(order3)
```
```tut
totalPrice(order3.add(Price(10)))
```



### Encoding/Decoding
```tut

```

## Motivation
Let's say you want to manipulate Orders in your system.
The common approach would be: 
```tut
case class SimpleOrder(symbol: String, quantity: BigDecimal, price: BigDecimal)
```
Using simple types such as String, Int, BigDecimal, ... everywhere can rapidly make the code confusing and fragile.
Imagine we have to extract the price and quantity of all the FTSE orders 
```tut
def simpleFootsieOrders(orders: List[SimpleOrder]): List[(BigDecimal, BigDecimal)] = orders collect {
  case SimpleOrder(sym, q, p) if sym == "^FTSE" =>  (q, p)
}
```
If I do not get the argument order right (or if it has been refactored), the code above will compile but will not do what I expect.
Furthermore, the return type is List[(BigDecimal, BigDecimal)], which is unclear for the users of the function. 


At this point you might decide to use [value classes](http://docs.scala-lang.org/overviews/core/value-classes.html) to enforce a better type safety
```tut:silent
case class Symbol(v: String) extends AnyVal
val FTSE = Symbol("FTSE")
case class Quantity(v: BigDecimal) extends AnyVal
case class Price(v: BigDecimal) extends AnyVal

case class TypedOrder(symbol: Symbol, quantity: Quantity, price: Price)
```
```tut
def typedFootsieOrders(orders: List[TypedOrder]): List[(Quantity, Price)] = orders.collect {
  case TypedOrder(sym, q, p) if sym == FTSE => (q, p)
}
```
Now the return type is much clearer and safer, and my matching expression is safer as well: 
I cannot inadvertently swap arguments without getting a compilation error.

However, we now observe that the names of the attributes are redundant with their types. 
It would be nicer if we could declare them only once.
Also, I cannot easily reuse a set of fields in another case class:
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
```tut
type BaseOrderType = Symbol with Quantity with Nil
type StructOrder = Struct[BaseOrderType with Price]
type StructStopOrder = Struct[BaseOrderType with StopPrice]
def filterFootsie[T <: Symbol](orders: List[Struct[T]]) = orders.filter(_.get[Symbol] == FTSE)
```
The "order" types are now **composable**. I can define an abstraction BaseOrder, and reuse it to define other Order types. 
Also, I do not have to declare field names anymore, as I use only the types of the fields to access them.  
This composition capability also applies to instances:

```tut
val baseOrder = Struct.empty + FTSE + Quantity(100)
val order: StructOrder = baseOrder + Price(30)
val stopOrder: StructStopOrder = baseOrder + StopPrice(20)
filterFootsie(List(order, order.update(Symbol("CAC40"))))

```  

