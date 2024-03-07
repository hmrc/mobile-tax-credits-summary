import sbt.{ModuleID, _}

private object AppDependencies {

  private val bootstrapPlayVersion = "7.20.0"
  private val playHmrcApiVersion   = "7.2.0-play-28"
  private val domainVersion        = "8.1.0-play-28"

  private val pegdownVersion   = "1.6.0"
  private val refinedVersion   = "0.9.26"
  private val wireMockVersion  = "2.21.0"
  private val scalaMockVersion = "5.1.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "play-hmrc-api"             % playHmrcApiVersion,
    "uk.gov.hmrc" %% "domain"                    % domainVersion,
    "eu.timepit"  %% "refined"                   % refinedVersion
  )

  trait TestDependencies {
    lazy val scope: String        = "test"
    lazy val test:  Seq[ModuleID] = ???
  }

  private def testCommon(scope: String) = Seq("uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapPlayVersion % scope)

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "org.scalamock" %% "scalamock" % scalaMockVersion % scope,
            "org.pegdown"   % "pegdown"    % pegdownVersion   % scope
          )
      }.test
  }

  object IntegrationTest {

    def apply(): Seq[ModuleID] =
      new TestDependencies {
        override lazy val scope: String = "it"

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "com.github.tomakehurst" % "wiremock" % wireMockVersion % scope
          )
      }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
