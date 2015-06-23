package todomvc

import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry
import scalacss.{Attr, CanIUse, Transform}

object Styles {
  final val appearance = Attr.real("appearance", Transform keys CanIUse.appearance)

  object CommonStyles extends StyleSheet.Inline {
    import dsl.{unsafeRoot ⇒ $, _}

    val basis = style(
      $("body")(
        margin(`0`, auto),
        padding.`0`,
        font := "14px 'Helvetica Neue', Helvetica, Arial, sans-serif",
        lineHeight(1.4.em),
        backgroundColor(c"#f5f5f5"),
        color(c"#4d4d4d"),
        minWidth(230.px),
        maxWidth(550.px),
        fontWeight._300
      ),
      $("""input[type="checkbox"]""")(
        outline.none
      )
    )

    val button = style(
      border.`0`,
      background := "none",
      fontSize(100.%%),
      verticalAlign.baseline,
      fontFamily := "inherit",
      fontWeight.inherit,
      color.inherit,
      appearance := "none",
      outline.none
    )

    val editText = style(
      position.relative,
      margin.`0`,
      width(100.%%),
      fontSize(24.px),
      fontFamily := "inherit",
      fontWeight.inherit,
      lineHeight(1.4.em),
      outline.none,
      color.inherit,
      padding(6.px),
      border(1.px, solid, c"#999"),
      boxShadow := "inset 0 -1px 5px 0 rgba(0, 0, 0, 0.2)",
      boxSizing.borderBox,
      Styles.appearance := "antialiased"
    )
  }

  def load() = {
    GlobalRegistry.register(CommonStyles, CFooter.Style, CTodoItem.Style, CTodoList.Style)
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
