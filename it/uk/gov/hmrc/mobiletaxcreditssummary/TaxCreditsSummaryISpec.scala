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

import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.Shuttering
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.LegacyRenewalStatus.{COMPLETE, NOT_SUBMITTED, SUBMITTED_AND_PROCESSING}
import uk.gov.hmrc.mobiletaxcreditssummary.stubs.AuthStub.grantAccess
import uk.gov.hmrc.mobiletaxcreditssummary.stubs.ShutteringStub._
import uk.gov.hmrc.mobiletaxcreditssummary.stubs.TaxCreditsBrokerStub._
import uk.gov.hmrc.mobiletaxcreditssummary.stubs.TaxCreditsRenewalsStub._
import uk.gov.hmrc.mobiletaxcreditssummary.support.BaseISpec

import java.time.{LocalDate, LocalDateTime}

class TaxCreditsSummaryISpec extends BaseISpec with FileResource {

  protected val now: LocalDateTime = LocalDateTime.now

  protected val reportActualProfitStartDate: String = now.toString

  protected val reportActualProfitEndDate: String = now.plusDays(1).toString

  override def configuration: Map[String, Any] =
    super.configuration ++
    Map(
      "microservice.reportActualProfitPeriod.startDate" -> reportActualProfitStartDate,
      "microservice.reportActualProfitPeriod.endDate"   -> reportActualProfitEndDate
    )

  "GET /income/:nino/tax-credits/tax-credits-summary " should {
    def request(nino: Nino): WSRequest =
      wsUrl(s"/income/${nino.value}/tax-credits/tax-credits-summary?journeyId=17d2420c-4fc6-4eee-9311-a37325066704")
        .addHttpHeaders(acceptJsonHeader)

    "return a valid response for TAX-CREDITS-USER - check more details on github.com/hmrc/mobile-tax-credits-summary" in {
      grantAccess(nino1.value)
      dashboardDataIsFound(nino1, nino2)
      exclusionFlagIsFound(nino1, excluded = false)
      noRenewalClaimsFound(nino1)
      stubForShutteringDisabled

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
      (response.json \\ "renewals").isEmpty                                                  shouldBe true
      (response.json \ "changeOfCircumstanceLinks" \ "changePersonalDetails")
        .as[String] shouldBe "/tax-credits-service/home/your-details"
      (response.json \ "changeOfCircumstanceLinks" \ "changeJobsOrIncome")
        .as[String] shouldBe "/tax-credits-service/home/jobs-and-income"
      (response.json \ "changeOfCircumstanceLinks" \ "addEditChildrenChildcare")
        .as[String] shouldBe "/tax-credits-service/home/children-and-childcare"
      (response.json \ "changeOfCircumstanceLinks" \ "otherChanges")
        .as[String] shouldBe "/tax-credits-service/home/other-changes"
    }

    "return a valid response for TAX-CREDITS-USER with report actual profit if applicable" in {
      grantAccess(sandboxNino.value)
      exclusionFlagIsFound(sandboxNino, excluded = false)
      dashboardDataIsFound(sandboxNino, nino2)
      stubForShutteringDisabled

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
      (((response.json \\ "claimants").head \ "reportActualProfit") \ "endDate")
        .as[String] shouldBe reportActualProfitEndDate + "Z"
      (((response.json \\ "claimants").head \ "reportActualProfit") \ "userMustReportIncome")
        .as[Boolean] shouldBe true
      (((response.json \\ "claimants").head \ "reportActualProfit") \ "partnerMustReportIncome")
        .as[Boolean] shouldBe false
    }

    "return a valid response for TAX-CREDITS-USER with Old Rate special circumstance" in {
      grantAccess(nino1.value)
      dashboardDataIsFound(nino1, nino2, "OLD RATE")
      exclusionFlagIsFound(nino1, excluded = false)
      stubForShutteringDisabled

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String] shouldBe "WEEKLY"
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "specialCircumstances")
        .as[String] shouldBe "OLD RATE"
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "informationMessage" \ "title")
        .as[String] shouldBe "Tax credit payment amounts increased on 6 April"
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "informationMessage" \ "message")
        .as[String]                                                                 shouldBe "You should only contact HMRC if you have not received your revised payment by 18 May."
      ((response.json \\ "claimants").head \ "messageLink" \ "linkName").as[String] shouldBe "Contact tax credits"
      ((response.json \\ "claimants").head \ "messageLink" \ "link")
        .as[String]                                                                          shouldBe "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/tax-credits-enquiries"
      ((response.json \\ "claimants").head \ "personalDetails" \ "forename").as[String]      shouldBe "Nuala"
      ((response.json \\ "claimants").head \ "personalDetails" \ "surname").as[String]       shouldBe "O'Shea"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "forename").as[String]       shouldBe "Frederick"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "otherForenames").as[String] shouldBe "Tarquin"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "surname").as[String]        shouldBe "Hunter-Smith"
      (((response.json \\ "claimants").head \ "children")(0) \ "forename").as[String]        shouldBe "Sarah"
      (((response.json \\ "claimants").head \ "children")(0) \ "surname").as[String]         shouldBe "Smith"
    }

    "return a valid response for TAX-CREDITS-USER with New Rate special circumstance" in {
      grantAccess(nino1.value)
      dashboardDataIsFound(nino1, nino2, "NEW RATE")
      exclusionFlagIsFound(nino1, excluded = false)
      stubForShutteringDisabled

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String] shouldBe "WEEKLY"
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "specialCircumstances")
        .as[String] shouldBe "NEW RATE"
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "informationMessage" \ "title")
        .as[String] shouldBe "Tax credit payment amounts increased on 6 April"
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "informationMessage" \ "message")
        .as[String]                                                                 shouldBe "Your payments have been revised. You should only contact HMRC if there is a problem with your revised payments."
      ((response.json \\ "claimants").head \ "messageLink" \ "linkName").as[String] shouldBe "Contact tax credits"
      ((response.json \\ "claimants").head \ "messageLink" \ "link")
        .as[String]                                                                          shouldBe "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/tax-credits-enquiries"
      ((response.json \\ "claimants").head \ "personalDetails" \ "forename").as[String]      shouldBe "Nuala"
      ((response.json \\ "claimants").head \ "personalDetails" \ "surname").as[String]       shouldBe "O'Shea"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "forename").as[String]       shouldBe "Frederick"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "otherForenames").as[String] shouldBe "Tarquin"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "surname").as[String]        shouldBe "Hunter-Smith"
      (((response.json \\ "claimants").head \ "children")(0) \ "forename").as[String]        shouldBe "Sarah"
      (((response.json \\ "claimants").head \ "children")(0) \ "surname").as[String]         shouldBe "Smith"
    }

    "return a valid response for TAX-CREDITS-USER with PXP5 special circumstance" in {
      grantAccess(nino1.value)
      dashboardDataIsFound(nino1, nino2, "PXP5")
      exclusionFlagIsFound(nino1, excluded = false)
      stubForShutteringDisabled

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String] shouldBe "WEEKLY"
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "specialCircumstances")
        .as[String] shouldBe "PXP5"
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "informationMessage" \ "title")
        .as[String] shouldBe "Your payments are being processed"
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "informationMessage" \ "message")
        .as[String]                                                                          shouldBe "It can take up to 2 days for your payments to show."
      ((response.json \\ "claimants").head \ "messageLink").isEmpty                          shouldBe true
      ((response.json \\ "claimants").head \ "personalDetails" \ "forename").as[String]      shouldBe "Nuala"
      ((response.json \\ "claimants").head \ "personalDetails" \ "surname").as[String]       shouldBe "O'Shea"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "forename").as[String]       shouldBe "Frederick"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "otherForenames").as[String] shouldBe "Tarquin"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "surname").as[String]        shouldBe "Hunter-Smith"
      (((response.json \\ "claimants").head \ "children")(0) \ "forename").as[String]        shouldBe "Sarah"
      (((response.json \\ "claimants").head \ "children")(0) \ "surname").as[String]         shouldBe "Smith"
    }

    "return a valid response for TAX-CREDITS-USER with unknown special circumstance" in {
      grantAccess(nino1.value)
      dashboardDataIsFound(nino1, nino2, "UNKNOWN CIRCUMSTANCE")
      exclusionFlagIsFound(nino1, excluded = false)
      stubForShutteringDisabled

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String]                                                                             shouldBe "WEEKLY"
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "specialCircumstances").isEmpty shouldBe true
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "informationMessage").isEmpty   shouldBe true
      ((response.json \\ "claimants").head \ "messageLink").isEmpty                             shouldBe true
      ((response.json \\ "claimants").head \ "personalDetails" \ "forename").as[String]         shouldBe "Nuala"
      ((response.json \\ "claimants").head \ "personalDetails" \ "surname").as[String]          shouldBe "O'Shea"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "forename").as[String]          shouldBe "Frederick"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "otherForenames").as[String]    shouldBe "Tarquin"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "surname").as[String]           shouldBe "Hunter-Smith"
      (((response.json \\ "claimants").head \ "children")(0) \ "forename").as[String]           shouldBe "Sarah"
      (((response.json \\ "claimants").head \ "children")(0) \ "surname").as[String]            shouldBe "Smith"
    }

    "return a valid response for TAX-CREDITS-USER with renewals info if applicable" in {
      grantAccess(sandboxNino.value)
      exclusionFlagIsFound(sandboxNino, excluded = false)
      dashboardDataIsFound(sandboxNino, nino2)
      singleRenewalClaimIsFound(sandboxNino, COMPLETE)
      stubForShutteringDisabled

      val response = await(request(sandboxNino).get())
      response.status                                                                shouldBe 200
      (response.json \ "excluded").as[Boolean]                                       shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary").isDefined             shouldBe true
      (response.json \ "taxCreditsSummary" \ "claimants").isDefined                  shouldBe true
      (response.json \ "taxCreditsSummary" \ "renewals").isDefined                   shouldBe true
      (response.json \ "taxCreditsSummary" \ "renewals" \ "status").as[String]       shouldBe "complete"
      (response.json \ "taxCreditsSummary" \ "renewals" \ "totalClaims").as[Int]     shouldBe 1
      (response.json \ "taxCreditsSummary" \ "renewals" \ "claimsSubmitted").as[Int] shouldBe 1
      (response.json \ "taxCreditsSummary" \ "renewals" \ "packReceivedDate")
        .as[String] shouldBe LocalDate.now().minusMonths(1).atStartOfDay().toString
      (response.json \ "taxCreditsSummary" \ "renewals" \ "renewalsEndDate")
        .as[String] shouldBe LocalDate.now().plusMonths(1).atStartOfDay().toString
      (response.json \ "taxCreditsSummary" \ "renewals" \ "viewRenewalsEndDate")
        .as[String]                                                                         shouldBe LocalDate.now().plusMonths(3).atStartOfDay().toString
      (response.json \ "taxCreditsSummary" \ "renewals" \ "householdBreakdown").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "renewals" \ "inGracePeriod").as[Boolean]      shouldBe false
      (response.json \ "taxCreditsSummary" \ "renewals" \ "currentYear")
        .as[String] shouldBe LocalDate.now().getYear.toString
    }

    "return correct response for TAX-CREDITS-USER with multiple claim renewals info if applicable" in {
      grantAccess(sandboxNino.value)
      exclusionFlagIsFound(sandboxNino, excluded = false)
      dashboardDataIsFound(sandboxNino, nino2)
      multipleRenewalClaimsAreFound(sandboxNino, SUBMITTED_AND_PROCESSING, NOT_SUBMITTED)
      stubForShutteringDisabled

      val response = await(request(sandboxNino).get())
      response.status                                                                shouldBe 200
      (response.json \ "excluded").as[Boolean]                                       shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary").isDefined             shouldBe true
      (response.json \ "taxCreditsSummary" \ "claimants").isDefined                  shouldBe true
      (response.json \ "taxCreditsSummary" \ "renewals").isDefined                   shouldBe true
      (response.json \ "taxCreditsSummary" \ "renewals" \ "status").as[String]       shouldBe "one_not_started_multiple"
      (response.json \ "taxCreditsSummary" \ "renewals" \ "totalClaims").as[Int]     shouldBe 2
      (response.json \ "taxCreditsSummary" \ "renewals" \ "claimsSubmitted").as[Int] shouldBe 1
      (response.json \ "taxCreditsSummary" \ "renewals" \ "packReceivedDate")
        .as[String] shouldBe LocalDate.now().minusMonths(1).atStartOfDay().toString
      (response.json \ "taxCreditsSummary" \ "renewals" \ "renewalsEndDate")
        .as[String] shouldBe LocalDate.now().plusMonths(1).atStartOfDay().toString
      (response.json \ "taxCreditsSummary" \ "renewals" \ "viewRenewalsEndDate")
        .as[String]                                                                         shouldBe LocalDate.now().plusMonths(3).atStartOfDay().toString
      (response.json \ "taxCreditsSummary" \ "renewals" \ "householdBreakdown").as[Boolean] shouldBe true
      (response.json \ "taxCreditsSummary" \ "renewals" \ "inGracePeriod").as[Boolean]      shouldBe false
      (response.json \ "taxCreditsSummary" \ "renewals" \ "currentYear")
        .as[String] shouldBe LocalDate.now().getYear.toString
    }

    "return a valid response for TAX-CREDITS-USER - with no renewals info if call to tax-credits-renewals fails" in {
      grantAccess(nino1.value)
      dashboardDataIsFound(nino1, nino2)
      exclusionFlagIsFound(nino1, excluded = false)
      renewalsCallFails(nino1)
      stubForShutteringDisabled

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
      (response.json \\ "renewals").isEmpty                                                  shouldBe true
    }

    "return a valid response for EXCLUDED-TAX-CREDITS-USER" in {
      grantAccess(nino1.value)
      exclusionFlagIsFound(nino1, excluded = true)
      stubForShutteringDisabled

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe true
    }

    "return a valid response for NON-TAX-CREDITS-USER" in {
      exclusionFlagIsNotFound(nino1)
      grantAccess(nino1.value)
      stubForShutteringDisabled

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
    }

    "return a valid response for ERROR-500 - tcs/:nino/exclusion call returns 500" in {
      grantAccess(nino1.value)
      exclusion500(nino1)

      val response = await(request(nino1).get())
      response.status shouldBe 500
    }

    "return a valid response - tcs/:nino/dashboardData call returns 404" in {
      grantAccess(nino1.value)
      dashboardDataIsNotFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)
      stubForShutteringDisabled

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
    }

    "return a valid response for ERROR-503 - tcs/:nino/exclusion call returns 503" in {
      grantAccess(nino1.value)
      exclusion503(nino1)
      stubForShutteringDisabled

      val response = await(request(nino1).get())
      response.status shouldBe 500
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
