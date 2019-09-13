import play.core.PlayVersion
import play.sbt.PlayImport.PlayKeys.playDefaultPort
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.versioning.SbtGitVersioning

val appName: String = "mobile-tax-credits-summary"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    Seq(
      play.sbt.PlayScala,
      SbtAutoBuildPlugin,
      SbtGitVersioning,
      SbtDistributablesPlugin,
      SbtArtifactory,
      ScoverageSbtPlugin
    ): _*
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(publishingSettings: _*)
  .settings(
    routesImport ++= Seq(
      "uk.gov.hmrc.domain._",
      "uk.gov.hmrc.mobiletaxcreditssummary.binders.Binders._"
    )
  )
  .settings(
    majorVersion := 0,
    playDefaultPort := 8246,
    scalaVersion := "2.11.12",
    libraryDependencies ++= compile ++ test ++ integration,
    dependencyOverrides ++= jettyOverrides,
    evictionWarningOptions in update := EvictionWarningOptions.default
      .withWarnScalaVersionEviction(false),
    resolvers += Resolver.jcenterRepo,
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(
      base => Seq(base / "it")
    ).value,
    testGrouping in IntegrationTest := oneForkedJvmPerTest(
      (definedTests in IntegrationTest).value
    ),
    coverageMinimum := 90,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageExcludedPackages := "<empty>;.*Routes.*;app.*;.*prod;.*definition;.*testOnlyDoNotUseInAppConf;.*com.kenshoo.*;.*javascript.*;.*BuildInfo;.*Reverse.*;.*Base64.*"
  )

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
  tests map { test =>
    Group(
      test.name,
      Seq(test),
      SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name)))
    )
  }

val compile = Seq(
  "uk.gov.hmrc" %% "bootstrap-play-26" % "0.35.0",
  "uk.gov.hmrc" %% "play-hmrc-api"     % "3.4.0-play-26",
  "uk.gov.hmrc" %% "domain"            % "5.6.0-play-26"
)

val scalatestPlusPlayVersion = "3.1.2"

val test = Seq(
  "org.scalatest"          %% "scalatest"          % "3.0.5"                  % Test,
  "org.scalamock"          %% "scalamock"          % "4.1.0"                  % Test,
  "org.pegdown"            % "pegdown"             % "1.6.0"                  % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % scalatestPlusPlayVersion % Test
)

val integration = Seq(
  "com.typesafe.play"      %% "play-test"          % PlayVersion.current      % IntegrationTest,
  "info.cukes"             %% "cucumber-scala"     % "1.2.5"                  % IntegrationTest,
  "info.cukes"             % "cucumber-junit"      % "1.2.5"                  % IntegrationTest,
  "com.github.tomakehurst" % "wiremock"            % "2.20.0"                 % IntegrationTest,
  "org.scalatestplus.play" %% "scalatestplus-play" % scalatestPlusPlayVersion % IntegrationTest
)

// Transitive dependencies in scalatest/scalatestplusplay drag in a newer version of jetty that is not
// compatible with wiremock, so we need to pin the jetty stuff to the older version.
// see https://groups.google.com/forum/#!topic/play-framework/HAIM1ukUCnI
val jettyVersion = "9.2.13.v20150730"
val jettyOverrides: Set[ModuleID] = Set(
  "org.eclipse.jetty"           % "jetty-server"       % jettyVersion,
  "org.eclipse.jetty"           % "jetty-servlet"      % jettyVersion,
  "org.eclipse.jetty"           % "jetty-security"     % jettyVersion,
  "org.eclipse.jetty"           % "jetty-servlets"     % jettyVersion,
  "org.eclipse.jetty"           % "jetty-continuation" % jettyVersion,
  "org.eclipse.jetty"           % "jetty-webapp"       % jettyVersion,
  "org.eclipse.jetty"           % "jetty-xml"          % jettyVersion,
  "org.eclipse.jetty"           % "jetty-client"       % jettyVersion,
  "org.eclipse.jetty"           % "jetty-http"         % jettyVersion,
  "org.eclipse.jetty"           % "jetty-io"           % jettyVersion,
  "org.eclipse.jetty"           % "jetty-util"         % jettyVersion,
  "org.eclipse.jetty.websocket" % "websocket-api"      % jettyVersion,
  "org.eclipse.jetty.websocket" % "websocket-common"   % jettyVersion,
  "org.eclipse.jetty.websocket" % "websocket-client"   % jettyVersion
)
