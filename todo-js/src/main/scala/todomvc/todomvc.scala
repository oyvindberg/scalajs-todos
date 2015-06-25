import japgolly.scalajs.react.vdom._

package object todomvc extends Base {
  import scalacss.ScalaCssReact._

  /* shows how to create new, alreayd styled html elements */
  object < extends JustTags {
    val myButton = button(CommonStyle.button)
  }
  object ^ extends JustAttrs {

  }

  implicit val executionContext =
    scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
}
