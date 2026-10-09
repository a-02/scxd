val scala3Version = "3.9.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "scxd",
    version := "0.0.0",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalameta" %% "munit" % "1.3.6" % Test
  )
