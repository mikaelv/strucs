By November
- Json decoding
- ValidationNel for decoding 
- use Monoid for Json and Fix
- example using disjunction (Either)
- compare with Shapeless ?

Easy
- rename packages: com.strucs -> strucs
- example enum in EncoderSpec + deal with the case object types in the macro
- documentation for encode/decode
- Example with pre-defined Struct type (say OrderSingle), verifies that it does not compile / that it compiles regardless of the insertion order
- documentation for get with options

LONG-TERM - hard
- look at the compiled byte-code for optimization: does the AnyVal have any effect ?
- use packed arrays