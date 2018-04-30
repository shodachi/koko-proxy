lazy val akkaHttpVersion = "10.1.1"
lazy val akkaVersion    = "2.5.12"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.shodachi",
      scalaVersion := "2.12.5",
      name := "koko-proxy"
    )),
    name := "TestProject",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
      "com.typesafe.akka" %% "akka-http-jackson"    % akkaHttpVersion,

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "junit"              % "junit"             % "4.12"          % Test,
      "com.novocode"       % "junit-interface"   % "0.10"          % Test
    ),

    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
  )
