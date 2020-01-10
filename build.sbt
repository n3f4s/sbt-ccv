lazy val root = (project in file("."))
  .settings(
    name := "sbt-ccv",
    version := "1.0",
    organization := "com.github.n3f4s",
    sbtPlugin := true,
  )
