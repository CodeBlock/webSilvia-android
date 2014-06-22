import sbt._
import Keys._

import android.Keys._

object General {
  val settings = Defaults.defaultSettings ++ Seq (
    name := "webSilvia",
    version := "0.1",
    scalaVersion := "2.10.4",
    resolvers             ++= Seq(
      "sonatype-s" at "http://oss.sonatype.org/content/repositories/snapshots"
    ),
    libraryDependencies   ++= Seq(
      "org.scalaz" %% "scalaz-core" % "7.0.4",
      "org.scalaz" %% "scalaz-effect" % "7.0.4",
      "org.scalaz" %% "scalaz-concurrent" % "7.0.4",
      "com.google.zxing" % "android-integration" % "3.1.0",
      "com.github.nkzawa" % "socket.io-client" % "0.1.1"
    ),
    scalacOptions         := Seq(
      "-encoding", "utf8",
      "-target:jvm-1.6",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-optimise",
      "-Ywarn-value-discard"
    ),
    javacOptions          ++= Seq(
      "-encoding", "utf8",
      "-source", "1.6",
      "-target", "1.6"
    )
  )

  val proguardSettings = Seq (
    useProguard in Android := true,
    proguardOptions in Android += "-keep class scala.Function1",
    proguardOptions in Android += "-keep class scala.PartialFunction",
    proguardOptions in Android += "-keep class scala.util.parsing.combinator.Parsers",
    proguardOptions in Android += "-dontwarn javax.swing.SwingWorker",
    proguardOptions in Android += "-dontwarn javax.swing.SwingUtilities",

    proguardCache in Android += ProguardCache("scalaz") % "org.scalaz"
  )

  lazy val fullAndroidSettings =
    General.settings ++
    android.Plugin.androidBuild ++
    proguardSettings
}

object AndroidBuild extends Build {
  lazy val main = Project (
    "webSilvia",
    file("."),
    settings = General.fullAndroidSettings ++ Seq(
      platformTarget in Android := "android-15"
    )
  )

  lazy val tests = Project (
    "tests",
    file("tests"),
    settings = General.settings
  ) dependsOn main
}
