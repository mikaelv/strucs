# Strucs - Flexible data structures in Scala

Strucs is a lightweight library that allows to manipulate and serialize flexible data structures while maintaining immutability and type safety.
A Struct is analogous to a case class or an unordered tuple
Using the strucs extensions, A single struc instance can be easily serialized/deserialized to various formats, such as JSON, FIX protocol, Protobuf, ...
  
## Quick start


## Motivation

Let's say you want to manipulate Orders in your system.
The common approach would be: 
```scala
case class Order(symbol: String, quantity: BigDecimal, price: BigDecimal)
```
Using String everywhere can rapidly make the code confusing and fragile.
Imagine we have to extract the price and quantity of all the FTSE orders 
```scala
def footsieOrders(orders: List[Order]): List[(BigDecimal, BigDecimal)] = orders collect {
  case Order(sym, q, p) if sym == "^FTSE" =>  (q, p)
}
```
If I do not get the argument order right (or if it has been refactored), the code above will compile but will not do what I expect.
Furthermore, the return type is List[(BigDecimal, BigDecimal)], which is not clear for the users of the function. 


At this point you might decide to use wrapper classes to enforce a better type safety
```scala
case class Symbol(v: String)
object Symbol { val FTSE = Symbol("FTSE")}
case class Quantity(v: BigDecimal)
case class Price(v: BigDecimal)

case class Order2(symbol: Symbol, quantity: Quantity, price: Price)

def footsieOrders2(orders: List[Order2]): List[(Quantity, Price)] = orders.collect {
  case Order2(sym, q, p) if sym == Symbol.FTSE => (q, p)
}
```
Now the return type is much clearer and safer, and my matching expression is safer as well: 
I cannot inadvertently swap argument without getting a compile error.

However, we now observe that the names of the attributes are redundant with their types. 
It would be nicer if we could declare them only once.
Also, I cannot easily reuse a set of fields in another case class:
```scala
case class StopPrice(v: BigDecimal)
case class StopOrder(symbol: Symbol, quantity: Quantity, price: StopPrice)
```
If I then want to define a function that accepts StopOrder or Order2, I need to define a common trait that these classes will extend. 
This leads to quite a lot of duplication, and it may not even be feasible if Order2 is defined in a third party library.
 
 


