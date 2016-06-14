name := "scala-json"

version := "3.0.2"

organization := "com.twitter"

scalaVersion := "2.10.6"

crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0-M4")

scalacOptions += "-language:implicitConversions"

javacOptions ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 12 =>
      Seq("-source", "1.8", "-target", "1.8")
    case _ =>
      Seq("-source", "1.6", "-target", "1.6")
  }
}

javacOptions in doc := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 12 =>
      Seq("-source", "1.8")
    case _ =>
      Seq("-source", "1.6")
  }
}

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "junit" % "junit" % "4.10" % "test"
)

libraryDependencies ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2")
    case _ =>
      Seq.empty
  }
}

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("sonatype-snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("sonatype-releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { x => false }

pomExtra := (
  <url>https://github.com/twitter/scala-json</url>
  <licenses>
    <license>
      <name>Apache License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:twitter/scala-json.git</url>
    <connection>scm:git:git@github.com:twitter/scala-json.git</connection>
  </scm>
  <developers>
    <developer>
      <id>twitter</id>
      <name>Twitter Inc.</name>
      <url>https://www.twitter.com/</url>
    </developer>
  </developers>
)
