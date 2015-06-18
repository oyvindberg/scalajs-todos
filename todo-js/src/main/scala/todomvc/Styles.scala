package todomvc

import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry
import scalacss.Defaults._

//object GlobalStyle extends StyleSheet.Inline {
//
//  import dsl._
//
//  style(unsafeRoot("body")(
//    margin.`0`,
//    padding.`0`
//  ))
//}

object Styles {

  def load() = {
    GlobalRegistry.register(
//      GlobalStyle,
      CFooter.Style)
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
