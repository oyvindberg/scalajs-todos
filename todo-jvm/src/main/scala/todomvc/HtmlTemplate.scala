package todomvc

object HtmlTemplate{
  val html =
    <html lang="en">
      <head>
        <meta charset="utf-8"/>
        <title>scalajs-react • TodoMVC</title>
      </head>
      <body>
      </body>
      <script src="todo-jsdeps.js"></script>
      <!--include application-->
      <script src="todo-opt.js"></script>
      <!--run application-->
      <script src="todo-launcher.js"></script>
    </html>
}