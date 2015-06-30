package todomvc

import org.scalajs.dom.ext.Ajax
import upickle._

import scala.concurrent.Future

object AjaxClient extends autowire.Client[String, Reader, Writer] {

  override def doCall(req: Request): Future[String] =
    Ajax.post(
      url          = s"todo-api/${req.path.mkString("/") }",
      data         = write(req.args),
      timeout      = 4000
    ).map(_.response.asInstanceOf[String])

  def read[Result: Reader](p: String)  = upickle.read[Result](p)
  def write[Result: Writer](r: Result) = upickle.write[Result](r)
}
