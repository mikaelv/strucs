By November
- use Monoid for Json and Fix
- Json example with adding common bits for mongo
- example using disjunction (Either)
- compare with Shapeless ?

Easy
- rename packages: com.strucs -> strucs
- documentation for encode/decode
- Example with pre-defined Struct type (say OrderSingle), verifies that it does not compile / that it compiles regardless of the insertion order
- documentation for get with options

Nice to have
- change the Wrapper macro to accomodate wrappers of Struct

LONG-TERM - hard
- look at the compiled byte-code for optimization: does the AnyVal have any effect ?
- use packed arrays