import Dependencies._
import CompilerOptions._

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "io.github.katlasik"
ThisBuild / organizationName := "katlasik"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version),
    buildInfoPackage := "io.tictactoe"
  )
  .configs(IntegrationTest)
  .settings(
    name := "tic-tac-toe",
    libraryDependencies ++= dependencies,
    Defaults.itSettings ++ Seq(parallelExecution := false),
    wartremoverErrors in (Compile, compile) ++= Warts
      .allBut(Wart.AnyVal, Wart.Any, Wart.Nothing, Wart.Product, Wart.Serializable, Wart.StringPlusAny, Wart.JavaSerializable),
    scalacOptions ++= compilerOptions,
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0")
  )
