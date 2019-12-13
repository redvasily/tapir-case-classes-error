import Dependencies._

ThisBuild / scalaVersion := "2.12.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

val tapirVersion = "0.7.6"

lazy val root = (project in file("."))
  .settings(
    name := "tapir-test",
    libraryDependencies ++= Seq(
      "com.softwaremill.tapir" %% "tapir-core" % tapirVersion,
      "com.softwaremill.tapir" %% "tapir-akka-http-server" % tapirVersion,
      "com.softwaremill.tapir" %% "tapir-json-circe" % tapirVersion,
      "com.softwaremill.tapir" %% "tapir-openapi-docs" % tapirVersion,
      "com.softwaremill.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion,
      "com.softwaremill.tapir" %% "tapir-sttp-client" % tapirVersion,
      "org.webjars" % "swagger-ui" % "3.22.0",
      scalaTest % Test
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
