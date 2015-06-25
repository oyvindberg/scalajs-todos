# Extended Scalajs-React TodoMVC Example

I originally wrote a a [TodoMVC example for scalajs-react][todomvc-example]. 
For this project, i extended it in two ways:

- css is now done purely through [scalacss][scalacss], a more or less typesafe 
 library that generates css. I tried to simplify things, so
 while this example is not meant to be a 100% replica, it's 
 very close.
 
- there is now a [netty][netty] server that keeps track of the current todo 
 list (shared among all users, because), so there is the necessary 
 plumbing to setup [autowire][autowire] with [unfiltered][unfiltered] 
 in order to get typesafe remote calls.
 
- there is shared scala code between server and client

## Running

start `sbt`, and run `~reStart`, then open a browser at `localhost:8080`

[todomvc-example]: https://github.com/elacin/todomvc/tree/scalajs-react-rebased
[scalacss]: https://github.com/japgolly/scalacss
[netty]: http://netty.io/
[autowire]: https://github.com/lihaoyi/autowire
[unfiltered]: http://unfiltered.databinder.net