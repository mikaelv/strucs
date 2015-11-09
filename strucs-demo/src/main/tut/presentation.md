class: center, middle

# strucs
Flexible data structures in scala

https://github.com/mikaelv/strucs
---

# Adding fields

```tut:silent
case class Name(v: String) extends AnyVal
case class Age(v: Int) extends AnyVal
```
???
Each field of the struct must have its own type. Referred to as Wrapper type.
Inside a Struct, each field is uniquely identified by its type
--
```tut:invisible
import strucs._
```
```tut
val person = Struct.empty + Name("Mikael") + Age(39)
```
???
We will have a look later at the internal structure
--
```tut:silent
case class Street(v: String) extends AnyVal
case class City(v: String) extends AnyVal
val address = Struct(City("London")) + Street("52 Upper Street")
```
```tut
val personWithAddress = person ++ address
```

---
# Accessing fields
```tut
person.get[Name]
```
```tut:fail
person.get[Street]
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
# Encoding
---
# Decoding
---
# Under the hood
```
case class Struct {
* look ma, this is yellow
}
```
---
# Under the hood
```
Encoder comma
```
---