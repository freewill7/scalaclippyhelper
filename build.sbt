import Dependencies._



lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      scalaVersion := "2.12.3",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Hello",
    libraryDependencies += jackson,
    libraryDependencies += firebase,
    libraryDependencies += scalaTest % Test
  )
