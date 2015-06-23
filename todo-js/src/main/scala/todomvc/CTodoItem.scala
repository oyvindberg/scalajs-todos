package todomvc

import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react._
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode

import scala.scalajs.js
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.StyleSheet
import scalaz.effect.IO
import scalaz.std.anyVal.unitInstance
import scalaz.syntax.semigroup._
import scalaz.syntax.std.option._
import scala.concurrent.duration._

object CTodoItem {

  case class Props private[CTodoItem] (
    onToggle:        IO[Unit],
    onDelete:        IO[Unit],
    onStartEditing:  IO[Unit],
    onUpdateTitle:   Title => IO[Unit],
    onCancelEditing: IO[Unit],
    todo:            Todo,
    isEditing:       Boolean
  )

  case class State(editText: UnfinishedTitle, showDestroy: Boolean)

  case class Backend($: BackendScope[Props, State]){

    val editFieldSubmit: Option[IO[Unit]] =
      $.state.editText.validated.map($.props.onUpdateTitle)

    val resetText: IO[Unit] =
      $.modStateIO(_.copy(editText = $.props.todo.title.editable))

    val editFieldKeyDown: ReactKeyboardEvent => Option[IO[Unit]] =
      e => e.nativeEvent.keyCode match {
        case KeyCode.Escape => (resetText |+| $.props.onCancelEditing).some
        case KeyCode.Enter  => editFieldSubmit
        case _              => None
      }

    val editFieldChanged: ReactEventI => IO[Unit] =
      e => $.modStateIO(_.copy(editText = UnfinishedTitle(e.target.value)))

    val destroyBtn =
      <.myButton(Style.destroyBtn, ^.onClick ~~> $.props.onDelete, "×")

    def render: ReactElement = {

      def view = <.div(
          <.input(
            Style.toggleChk,
            ^.`type`    := "checkbox",
            ^.checked   := $.props.todo.isCompleted,
            ^.onChange ~~> $.props.onToggle
          ),
          <.label(
            Style.itemLabel($.props.todo.isCompleted),
            $.props.todo.title.value,
            ^.onDoubleClick ~~> $.props.onStartEditing
          ),
          $.state.showDestroy ?= destroyBtn
        )

      def edit = <.input(
          Style.edit,
          Styles.CommonStyles.editText,
          ^.onBlur    ~~>? editFieldSubmit,
          ^.onChange   ~~> editFieldChanged,
          ^.onKeyDown ~~>? editFieldKeyDown,
          ^.value       := $.state.editText.value
        )

      <.li(
        Style.listItem,
        ^.onMouseOver --> js.timers.setTimeout(200){$.modState(_.copy(showDestroy = true))},
        ^.onMouseOut  --> js.timers.setTimeout(200){$.modState(_.copy(showDestroy = false))},
        if ($.props.isEditing) edit else view
      )
    }
  }

  private val component = ReactComponentB[Props]("CTodoItem")
    .initialStateP(p => State(p.todo.title.editable, showDestroy = false))
    .backend(Backend)
    .render(_.backend.render)
    .build

  def apply(onToggle:        IO[Unit],
            onDelete:        IO[Unit],
            onStartEditing:  IO[Unit],
            onUpdateTitle:   Title => IO[Unit],
            onCancelEditing: IO[Unit],
            todo:            Todo,
            isEditing:       Boolean) =

    component.withKey(todo.id.id.toString)(
      Props(
        onToggle        = onToggle,
        onDelete        = onDelete,
        onStartEditing  = onStartEditing,
        onUpdateTitle   = onUpdateTitle,
        onCancelEditing = onCancelEditing,
        todo            = todo,
        isEditing       = isEditing
      )
    )

  object Style extends StyleSheet.Inline {
    import dsl._

    val listItem = style(
      position.relative,
      fontSize(24.px),
      borderBottom(1.px, solid, c"#ededed"),

      &.lastChild(
        borderBottom.none
      )
    )

    val edit = style(
      display.block,
      width(506.px),
      padding(13.px, 17.px, 12.px, 17.px),
      margin(`0`, `0`, `0`, 43.px)
    )

    val toggleChk = style(
      textAlign.center,
      width(40.px),
      height.auto,
      position.absolute,
      top.`0`,
      bottom.`0`,
      margin(auto, `0`),
      border.none,
      Styles.appearance := "none",

      &.after(
        content := """url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="-10 -18 100 135"><circle cx="50" cy="50" r="50" fill="none" stroke="#ededed" stroke-width="3"/></svg>')"""
      ),
      &.after.checked(
        content := """url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="-10 -18 100 135"><circle cx="50" cy="50" r="50" fill="none" stroke="#bddad5" stroke-width="3"/><path fill="#5dc2af" d="M72 25L42 71 27 56l-4 4 20 20 34-52z"/></svg>')"""
      )
    )

    val itemLabel = styleF.bool(
      isCompleted ⇒ styleS(
        whiteSpace.pre,
        wordBreak.breakAll,
        padding(15.px, 60.px, 15.px, 15.px),
        marginLeft(45.px),
        display.block,
        lineHeight(1.2),
        transitionProperty := "color",
        transitionDuration(400.millisecond),
        if (isCompleted) textDecorationLine.lineThrough
        else             textDecorationLine.none
      )
    )

    val destroyBtn = style(
      display.block,
      position.absolute,
      top.`0`,
      right(10.px),
      bottom.`0`,
      width(40.px),
      height(40.px),
      fontSize(30.px),
      color(c"#cc9a9a"),
      marginBottom(11.px),
      transition := "color, 0.2s, ease-out"
    )
  }
}
