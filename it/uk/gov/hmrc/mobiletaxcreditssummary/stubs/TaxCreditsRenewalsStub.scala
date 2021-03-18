package uk.gov.hmrc.mobiletaxcreditssummary.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlPathEqualTo}
import uk.gov.hmrc.domain.Nino

object TaxCreditsRenewalsStub {

  def singleRenewalClaimIsFound(
    nino:          Nino,
    renewalStatus: String
  ): Unit =
    stubFor(
      get(urlPathEqualTo(s"/income/${nino.value}/tax-credits/full-claimant-details"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(renewalSingleClaimJson(nino, renewalStatus))
        )
    )

  def multipleRenewalClaimsAreFound(
    nino:                Nino,
    firstRenewalStatus:  String,
    secondRenewalStatus: String
  ): Unit =
    stubFor(
      get(urlPathEqualTo(s"/income/${nino.value}/tax-credits/full-claimant-details"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(renewalMultipleClaimsJson(nino, firstRenewalStatus, secondRenewalStatus))
        )
    )

  def noRenewalClaimsFound(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/income/${nino.value}/tax-credits/full-claimant-details"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json"))
    )

  def renewalsCallFails(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/income/${nino.value}/tax-credits/full-claimant-details"))
        .willReturn(aResponse().withStatus(502).withHeader("Content-Type", "application/json"))
    )

  def renewalSingleClaimJson(
    nino:          Nino,
    renewalStatus: String
  ): String =
    s"""
    {
      "references": [
        {
          "household": {
            "barcodeReference": "200000000000013",
            "applicationID": "198765432134567",
            "applicant1": {
              "nino": "$nino",
              "title": "MR",
              "firstForename": "JOHN",
              "secondForename": "",
              "surname": "DENSMORE"
            },
            "householdEndReason": ""
          },
          "renewal": {
            "awardStartDate": "12/10/2030",
            "awardEndDate": "12/10/2010",
            "renewalStatus": "$renewalStatus",
            "renewalNoticeIssuedDate": "12/10/2030",
            "renewalNoticeFirstSpecifiedDate": "12/10/2010",
            "renewalFormType": "D"
          }
        }
      ]
  } """

  def renewalMultipleClaimsJson(
    nino:                Nino,
    firstRenewalStatus:  String,
    secondRenewalStatus: String
  ): String =
    s"""
    {
      "references": [
        {
          "household": {
            "barcodeReference": "200000000000013",
            "applicationID": "198765432134567",
            "applicant1": {
              "nino": "$nino",
              "title": "MR",
              "firstForename": "JOHN",
              "secondForename": "",
              "surname": "DENSMORE"
            },
            "householdEndReason": "HOUSEHOLD BREAKDOWN"
          },
          "renewal": {
            "awardStartDate": "12/10/2030",
            "awardEndDate": "12/10/2010",
            "renewalStatus": "$firstRenewalStatus",
            "renewalNoticeIssuedDate": "12/10/2030",
            "renewalNoticeFirstSpecifiedDate": "12/10/2010",
            "renewalFormType": "D"
          }
        },
        {
          "household": {
            "barcodeReference": "2000000000000143",
            "applicationID": "198765432134567",
            "applicant1": {
              "nino": "$nino",
              "title": "MR",
              "firstForename": "JOHN",
              "secondForename": "",
              "surname": "DENSMORE"
            },
            "householdEndReason": ""
          },
          "renewal": {
            "awardStartDate": "12/10/2030",
            "awardEndDate": "12/10/2010",
            "renewalStatus": "$secondRenewalStatus",
            "renewalNoticeIssuedDate": "12/10/2030",
            "renewalNoticeFirstSpecifiedDate": "12/10/2010",
            "renewalFormType": "D"
          }
        }
      ]
  } """
}
