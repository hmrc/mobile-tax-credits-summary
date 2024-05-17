import sbt.{ModuleID, _}

private object AppDependencies {

  private val bootstrapPlayVersion = "8.5.0"
  private val playHmrcApiVersion   = "8.0.0"
  private val domainVersion        = "9.0.0"

  private val refinedVersion   = "0.11.1"
  private val scalaMockVersion = "5.1.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "play-hmrc-api-play-30"     % playHmrcApiVersion,
    "uk.gov.hmrc" %% "domain-play-30"            % domainVersion,
    "eu.timepit"  %% "refined"                   % refinedVersion
  )

  trait TestDependencies {
    lazy val scope: String        = "test"
    lazy val test:  Seq[ModuleID] = ???
  }

  private def testCommon(scope: String) = Seq("uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion % scope)

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "org.scalamock" %% "scalamock" % scalaMockVersion % scope,
          )
      }.test
  }

  object IntegrationTest {

    def apply(): Seq[ModuleID] =
      new TestDependencies {
        override lazy val scope: String = "it"

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
          )
      }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
