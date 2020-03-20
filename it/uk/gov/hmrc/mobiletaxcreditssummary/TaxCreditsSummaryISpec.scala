/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.mobiletaxcreditssummary

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.Shuttering
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.{InformationMessage, TaxCreditsSummaryResponse}
import uk.gov.hmrc.mobiletaxcreditssummary.stubs.AuthStub.grantAccess
import uk.gov.hmrc.mobiletaxcreditssummary.stubs.ShutteringStub._
import uk.gov.hmrc.mobiletaxcreditssummary.stubs.TaxCreditsBrokerStub._
import uk.gov.hmrc.mobiletaxcreditssummary.support.BaseISpec
import uk.gov.hmrc.time.DateTimeUtils

class TaxCreditsSummaryISpec extends BaseISpec with FileResource {

  protected val now: DateTime = DateTimeUtils.now.withZone(UTC)

  protected def reportActualProfitStartDate: String = now.toString

  protected def reportActualProfitEndDate: String = now.plusDays(1).toString

  override def configuration: Map[String, Any] =
    super.configuration ++
    Map(
      "microservice.reportActualProfitPeriod.startDate" -> reportActualProfitStartDate,
      "microservice.reportActualProfitPeriod.endDate"   -> reportActualProfitEndDate,
      "microservice.featureFlag.covid"                  -> false
    )

  "GET /income/:nino/tax-credits/tax-credits-summary " should {
    def request(nino: Nino): WSRequest =
      wsUrl(s"/income/${nino.value}/tax-credits/tax-credits-summary?journeyId=17d2420c-4fc6-4eee-9311-a37325066704")
        .addHttpHeaders(acceptJsonHeader)

    "return a valid response for TAX-CREDITS-USER - check more details on github.com/hmrc/mobile-tax-credits-summary" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String]                                                                          shouldBe "WEEKLY"
      ((response.json \\ "claimants").head \ "personalDetails" \ "forename").as[String]      shouldBe "Nuala"
      ((response.json \\ "claimants").head \ "personalDetails" \ "surname").as[String]       shouldBe "O'Shea"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "forename").as[String]       shouldBe "Frederick"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "otherForenames").as[String] shouldBe "Tarquin"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "surname").as[String]        shouldBe "Hunter-Smith"
      (((response.json \\ "claimants").head \ "children")(0) \ "forename").as[String]        shouldBe "Sarah"
      (((response.json \\ "claimants").head \ "children")(0) \ "surname").as[String]         shouldBe "Smith"
    }

    "return a valid response for TAX-CREDITS-USER with report actual profit if applicable" in {
      grantAccess(sandboxNino.value)
      childrenAreFound(sandboxNino)
      partnerDetailsAreFound(sandboxNino, nino2)
      paymntSummaryIsFound(sandboxNino)
      personalDetailsAreFound(sandboxNino)
      exclusionFlagIsFound(sandboxNino, excluded = false)
      dashboardDataIsFound(sandboxNino)

      val response = await(request(sandboxNino).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String]                                                                          shouldBe "WEEKLY"
      ((response.json \\ "claimants").head \ "personalDetails" \ "forename").as[String]      shouldBe "Nuala"
      ((response.json \\ "claimants").head \ "personalDetails" \ "surname").as[String]       shouldBe "O'Shea"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "forename").as[String]       shouldBe "Frederick"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "otherForenames").as[String] shouldBe "Tarquin"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "surname").as[String]        shouldBe "Hunter-Smith"
      (((response.json \\ "claimants").head \ "children")(0) \ "forename").as[String]        shouldBe "Sarah"
      (((response.json \\ "claimants").head \ "children")(0) \ "surname").as[String]         shouldBe "Smith"
      (((response.json \\ "claimants").head \ "reportActualProfit") \ "link")
        .as[String] shouldBe "/tax-credits-service/actual-self-employed-profit-or-loss"
    }

    "return a valid response for TAX-CREDITS-USER with no report actual profit if an error is thrown when calling broker" in {
      grantAccess(sandboxNino.value)
      childrenAreFound(sandboxNino)
      partnerDetailsAreFound(sandboxNino, nino2)
      paymntSummaryIsFound(sandboxNino)
      personalDetailsAreFound(sandboxNino)
      exclusionFlagIsFound(sandboxNino, excluded = false)
      dashboardDataIsNotFound(sandboxNino)

      val response = await(request(sandboxNino).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String]                                                                          shouldBe "WEEKLY"
      ((response.json \\ "claimants").head \ "personalDetails" \ "forename").as[String]      shouldBe "Nuala"
      ((response.json \\ "claimants").head \ "personalDetails" \ "surname").as[String]       shouldBe "O'Shea"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "forename").as[String]       shouldBe "Frederick"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "otherForenames").as[String] shouldBe "Tarquin"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "surname").as[String]        shouldBe "Hunter-Smith"
      (((response.json \\ "claimants").head \ "children")(0) \ "forename").as[String]        shouldBe "Sarah"
      (((response.json \\ "claimants").head \ "children")(0) \ "surname").as[String]         shouldBe "Smith"
      ((response.json \\ "claimants").head \ "reportActualProfit").isEmpty
    }

    "return a valid response for EXCLUDED-TAX-CREDITS-USER" in {
      grantAccess(nino1.value)
      exclusionFlagIsFound(nino1, excluded = true)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe true
    }

    "return a valid response for NON-TAX-CREDITS-USER" in {
      exclusionFlagIsNotFound(nino1)
      grantAccess(nino1.value)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
    }

    "return a valid response for EXCLUDED USER" in {
      grantAccess(nino1.value)
      paymntSummary500(nino1)
      exclusionFlagIsFound(nino1, excluded = true)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe true
    }

    "return a valid response for ERROR-500 - tcs/:nino/exclusion call returns 500" in {
      grantAccess(nino1.value)
      exclusion500(nino1)

      val response = await(request(nino1).get())
      response.status shouldBe 500
    }

    "return a valid response for ERROR-503 - tcs/:nino/paymentSummary call returns 503" in {
      grantAccess(nino1.value)
      paymntSummary503(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status shouldBe 500
    }

    "return a valid response - tcs/:nino/paymentSummary call returns 404" in {
      grantAccess(nino1.value)
      paymentSummary404(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
    }

    "return a valid response for ERROR-503 - tcs/:nino/exclusion call returns 503" in {
      grantAccess(nino1.value)
      exclusion503(nino1)

      val response = await(request(nino1).get())
      response.status shouldBe 500
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/personal-details call returns 404" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreNotFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String]                          shouldBe "WEEKLY"
      (response.json \\ "claimants").isEmpty shouldBe true
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/personal-details call returns 500" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetails500(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String]                          shouldBe "WEEKLY"
      (response.json \\ "claimants").isEmpty shouldBe true
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/personal-details call returns 503" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetails503(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String]                          shouldBe "WEEKLY"
      (response.json \\ "claimants").isEmpty shouldBe true
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/partner-details call returns 404" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetailsAreNotFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                                                                   shouldBe 200
      (response.json \ "excluded").as[Boolean]                                          shouldBe false
      ((response.json \\ "claimants").head \ "personalDetails" \ "forename").as[String] shouldBe "Nuala"
      ((response.json \\ "claimants").head \ "personalDetails" \ "surname").as[String]  shouldBe "O'Shea"
      (response.json \\ "partnerDetails").isEmpty                                       shouldBe true
      (((response.json \\ "claimants").head \ "children")(0) \ "forename").as[String]   shouldBe "Sarah"
      (((response.json \\ "claimants").head \ "children")(0) \ "surname").as[String]    shouldBe "Smith"
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/partner-details call returns 500" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetails500(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \\ "claimants").isEmpty   shouldBe true
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/partner-details call returns 503" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetails503(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \\ "claimants").isEmpty   shouldBe true
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/children call returns OK with no children" in {
      grantAccess(nino1.value)
      childrenAreNotFound(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                                                                   shouldBe 200
      (response.json \ "excluded").as[Boolean]                                          shouldBe false
      (response.json \\ "claimants").isEmpty                                            shouldBe false
      ((response.json \\ "claimants").head \ "personalDetails" \ "forename").as[String] shouldBe "Nuala"
      ((response.json \\ "claimants").head \ "personalDetails" \ "surname").as[String]  shouldBe "O'Shea"
      (response.json \\ "children").isEmpty                                             shouldBe false
      (response.json \\ "children").head.asInstanceOf[JsArray].value.isEmpty
      ((response.json \\ "claimants").head \ "partnerDetails" \ "forename").as[String]       shouldBe "Frederick"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "otherForenames").as[String] shouldBe "Tarquin"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "surname").as[String]        shouldBe "Hunter-Smith"
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/children call returns 500" in {
      grantAccess(nino1.value)
      children500(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \\ "claimants").isEmpty   shouldBe true
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/children call returns 503" in {
      grantAccess(nino1.value)
      children503(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \\ "claimants").isEmpty   shouldBe true
    }

    "return 400 if journeyId not supplied" in {
      val response = await(wsUrl(s"/income/$nino1/tax-credits/tax-credits-summary").get())
      response.status shouldBe 400
    }

    "return 400 if journeyId is invalid" in {
      val response =
        await(wsUrl(s"/income/$nino1/tax-credits/tax-credits-summary?journeyId=XXXXXXXXXXXXXXXXXXXX").get())
      response.status shouldBe 400
    }

    "return SHUTTERED when shuttered" in {
      stubForShutteringEnabled
      grantAccess(nino1.value)

      val response = await(request(nino1).get())
      response.status shouldBe 521
      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe Some("Shuttered")
      shuttering.message   shouldBe Some("Tax Credits Summary is currently not available")
    }
  }
}

class TaxCreditsSummaryCovidISpec extends BaseISpec with FileResource {

  protected val now: DateTime = DateTimeUtils.now.withZone(UTC)

  protected def reportActualProfitStartDate: String = now.toString

  protected def reportActualProfitEndDate: String = now.plusDays(1).toString

  override def configuration: Map[String, Any] =
    super.configuration ++
    Map(
      "microservice.reportActualProfitPeriod.startDate" -> reportActualProfitStartDate,
      "microservice.reportActualProfitPeriod.endDate"   -> reportActualProfitEndDate,
      "microservice.featureFlag.covid"                  -> true
    )

  "GET /income/:nino/tax-credits/tax-credits-summary " should {
    def request(nino: Nino): WSRequest =
      wsUrl(s"/income/${nino.value}/tax-credits/tax-credits-summary?journeyId=17d2420c-4fc6-4eee-9311-a37325066704")
        .addHttpHeaders(acceptJsonHeader)

    "return a valid response for TAX-CREDITS-USER - COVID" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status shouldBe 200
      val taxCreditsSummaryResponse = (response.json.as[TaxCreditsSummaryResponse])
      val paymentSummary            = taxCreditsSummaryResponse.taxCreditsSummary.get.paymentSummary

      paymentSummary.specialCircumstances shouldBe Some("COVID")
      paymentSummary.informationMessage   shouldBe Some(InformationMessage("COVID", "COVID Body"))

      taxCreditsSummaryResponse.excluded                   shouldBe false
      paymentSummary.workingTaxCredit.get.paymentFrequency shouldBe "WEEKLY"

      ((response.json \\ "claimants").head \ "personalDetails" \ "forename").as[String]      shouldBe "Nuala"
      ((response.json \\ "claimants").head \ "personalDetails" \ "surname").as[String]       shouldBe "O'Shea"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "forename").as[String]       shouldBe "Frederick"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "otherForenames").as[String] shouldBe "Tarquin"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "surname").as[String]        shouldBe "Hunter-Smith"
      (((response.json \\ "claimants").head \ "children")(0) \ "forename").as[String]        shouldBe "Sarah"
      (((response.json \\ "claimants").head \ "children")(0) \ "surname").as[String]         shouldBe "Smith"
    }
  }
}
