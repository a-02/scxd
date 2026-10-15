val scala3Version = "3.9.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "scxd",
    version := "0.0.0",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.3.6" % Test,
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %% "cats-effect" % "3.7.1",
      "com.monovore" %% "decline-effect" % "2.6.1",
      "com.lihaoyi" %% "fansi" % "0.5.1",

    )
  )
