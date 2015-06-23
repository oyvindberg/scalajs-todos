package todomvc

import autowire._
import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.extra.router2.RouterCtl
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import upickle.{Reader, Writer}

import scala.concurrent.Future
import scala.scalajs.js
import scala.util.{Failure, Success}
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.StyleSheet
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
            dom.console.warn(s"Failed remote call: $t")
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
        <.div(
          Style.todoList,
          <.myButton("here", ^.onClick ~~> remote(_.todos("arne").call())),
          <.h1("todos", Style.header),
          <.header(
            <.input(
              Styles.CommonStyles.editText,
              Style.newTodo,
              ^.placeholder   := "What needs to be done?",
              ^.onKeyDown  ~~>? handleNewTodoKeyDown,
              ^.autoFocus     := true
            )
          ),
          todos.nonEmpty ?= todoList(filteredTodos, activeCount),
          todos.nonEmpty ?= footer(activeCount, completedCount)
        ),
        bottomText
      )
    }

    def renderToggleInput(activeCount: Int) =
      <.input(
        Style.toggleAll,
        ^.`type`     := "checkbox",
        ^.checked    := activeCount == 0,
        ^.onChange ~~> toggleAll
      )

    def todoList(filteredTodos: Seq[Todo], activeCount: Int): ReactElement =
      <.section(
        renderToggleInput(activeCount),
        <.ul(
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

    val bottomText =
      <.footer(
        Style.BottomText.info,
        <.p(Style.BottomText.p, "Double-click to edit a todo"),
        <.p(Style.BottomText.p, "Created by ",
          <.a(
            Style.BottomText.a,
            ^.href := "http://github.com/elacin/",
            "Øyvind Raddum Berg"
          )
        )
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
    .componentDidMountIO($ ⇒ $.backend.remote(_.todos("aasdasd").call()))
    /**
     * For performance reasons its important to only call `build` once for each component
     */
    .build

  def apply(currentFilter: TodoFilter)(ctl: RouterCtl[TodoFilter]) =
    component(Props(ctl, currentFilter))

    object Style extends StyleSheet.Inline {
      import dsl._

      val todoList = style(
        listStyle := "none",
        zIndex(2),
        borderTop(1.px, solid, c"#e6e6e6"),
        backgroundColor(c"#fff"),
        margin(130.px, `0`, 40.px, `0`),
        position.relative,
        boxShadow :=
        """0 2px   4px 0 rgba(0, 0, 0, 0.2),
           0 25px 50px 0 rgba(0, 0, 0, 0.1)"""
      )

      val header = style(
      	position.absolute,
        top((-155).px),
        width(100.%%),
        fontSize(100.px),
        fontWeight._100,
        textAlign.center,
        color(c"rgba(175, 47, 47, 0.15)"),
        textRendering := "optimizeLegibility"
      )

      val newTodo = style(
        padding(16.px, 16.px, 16.px, 60.px),
        border.none,
        backgroundColor(c"rgba(0, 0, 0, 0.003)"),
        boxShadow := "inset 0 -2px 1px rgba(0,0,0,0.03)"
      )

      val toggleAll = style(
        position.absolute,
        top((-55).px),
        left((-12).px),
        width(60.px),
        height(34.px),
        textAlign.center,
        border.none /* Mobile Safari */,
        &.before(
          content := "❯",
          fontSize(22.px),
          color(c"#e6e6e6"),
          padding(10.px, 27.px, 10.px, 27.px)
        ),
        &.checked(
          color(c"#737373")
        )
      )

      object BottomText {
        val info = style(
          margin(65.px, auto, `0`),
          color(c"#bfbfbf"),
          fontSize(10.px),
          textShadow := "0 1px 0 rgba(255, 255, 255, 0.5)",
          textAlign.center
        )
        val p = style(lineHeight(1))

        val a = style(
          color.inherit,
          textDecorationLine.none,
          fontWeight._400,
          &.hover(
            textDecorationLine.underline
          )
        )
      }
      initInnerObjects(BottomText.a)
    }
}
