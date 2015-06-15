package todomvc

import org.scalajs.dom
import upickle.{Reader, Writer}

import scala.util.{Failure, Success, Try}

case class Storage(storage: dom.ext.Storage, namespace: String) {
  def write[T: Writer](data: T) =
    storage(namespace) = upickle.write(data)

  def read[T: Reader]: Option[T] =
    Try(storage(namespace).map(s => upickle.read(s))) match {
      case Success(Some(t)) => Some(t)
      case Success(None)    => None
      case Failure(th)      =>
        dom.console.error(s"Got invalid data ${th.getMessage}")
        None
    }
}
