import AssemblyKeys._

seq(assemblySettings: _*)

name := "scala-storm-starter"

version := "0.0.2-SNAPSHOT"

scalaVersion := "2.9.2"

fork in run := true

net.virtualvoid.sbt.graph.Plugin.graphSettings

resolvers ++= Seq(
  "twitter4j" at "http://twitter4j.org/maven2",
  "clojars.org" at "http://clojars.org/repo"
)

libraryDependencies ++= Seq(
  "storm" % "storm" % "0.8.2" % "provided",
  "junit" % "junit" % "4.10" % "test",
  "org.slf4j" % "slf4j-log4j12" % "1.6.4",
  "org.slf4j" % "slf4j-api" % "1.6.4" intransitive,
  "com.netflix.astyanax" % "astyanax" % "1.56.37",
  "org.mockito" % "mockito-all" % "1.9.5" % "test",
  "com.rabbitmq" % "amqp-client" % "3.1.1",
  "org.clojure" % "clojure" % "1.4.0" % "provided",
  "org.twitter4j" % "twitter4j-core" % "2.2.6-SNAPSHOT",
  "org.twitter4j" % "twitter4j-stream" % "2.2.6-SNAPSHOT",
  "org.specs2" %% "specs2" % "1.11" % "test",
  "javax.servlet" % "servlet-api" % "2.5" intransitive
)

// mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
//   {
//     case PathList("javax", "servlet", "servlet-api", xs @ _*) => MergeStrategy.last
//     case x => old(x)
//   }
// }

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter { f => 
    (f.data.getName == "log4j-over-slf4j.jar" || f.data.getName == "servlet-api-2.5-20081211.jar")
  }
}

mainClass in Compile := Some("storm.starter.topology.ExclamationTopology")

mainClass in assembly := Some("storm.starter.topology.ExclamationTopology")

TaskKey[File]("generate-storm") <<= (baseDirectory, fullClasspath in Compile, mainClass in Compile) map { (base, cp, main) =>
  val template = """#!/bin/sh
storm classpath "%s"
storm jar target/target/scala-2.9.2/scala-storm-starter_2.9.2-0.0.2-SNAPSHOT.jar %s firesuit
"""
  /*"""#!/bin/sh
java -classpath "%s" %s "$@"
"""*/
  val mainStr = main getOrElse error("No main class specified")
  val contents = template.format(cp.files.absString, mainStr)
  val out = base / "bin/run-main-topology.sh"
  IO.write(out, contents)
  out.setExecutable(true)
  out
}
