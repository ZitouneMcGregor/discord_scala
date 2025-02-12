error id: scala/Boolean#
file://<WORKSPACE>/adminapi/src/main/scala/Models/User.scala
empty definition using pc, found symbol in pc: scala/Boolean#
empty definition using semanticdb
|empty definition using fallback
non-local guesses:
	 -Boolean#
	 -scala/Predef.Boolean#

Document text:

```scala
package models

case class User(
    id: Option[Int], 
    username: String, 
    password: String,
    deleted: Boolean
)

```

#### Short summary: 

empty definition using pc, found symbol in pc: scala/Boolean#