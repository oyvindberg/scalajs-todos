package todomvc

import java.io.{File, OutputStreamWriter}
import java.net.URL

import unfiltered.filter.async.Plan
import unfiltered.filter.request.ContextPath
import unfiltered.request.{Body, Seg}
import unfiltered.response._
import upickle._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Failure, Success, Properties}

object TodoServer{
  object Router extends autowire.Server[String, Reader, Writer] {
    def read[ Result: Reader](p: String) = upickle.read[Result](p)
    def write[Result: Writer](r: Result) = upickle.write[Result](r)
  }

  implicit class ResponseFunctionX(rf: ResponseFunction[Any]){
    //eliminate compiler warning for inferring Any when using ~>
    def ~~>(other: ResponseFunction[Any]) = rf ~> other
  }

  class UnfilteredIntegration(routers: Router.Router*) extends Plan {
    val route     = routers.reduce(_ orElse _)

    override val intent: Plan.Intent = {
      case req@ContextPath(_, Seg("arne-todo-api" :: s)) ⇒
        val body = Body.string(req)
        Try(upickle.read[Map[String, String]](body)) match {
          case Success(parsed) ⇒
            route(autowire.Core.Request(s, parsed)) onComplete {
              case Success(result) ⇒
                println(s"Success: $s")
                req.respond(JsonContent ~~> ResponseString(result))
              case Failure(th)     ⇒
                println(s"Call to $s failed: ${th.getMessage}")
                req.respond(BadRequest)
            }
          case Failure(th) ⇒
            println(s"Call to $s: failed because couldn't parse args$body: ${th.getMessage}")
            req.respond(BadRequest)
        }

      case req@ContextPath(_, Seg(Nil)) ⇒
        req.respond(Html5(HtmlTemplate.html))
    }
  }
  case class UrlStream(res: URL) extends ResponseWriter {
    def write(writer: OutputStreamWriter): Unit = {
      val is = res.openStream()

      while(is.available() > 0){
        writer.write(is.read())
      }
      is.close()
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
    unfiltered.jetty.Server.local(port)
      .plan(new UnfilteredIntegration(Router.route[TodoApi](storage)))
      .resources(resources)
      .originalContext(_.allowAliases(true))
      .run()
    }
}
