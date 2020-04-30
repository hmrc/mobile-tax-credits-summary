package uk.gov.hmrc.mobiletaxcreditssummary.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object ShutteringStub {

  def stubForShutteringDisabled: StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/mobile-shuttering/service/mobile-tax-credits-summary/shuttered-status"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "shuttered": false,
                         |  "title":     "",
                         |  "message":    ""
                         |}
          """.stripMargin)
        )
    )

  def stubForShutteringEnabled: StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/mobile-shuttering/service/mobile-tax-credits-summary/shuttered-status"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "shuttered": true,
                         |  "title":     "Shuttered",
                         |  "message":   "Tax Credits Summary is currently not available"
                         |}
          """.stripMargin)
        )
    )

}
