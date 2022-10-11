val scala3Version = "3.1.1"
val AkkaVersion = "2.6.19"

lazy val startupTransition: State => State = "writeHooks" :: _

lazy val root = project
  .in(file("."))
  .settings(
    name := "actor-smart-city",
    assembly / assemblyJarName := "actor-smart-city.jar",
    scalaVersion := scala3Version,
    Global / onLoad := {
      val old = (Global / onLoad).value
      startupTransition compose old
    },
    jacocoReportSettings := JacocoReportSettings(
      "Jacoco Coverage Report",
      None,
      JacocoThresholds(),
      Seq(JacocoReportFormats.ScalaHTML, JacocoReportFormats.XML),
      "utf-8"
    ),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.13" % Test,
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion, // For standard log configuration
      "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion, // akka clustering module
      "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    )
  )
