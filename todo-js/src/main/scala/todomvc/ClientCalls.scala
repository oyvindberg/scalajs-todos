package todomvc

import autowire._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalaz.effect.IO

class ClientCalls(commitF: Seq[Todo] ⇒ Unit) {
  private implicit class Commiter(f: Future[Seq[Todo]]){
    final def commit(): Unit = f foreach commitF
  }

  def todos(s: String): IO[Unit] =
    IO(AjaxClient[TodoApi].todos(s).call().commit())

  def addTodo(title: Title): IO[Unit] =
    IO(AjaxClient[TodoApi].addTodo(title).call().commit())

  def clearCompleted(): IO[Unit] =
    IO(AjaxClient[TodoApi].clearCompleted().call().commit())

  def delete(id: TodoId): IO[Unit] =
    IO(AjaxClient[TodoApi].delete(id).call().commit())

  def toggleAll(checked: Boolean): IO[Unit] =
    IO(AjaxClient[TodoApi].toggleAll(checked).call().commit())

  def toggleCompleted(id: TodoId): IO[Unit] =
    IO(AjaxClient[TodoApi].toggleCompleted(id).call().commit())

  def update(id: TodoId, text: Title): IO[Unit] =
    IO(AjaxClient[TodoApi].update(id, text).call().commit())
}
