version := "0.1"

val Http4sVersion = "0.20.0"
val CirceVersion = "0.11.1"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"
val Log4CatsVersion = "0.3.0"


lazy val `dropbox-loader` = project.in(file("."))
  .settings(commonSettings)
  .settings(
    name := "loader"
  )

lazy val commonSettings = Seq(
  organization := "com.sigevsky",
  licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
  scalacOptions += "-Ypartial-unification",
  scalacOptions += "-language:higherKinds",

  scalaVersion := "2.12.8",

  addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.6"),
  addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.2.4"),

  libraryDependencies ++= Seq(
    "io.circe"                    %% "circe-parser"               % CirceVersion,
    "io.circe"                    %% "circe-generic"              % CirceVersion,
    "org.http4s"                  %% "http4s-dsl"                 % Http4sVersion,
    "org.http4s"                  %% "http4s-circe"               % Http4sVersion,
    "org.http4s"                  %% "http4s-blaze-server"        % Http4sVersion,
    "org.http4s"                  %% "http4s-blaze-client"        % Http4sVersion,
    "ch.qos.logback"              %  "logback-classic"            % LogbackVersion,
    "io.chrisdavenport"           %% "log4cats-core"              % Log4CatsVersion,  // Only if you want to Support Any Backend
    "io.chrisdavenport"           %% "log4cats-slf4j"             % Log4CatsVersion,  // Direct Slf4j Support - Recommended
  ),
)