import japgolly.scalajs.react.vdom._

package object todomvc extends Base {
  import scalacss.ScalaCssReact._

  object < extends JustTags {
    val myButton = button(Styles.CommonStyles.button)
  }
  object ^ extends JustAttrs {

  }

  implicit val executionContext =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
}
