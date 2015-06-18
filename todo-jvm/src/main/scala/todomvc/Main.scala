package todomvc

import unfiltered.filter.Plan
import unfiltered.filter.request.ContextPath
import unfiltered.request.{Body, Seg}
import unfiltered.response.{ResponseFunction, HeaderName, ResponseString}
import upickle._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Main{
  object Router extends autowire.Server[String, Reader, Writer] {
    def read[ Result: Reader](p: String) = upickle.read[Result](p)
    def write[Result: Writer](r: Result) = upickle.write[Result](r)
  }

  implicit class ResponseFunctionX(rf: ResponseFunction[Any]){
    //eliminate compiler warning for inferring Any when using ~>
    def ~~>(other: ResponseFunction[Any]) = rf.~>(other)
  }

  class Server(routers: Router.Router*) extends Plan {
    val cors  = new HeaderName("Access-Control-Allow-Origin")
    val route = routers.reduce(_ orElse _)

    override val intent: Plan.Intent = {
      case req@ContextPath(_, Seg("todo-api" :: s)) ⇒
        val p       = upickle.read[Map[String, String]](Body.string(req))
        val resultF = route(autowire.Core.Request(s, p))
        val result  = Await.result(resultF, 10.second)
        cors("*") ~~> ResponseString(result)
    }
  }
  
  val storage = new TodoStorage
  val server  = new Server(
    Router.route[TodoApi](storage)
  )

  def main(args: Array[String]): Unit =
    unfiltered.jetty.Server.local(8080).plan(server).run()
}
