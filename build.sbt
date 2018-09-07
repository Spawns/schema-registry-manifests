lazy val commonSettings = Seq(
  name := "schema-registry",
  version := "0.1",
  organization := "com.sumup",
  scalaVersion := "2.12.4"
)

lazy val akkaVersion = "2.5.6"
lazy val akkaHttpVersion = "10.0.10"

lazy val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
)

lazy val otherDependencies = Seq(
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.1.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.gnieh" %% "diffson-spray-json" % "2.2.4",

  // NOTE: `cors` is required for Swagger
  "ch.megard" %% "akka-http-cors" % "0.2.2",
  "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.11.1",
  "io.swagger" %% "swagger-scala-module" % "1.0.4"
)

lazy val testDependencies = Seq(
  "org.scalactic" %% "scalactic" % "3.0.4" % "it,test",
  "org.scalatest" %% "scalatest" % "3.0.4" % "it,test",
  "org.mockito" % "mockito-core" % "2.13.0" % "it,test",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.10" % "it,test",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.9" % "it,test"
)

libraryDependencies ++= akkaDependencies
libraryDependencies ++= otherDependencies
libraryDependencies ++= testDependencies

// Assembly settings
assemblyMergeStrategy in assembly := {
  case "reference.conf" => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

mainClass in assembly := Some("com.sumup.ApplicationMain")
test in assembly := {}
assemblyJarName in assembly := "schema-registry.jar"

val CustomIntegrationTest: Configuration = config("it") extend Test

def isIntegrationTest(str: String): Boolean = {
  str.endsWith("IntegrationTests")
}

def isUnitTest(str: String): Boolean = {
  str.startsWith("com.sumup.unit")
}

// NOTE: Disable parallel execution since it's problematic when using `Mockito`.
parallelExecution in Test := false
parallelExecution in CustomIntegrationTest := false
// NOTE: Enable full stack traces for tests
testOptions in Test += Tests.Argument("-oF")
testOptions in CustomIntegrationTest += Tests.Argument("-oF")

lazy val root = (project in file("."))
  .configs(CustomIntegrationTest)
  .settings(
    commonSettings,
    inConfig(CustomIntegrationTest)(Defaults.testTasks),
    testOptions in Test := Seq(Tests.Filter(isUnitTest)),
    testOptions in CustomIntegrationTest := Seq(Tests.Filter(isIntegrationTest))
  )