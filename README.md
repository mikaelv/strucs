# Strucs - Flexible data structures in Scala

Strucs is a lightweight library that allows to manipulate, encode and decode flexible data structures while maintaining immutability and type safety.

A Struct is analogous to a case class that can accept new fields dynamically.

Using the strucs extensions, a single struc instance can be easily serialized/deserialized to various formats, such as JSON, FIX protocol, Protobuf, ...
  
## Quick start

### Create/Add/Update
The following code snippet can be pasted in a REPL session.
Check out the unit tests for more.

```scala
import org.strucs._

case class Symbol(v: String) extends AnyVal
case class Quantity(v: BigDecimal) extends AnyVal
case class Price(v: BigDecimal) extends AnyVal

val order = Struct(Symbol("^FTSE"))
// order: org.strucs.Struct[Symbol]
val order2 = order.add(Quantity(5))
// order2: org.strucs.Struct[Symbol with Quantity]

order2.get[Symbol]
// res0: Symbol = Symbol(^FTSE)

val order3 = order2.update(Symbol("^FCHI"))
order3.get[Symbol]
// res1: Symbol = Symbol(^FCHI)

order3.get[Price]
// error: Cannot prove that Symbol with Quantity with Symbol <:< Price.
```

### Structural typing
*When I see a bird that walks like a duck and swims like a duck and quacks like a duck, I call that bird a duck.*

```scala
def totalPrice[T <: Quantity with Price](struct: Struct[T]): BigDecimal = {
  struct.get[Quantity].v * struct.get[Price].v
}

totalPrice(order3)
// error: inferred type arguments [Symbol with Quantity] do not conform to method totalPrice's type parameter bounds [T <: Quantity with Price]

totalPrice(order3.add(Price(10)))
// res2: BigDecimal = 50

```



### Encoding/Decoding


## Motivation

Let's say you want to manipulate Orders in your system.
The common approach would be: 
```scala
case class Order(symbol: String, quantity: BigDecimal, price: BigDecimal)
```
Using simple types such as String, Int, BigDecimal, ... everywhere can rapidly make the code confusing and fragile.
Imagine we have to extract the price and quantity of all the FTSE orders 
```scala
def footsieOrders(orders: List[Order]): List[(BigDecimal, BigDecimal)] = orders collect {
  case Order(sym, q, p) if sym == "^FTSE" =>  (q, p)
}
```
If I do not get the argument order right (or if it has been refactored), the code above will compile but will not do what I expect.
Furthermore, the return type is List[(BigDecimal, BigDecimal)], which is unclear for the users of the function. 


At this point you might decide to use [value classes](http://docs.scala-lang.org/overviews/core/value-classes.html) to enforce a better type safety
```scala
case class Symbol(v: String) extends AnyVal
object Symbol { val FTSE = Symbol("FTSE")} extends AnyVal
case class Quantity(v: BigDecimal) extends AnyVal
case class Price(v: BigDecimal) extends AnyVal

case class Order2(symbol: Symbol, quantity: Quantity, price: Price)

def footsieOrders2(orders: List[Order2]): List[(Quantity, Price)] = orders.collect {
  case Order2(sym, q, p) if sym == Symbol.FTSE => (q, p)
}
```
Now the return type is much clearer and safer, and my matching expression is safer as well: 
I cannot inadvertently swap arguments without getting a compile error.

However, we now observe that the names of the attributes are redundant with their types. 
It would be nicer if we could declare them only once.
Also, I cannot easily reuse a set of fields in another case class:
```scala
case class StopPrice(v: BigDecimal)
case class StopOrder(symbol: Symbol, quantity: Quantity, price: StopPrice)
```
If I then want to define a function that accepts StopOrder or Order2, I need to define a common trait that these classes will extend. 
This leads to quite a lot of duplication, and it may not even be feasible if Order2 is defined in a third party library.
 
 


