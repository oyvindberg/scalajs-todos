package todomvc

import org.scalajs.dom
import org.scalajs.dom.XMLHttpRequest
import org.scalajs.dom.ext.AjaxException
import upickle._

import scala.concurrent.{Future, Promise}

object AjaxClient extends autowire.Client[String, Reader, Writer] {

  def post(url:             String,
           data:            String,
           timeout:         Int                 = 0,
           headers:         Map[String, String] = Map.empty,
           withCredentials: Boolean             = false,
           responseType:    String              = ""): Future[XMLHttpRequest] = {
    val req     = new dom.XMLHttpRequest()
    val promise = Promise[dom.XMLHttpRequest]()

    req.onreadystatechange = { (e: dom.Event) =>
      if (req.readyState == 4) {
        if ((req.status >= 200 && req.status < 300) || req.status == 304)
          promise.success(req)
        else
          promise.failure(AjaxException(req))
      }
    }
    req.open("POST", url)
    req.responseType = responseType
    req.timeout = timeout
    req.withCredentials = withCredentials
    headers.foreach(x => req.setRequestHeader(x._1, x._2))
    req.send(data)
    promise.future
  }

  override def doCall(req: Request): Future[String] =
    post(
      url          = s"todo-api/${req.path.mkString("/") }",
      data         = write(req.args),
      timeout      = 4000
    ).map(_.response.asInstanceOf[String])

  def read[Result: Reader](p: String)  = upickle.read[Result](p)
  def write[Result: Writer](r: Result) = upickle.write[Result](r)
}
