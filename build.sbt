import play.sbt.PlayImport.PlayKeys.playDefaultPort

val appName: String = "mobile-tax-credits-summary"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    Seq(
      play.sbt.PlayScala,
      SbtAutoBuildPlugin,
      SbtDistributablesPlugin,
      ScoverageSbtPlugin
    ): _*
  )
  .disablePlugins(JUnitXmlReportPlugin)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    routesImport ++= Seq(
      "uk.gov.hmrc.domain._",
      "uk.gov.hmrc.mobiletaxcreditssummary.binders.Binders._",
      "uk.gov.hmrc.mobiletaxcreditssummary.domain.types._",
      "uk.gov.hmrc.mobiletaxcreditssummary.domain.types.ModelTypes._"
    )
  )
  .settings(
    majorVersion := 0,
    playDefaultPort := 8246,
    scalaVersion := "2.13.12",
    libraryDependencies ++= AppDependencies(),
    update / evictionWarningOptions := EvictionWarningOptions.default
      .withWarnScalaVersionEviction(false),
    resolvers += Resolver.jcenterRepo,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory) (
      base => Seq(base / "it")
    ).value,
    coverageMinimumStmtTotal := 91,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageExcludedPackages := "<empty>;.*Routes.*;app.*;.*prod;.*definition;.*testOnlyDoNotUseInAppConf;.*com.kenshoo.*;.*javascript.*;.*BuildInfo;.*Reverse.*;.*Base64.*;.*binders.*"
  )

