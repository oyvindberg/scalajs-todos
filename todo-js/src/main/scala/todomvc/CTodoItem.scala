package todomvc

import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react._
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

    val editFieldSubmit: UnfinishedTitle ⇒ Option[IO[Unit]] =
      _.validated filterNot (_ == $.props.todo.title) map $.props.onUpdateTitle

    val resetText: IO[Unit] =
      $.modStateIO(_.copy(editText = $.props.todo.title.editable))

    val editFieldKeyDown: ReactKeyboardEvent => Option[IO[Unit]] =
      e => e.nativeEvent.keyCode match {
        case KeyCode.Escape => (resetText |+| $.props.onCancelEditing).some
        case KeyCode.Enter  => editFieldSubmit($.state.editText)
        case _              => None
      }

    val editFieldChanged: ReactEventI => IO[Unit] =
      e => $.modStateIO(_.copy(editText = UnfinishedTitle(e.target.value)))

    val destroyBtn =
      <.myButton(Style.destroyBtn, ^.onClick ~~> $.props.onDelete, "×")

    val startEditing: ReactEventI ⇒ IO[Unit] =
      e ⇒ $.props.onStartEditing.map(_ ⇒ e.preventDefault())

    def render: ReactElement = {
      <.li(
        Style.itemContainer,
        ^.onMouseOver --> js.timers.setTimeout(200){$.modState(_.copy(showDestroy = true))},
        ^.onMouseOut  --> js.timers.setTimeout(200){$.modState(_.copy(showDestroy = false))},
        <.div(
          <.input(
            Style.toggleChk,
            ^.`type`    := "checkbox",
            ^.checked   := $.props.todo.isCompleted,
            ^.onChange ~~> $.props.onToggle
          ),
          todoText($.props.isEditing),
          $.state.showDestroy ?= destroyBtn,
          ^.onDoubleClick ~~> startEditing
        )
      )
    }

    def todoText(isEditing: Boolean) =
      <.div(Style.itemFadeOnComplete($.props.todo.isCompleted))(
        if (isEditing)
          <.input(
            CommonStyle.editText,
            Style.itemEdit,
            ^.onBlur    ~~>? editFieldSubmit($.state.editText),
            ^.onChange   ~~> editFieldChanged,
            ^.onKeyDown ~~>? editFieldKeyDown,
            ^.value       := $.state.editText.value
          )
        else
          <.label(
            Style.itemLabel,
            $.props.todo.title.value
          )
      )
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

    val itemContainer = style(
      position.relative,
      fontSize(24.px),
      borderBottom(1.px, solid, c"#ededed"),

      &.lastChild(
        borderBottom.none
      )
    )

    val itemFadeOnComplete = styleF.bool(
      isCompleted ⇒ styleS(
        padding(15.px, 60.px, 15.px, 15.px),
        marginLeft(45.px),
        lineHeight(1.2),
        height(30.px),
        transitionProperty := "color",
        transitionDuration(400.millisecond),
        if (isCompleted) styleS(textDecoration := "line-through", color(c"#d9d9d9"))
        else             textDecoration := "none"
      )
    )

    val itemLabel = style(
      whiteSpace.pre,
      wordBreak.breakAll

    )

    val itemEdit = style(
      marginTop.`0`
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

    val toggleChk = style(
      CommonStyle.webkitOnly(CommonStyle.appearance := "none"),
      position.absolute,
      textAlign.center,
      width(40.px),
      height.auto,
      top(5.px),

      &.after(
        content := """url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="-10 -18 100 135"><circle cx="50" cy="50" r="50" fill="none" stroke="#ededed" stroke-width="3"/></svg>')"""
      ),
      &.after.checked(
        content := """url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="-10 -18 100 135"><circle cx="50" cy="50" r="50" fill="none" stroke="#bddad5" stroke-width="3"/><path fill="#5dc2af" d="M72 25L42 71 27 56l-4 4 20 20 34-52z"/></svg>')"""
      )
    )

  }
}
