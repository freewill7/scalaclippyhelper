import sbt._

object Dependencies {
  lazy val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.2"
  lazy val firebase = "com.google.firebase" % "firebase-admin" % "5.8.0"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.3"
}
