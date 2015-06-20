package todomvc

import autowire._
import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import upickle.{Reader, Writer}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.util.{Failure, Success}
import scalaz.effect.IO
import scalaz.std.anyVal.unitInstance
import scalaz.syntax.semigroup._

object CTodoList {

  case class Props private[CTodoList] (
    ctl:           RouterCtl[TodoFilter],
    currentFilter: TodoFilter
  )

  case class State(
    todos:   Seq[Todo],
    editing: Option[TodoId]
  )

  /**
   * These specify when it makes sense to skip updating this component (see comment on `Listenable` below)
   */
  implicit val r1 = Reusability.fn[Props](
    (p1, p2) ⇒ p1.currentFilter == p2.currentFilter
  )

  implicit val r2 = Reusability.fn[State](
    (s1, s2) ⇒ s1.editing == s2.editing && (s1.todos eq s2.todos)
  )

  /**
   * One difference between normal react and scalajs-react is the use of backends.
   * Since components are not inheritance-based, we often use a backend class
   * where we put most of the functionality: rendering, state handling, etc.
   */
  case class Backend($: BackendScope[Props, State]) {

    /* sets up how we respond to successful backend calls */
    object remote {
      def apply[T](f: ClientProxy[TodoApi, String, Reader, Writer] ⇒ Future[Seq[Todo]]): IO[Unit] =
        IO(f(AjaxClient[TodoApi]) onComplete {
          case Success(newTodos) ⇒
            $.modState(_.copy(todos = newTodos))
          case Failure(t) ⇒
            dom.console.warn(s"Failed update: $t")
        })
    }

    val handleNewTodoKeyDown: ReactKeyboardEventI => Option[IO[Unit]] =
      e ⇒ Some((e.nativeEvent.keyCode, UnfinishedTitle(e.target.value).validated)) collect {
        case (KeyCode.Enter, Some(title)) =>
          IO(e.target.value = "") |+| remote(_.addTodo(title).call())
      }

    val updateTitle: TodoId ⇒ Title ⇒ IO[Unit] =
      id ⇒ title ⇒ editingDone(cb = remote(_.update(id, title).call()))

    val toggleAll: ReactEventI ⇒ IO[Unit] =
      e ⇒ remote(_.toggleAll(e.target.checked).call())

    val startEditing: TodoId ⇒ IO[Unit] =
      id ⇒ $.modStateIO(_.copy(editing = Some(id)))

    def editingDone(cb: OpCallbackIO = js.undefined): IO[Unit] =
      $.modStateIO(_.copy(editing = None), cb)

    def render: ReactElement = {
      val todos           = $.state.todos
      val filteredTodos   = todos filter $.props.currentFilter.accepts

      val activeCount     = todos count TodoFilter.Active.accepts
      val completedCount  = todos.length - activeCount

      <.div(
        <.h1("todos"),
        <.header(
          ^.className := "header",
          <.input(
            ^.className     := "new-todo",
            ^.placeholder   := "What needs to be done?",
            ^.onKeyDown  ~~>? handleNewTodoKeyDown,
            ^.autoFocus     := true
          )
        ),
        todos.nonEmpty ?= todoList(filteredTodos, activeCount),
        todos.nonEmpty ?= footer(activeCount, completedCount)
      )
    }

    def todoList(filteredTodos: Seq[Todo], activeCount: Int): ReactElement =
      <.section(
        ^.className := "main",
        <.input(
          ^.className  := "toggle-all",
          ^.`type`     := "checkbox",
          ^.checked    := activeCount == 0,
          ^.onChange ~~> toggleAll
        ),
        <.ul(
          ^.className := "todo-list",
          filteredTodos.map(todo =>
            CTodoItem(
              onToggle         = remote(_.toggleCompleted(todo.id).call()),
              onDelete         = remote(_.delete(todo.id).call()),
              onStartEditing   = startEditing(todo.id),
              onUpdateTitle    = updateTitle(todo.id),
              onCancelEditing  = editingDone(),
              todo             = todo,
              isEditing        = $.state.editing.contains(todo.id)
            )
          )
        )
      )

    def footer(activeCount: Int, completedCount: Int): ReactElement =
      CFooter(
        filterLink       = $.props.ctl.link,
        onClearCompleted = remote(_.clearCompleted().call()),
        currentFilter    = $.props.currentFilter,
        activeCount      = activeCount,
        completedCount   = completedCount
      )
  }

  private val component = ReactComponentB[Props]("CTodoList")
    .initialState(State(Seq(), None))
    .backend(Backend)
    .render(_.backend.render)
    /**
     * Optimization where we specify whether the component can have changed.
     * In this case we avoid comparing model and routerConfig, and only do
     *  reference checking on the list of todos.
     *
     * The implementation of the «equality» checks are in the Reusability
     *  typeclass instances for `State` and `Props` at the top of the file.
     *
     *  To understand how things are redrawn, change `shouldComponentUpdate` for
     *  either `shouldComponentUpdateWithOverlay` or `shouldComponentUpdateAndLog`
     */
    .configure(Reusability.shouldComponentUpdate)
    .componentDidMountIO($ ⇒ $.backend.remote(_.todos("").call()))
    /**
     * For performance reasons its important to only call `build` once for each component
     */
    .build

  def apply(currentFilter: TodoFilter)(ctl: RouterCtl[TodoFilter]) =
    component(Props(ctl, currentFilter))
}
