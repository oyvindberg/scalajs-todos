package todomvc

import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalaz.effect.IO
import scalaz.syntax.std.list._
import scalaz.syntax.std.string._

object CFooter {

  case class Props private[CFooter](
    filterLink:       TodoFilter => ReactTag,
    onClearCompleted: IO[Unit],
    currentFilter:    TodoFilter,
    activeCount:      Int,
    completedCount:   Int
  )

  case class Backend($: BackendScope[Props, Unit]){
    val clearButton =
      <.button(Style.clearCompleted,
        ^.onClick ~~> $.props.onClearCompleted,
        "Clear completed"
      )

    def filterLink(s: TodoFilter) =
      <.li(Style.li,
        $.props.filterLink(s)(Style.filterLink($.props.currentFilter == s), s.title)
      )

    def withSpaces(ts: TagMod*) =
      ts.toList.intersperse(" ")

    def render =
      <.footer(Style.footer,
        <.span(Style.todoCount,
          <.strong(Style.strong,
            $.props.activeCount
          ),
          s" ${"item" plural $.props.activeCount} left"
        ),
        <.ul(Style.filters,
          withSpaces(TodoFilter.values map filterLink)
        ),
        ($.props.completedCount > 0) ?= clearButton
      )
  }

  private val component = ReactComponentB[Props]("CFooter")
    .stateless
    .backend(Backend)
    .render(_.backend.render)
    .build

  object Style extends StyleSheet.Inline {
    import dsl._

    val footer = style(
      color("#777"),
      padding(10.px, 15.px),
      height(20.px),
      textAlign.center,
      borderTopWidth(1.px),
      borderTopColor("#e6e6e6"),
      borderTopStyle.solid,

      &.before(
        position.absolute,
        right(`0`),
        bottom(`0`),
        left(`0`),
        height(50.px),
        overflow.hidden,
        boxShadow :=
          """
            |0 1px 1px rgba(0, 0, 0, 0.2),
            |0 8px 0 -3px #f6f6f6,
            |0 9px 1px -3px rgba(0, 0, 0, 0.2),
            |0 16px 0 -6px #f6f6f6,
            |0 17px 2px -6px rgba(0, 0, 0, 0.2);
        """.stripMargin
      )
    )

    val todoCount = style(
      float.left,
      textAlign.left
    )

    val strong = style(
      fontWeight._300
    )

    val clearCompleted = style(
      float.right,
      position.relative,
      lineHeight(20.px),
      cursor.pointer,
      textDecoration := "none",

      &.hover(
        textDecoration := "underline"
      )
    )

    val filters = style(
      margin(`0`),
      padding(`0`),
      listStyle := "none",
      position.absolute,
      right(`0`),
      left(`0`)
    )
    
    val li = style(
      display.inline
    )

    def filterLink(selected: Boolean) = if (selected) filterLinkSelected else filterLinkNotSelected

    val filterLinkAbstract = style(
      color.inherit,
      margin(3.px),
      padding(3.px, 7.px),
      textDecoration := "none",
      borderWidth(1.px),
      borderStyle.solid,
      borderRadius(3.px)
    )

    val filterLinkNotSelected = style(filterLinkAbstract,
      borderColor.transparent,
      &.hover(
        borderColor.rgba(175, 47, 47, 0.1)
      )
    )

    val filterLinkSelected = style(filterLinkAbstract,
      borderColor.rgba(175, 47, 47, 0.2)
    )
  }

  def apply(filterLink:       TodoFilter => ReactTag,
            onClearCompleted: IO[Unit],
            currentFilter:    TodoFilter,
            activeCount:      Int,
            completedCount:   Int) =

    component(
      Props(
        filterLink       = filterLink,
        onClearCompleted = onClearCompleted,
        currentFilter    = currentFilter,
        activeCount      = activeCount,
        completedCount   = completedCount
      )
    )
}

