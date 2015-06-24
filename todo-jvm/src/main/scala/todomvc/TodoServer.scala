package todomvc

import java.io.File

import unfiltered.request.{Body, Path, Seg}
import unfiltered.response._
import upickle._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Properties, Success, Try}

object TodoServer{
  object Router extends autowire.Server[String, Reader, Writer] {
    def read[ Result: Reader](p: String) = upickle.read[Result](p)
    def write[Result: Writer](r: Result) = upickle.write[Result](r)
  }

  implicit class ResponseFunctionX(rf: ResponseFunction[Any]){
    //eliminate compiler warning for inferring Any when using ~>
    def ~~>(other: ResponseFunction[Any]) = rf ~> other
  }

  class UnfilteredIntegration(routers: Router.Router*) {
    val route     = routers.reduce(_ orElse _)

    val intent = unfiltered.netty.cycle.Planify {
      case req@Path(Seg("arne-todo-api" :: s)) ⇒
        val body = Body.string(req)
        Try(upickle.read[Map[String, String]](body)) match {
          case Success(parsed) ⇒
            Try(Await.result(route(autowire.Core.Request(s, parsed)), 2.seconds)) match {
              case Success(result) ⇒
                println(s"Success: $s")
                JsonContent ~~> ResponseString(result)
              case Failure(th)     ⇒
                println(s"Call to $s failed: ${th.getMessage}")
                BadRequest
            }
          case Failure(th) ⇒
            println(s"Call to $s: failed because couldn't parse args$body: ${th.getMessage}")
            BadRequest
        }

      case req@Path(Seg(Nil)) ⇒
        Html5(HtmlTemplate.html)
    }
  }

  val storage    = new TodoStorage
  val resources  = new File("../todo-js/target/scala-2.11/").toURI.toURL

  storage.addTodo(Title("a1"))
  storage.addTodo(Title("a2"))
  storage.addTodo(Title("a3"))
  storage.addTodo(Title("a4"))
  storage.addTodo(Title("a5"))

  def main(args: Array[String]): Unit = {
    val port = Properties.envOrElse("PORT", "8080").toInt
    unfiltered.netty.Server.local(port)
      .plan(new UnfilteredIntegration(Router.route[TodoApi](storage)).intent)
      .resources(resources)
      .run()
    }
}
