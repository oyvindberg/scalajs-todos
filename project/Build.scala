import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.cross.CrossProject
import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin.Revolver

object Build extends Build {
  import ScalaJSPlugin.{autoImport ⇒ sjs}
  import sjs.{JSModuleIDBuilder, toScalaJSGroupID}

  object versions{
    val scalaJsReact = "0.9.1"
    val scalaCss     = "0.3.0"
    val unfiltered   = "0.8.4"
  }

  val sharedDeps = Def.setting(Seq(
    "com.lihaoyi"                       %%% "upickle"       % "0.2.8",
    "com.lihaoyi"                       %%% "autowire"      % "0.2.5"
  ))

  val todos = CrossProject("todo", file("todo-shared"), sjs.CrossType.Full)
    .settings(
      organization                   := "com.olvind",
      version                        := "1-SNAPSHOT",
      licenses                       += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0")),
      scalaVersion                   := "2.11.6",
      scalacOptions                 ++= Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-unchecked",
                                            "-Xfatal-warnings", "-Xlint", "-Yno-adapted-args", "-Ywarn-dead-code",
                                            "-Xfuture", "-Ywarn-unused-import"),
      updateOptions                  := updateOptions.value.withCachedResolution(true),
      libraryDependencies           ++= sharedDeps.value
  )

  val todosJs = todos.js.in(file("todo-js")).settings(
    libraryDependencies ++= sharedDeps.value ++ Seq(
      "com.github.japgolly.scalajs-react" %%% "ext-scalaz71"  % versions.scalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "extra"         % versions.scalaJsReact,
      "com.github.japgolly.scalacss"      %%% "core"          % versions.scalaCss,
      "com.github.japgolly.scalacss"      %%% "ext-react"     % versions.scalaCss
    ),
    sjs.emitSourceMaps := false,

    /* create javascript launcher. Searches for an object extends JSApp */
    sjs.persistLauncher := true,

    /* javascript dependencies */
    sjs.jsDependencies += "org.webjars" % "react" % "0.12.1" /
      "react-with-addons.js" commonJSName "React" minified "react-with-addons.min.js",

    artifactPath in (Compile, sjs.fastOptJS) :=
      ((crossTarget in (Compile, sjs.fastOptJS)).value / ((moduleName in sjs.fastOptJS).value + "-opt.js"))
  )

  val todosJvm = todos.jvm.in(file("todo-jvm")).settings(
    libraryDependencies ++= sharedDeps.value ++ Seq(
      "net.databinder" %% "unfiltered-netty-server" % versions.unfiltered,
      "net.databinder" %% "unfiltered-filter-async" % versions.unfiltered
    ),
    mainClass := Some("todomvc.TodoServer"),
    Revolver.settings,
    Revolver.reStart <<= Revolver.reStart dependsOn (sjs.fastOptJS in(todosJs, Compile))
  )

  val root = project.in(file(".")).
    aggregate(todosJs, todosJvm).
    settings(
      publish := {},
      publishLocal := {}
    )
}

