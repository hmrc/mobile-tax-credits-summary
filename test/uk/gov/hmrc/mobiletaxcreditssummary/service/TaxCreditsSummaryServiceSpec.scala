/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.mobiletaxcreditssummary.service

import java.time.LocalDate
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{Tag, TestData}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.json.Json.parse
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.mobiletaxcreditssummary.controllers.TestSetup
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.LegacyRenewalStatus.COMPLETE
import uk.gov.hmrc.mobiletaxcreditssummary.domain.{Complete, Renewals, TaxCreditsNino}
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._
import uk.gov.hmrc.mobiletaxcreditssummary.services.{InformationMessageService, LiveTaxCreditsSummaryService, ReportActualProfitService, TaxCreditsRenewalsService}
import uk.gov.hmrc.mobiletaxcreditssummary.utils.LocalDateProvider

import scala.concurrent.ExecutionContext.Implicits.global

class TaxCreditsSummaryServiceSpec
    extends TestSetup
    with FileResource
    with FutureAwaits
    with DefaultAwaitTimeout
    with GuiceOneAppPerTest {

  val currentYear: Int = LocalDate.now().getYear
  val lastYear:    Int = currentYear - 1
  val reportActualProfitPeriodStartDate = "2018-11-30T10:00:00.000Z"
  val reportActualProfitPeriodEndDate   = "2019-01-31T10:00:00.000Z"

  val exclusionPaymentSummary: PaymentSummary = PaymentSummary(None, None, None, None, excluded = Some(true))
  val taxCreditsNino:          TaxCreditsNino = TaxCreditsNino(nino)

  val upstream4xxException: Upstream4xxResponse = Upstream4xxResponse("blows up for excluded users", 405, 405)
  val upstream5xxException: Upstream5xxResponse = Upstream5xxResponse("blows up for excluded users", 500, 500)

  val taxCreditsSummary: TaxCreditsSummaryResponse =
    TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary(paymentSummary, Some(claimants))))

  def taxCreditsSummaryWithFtnae(
    link:         Option[MessageLink] = None,
    preSeptember: Boolean             = false,
    currentYear:  Boolean             = true,
    ftnae:        Boolean             = true,
    ctc:          Boolean             = true
  ): TaxCreditsSummaryResponse =
    TaxCreditsSummaryResponse(
      taxCreditsSummary =
        Some(TaxCreditsSummary(paymentSummaryFtnae(preSeptember, currentYear, ftnae, ctc), Some(claimantsFtnae(link))))
    )

  def taxCreditsSummaryWithMultipleFtnae(
    link:         Option[MessageLink] = None,
    preSeptember: Boolean             = false,
    currentYear:  Boolean             = true,
    ftnae:        Boolean             = true,
    ctc:          Boolean             = true
  ): TaxCreditsSummaryResponse =
    TaxCreditsSummaryResponse(
      taxCreditsSummary = Some(
        TaxCreditsSummary(paymentSummaryMultipleFtnae(preSeptember, currentYear, ftnae, ctc),
                          Some(claimantsMultipleFtnae(link)))
      )
    )

  def taxCreditsSummaryWithInfoMessage(
    specialCircumstance: Option[SpecialCircumstance] = None,
    informationMessage:  Option[InformationMessage] = None,
    messageLink:         Option[MessageLink]
  ): TaxCreditsSummaryResponse =
    TaxCreditsSummaryResponse(taxCreditsSummary = Some(
      TaxCreditsSummary(paymentSummaryWithInfoMessage(specialCircumstance, informationMessage),
                        Some(claimants.copy(messageLink = messageLink)))
    )
    )

  val taxCreditsSummaryNoPartnerDetails: TaxCreditsSummaryResponse =
    TaxCreditsSummaryResponse(taxCreditsSummary =
      Some(TaxCreditsSummary(paymentSummary, Some(claimantsNoPartnerDetails)))
    )

  val taxCreditsSummaryNoChildren: TaxCreditsSummaryResponse =
    TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary(paymentSummary, Some(claimantsNoChildren))))

  val taxCreditsSummaryNoClaimants: TaxCreditsSummaryResponse =
    TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary(paymentSummary, None)))

  val taxCreditsSummaryEmpty: TaxCreditsSummaryResponse = TaxCreditsSummaryResponse(taxCreditsSummary = None)

  def taxCreditsSummaryWithReportActualProfitLink(reportActualProfit: ReportActualProfit): TaxCreditsSummaryResponse =
    TaxCreditsSummaryResponse(taxCreditsSummary =
      Some(TaxCreditsSummary(paymentSummary, Some(claimantsWithReportActualProfit(reportActualProfit))))
    )

  val dashboardData: DashboardData =
    Json
      .fromJson[DashboardData](parse(findResource("/resources/taxcreditssummary/CS700100A-dashboard-data.json").get))
      .get

  val reportActualProfitService =
    new ReportActualProfitService(reportActualProfitPeriodStartDate, reportActualProfitPeriodEndDate)

  val taxCreditsRenewalsService = new TaxCreditsRenewalsService(mockTaxCreditsRenewalsConnector,
                                                                now.minusMonths(1).toString,
                                                                now.minusMonths(1).toString,
                                                                now.plusMonths(1).toString,
                                                                now.plusMonths(2).toString,
                                                                now.plusMonths(3).toString)

  "getTaxCreditsSummaryResponse" should {
    "return a non-tax-credits user payload when exclusion returns None" in {
      val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService = new InformationMessageService(localDateProvider)
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(None, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe TaxCreditsSummaryResponse(excluded =
                                                                                                              false,
                                                                                                            None)
    }

    "return a tax-credits user payload when a payment summary is returned" in {
      val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService = new InformationMessageService(localDateProvider)
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardData(
        dashboardData.copy(personalDetails,
                           Some(partnerDetails),
                           Children(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
                           paymentSummary),
        taxCreditsNino
      )
      mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummary
    }

    "return a tax-credits user payload with renewals when claims are returned" in {
      val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService = new InformationMessageService(localDateProvider)
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardData(
        dashboardData.copy(personalDetails,
                           Some(partnerDetails),
                           Children(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
                           paymentSummary),
        taxCreditsNino
      )
      mockTaxCreditsRenewalsConnectorSingleClaim(tcrNino, COMPLETE)

      val taxCreditsSummaryWithRenewals = taxCreditsSummary.taxCreditsSummary.get.copy(renewals = Some(
        Renewals(Complete, 1, 1, now.minusMonths(1).toString, now.plusMonths(1).toString, now.plusMonths(3).toString)
      )
      )
      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummary
        .copy(taxCreditsSummary = Some(taxCreditsSummaryWithRenewals))
    }

    "return a tax-credits user payload when a payment summary is returned but when there are no partner details" in {
      val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService = new InformationMessageService(localDateProvider)
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardData(
        dashboardData.copy(personalDetails,
                           None,
                           Children(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
                           paymentSummary),
        taxCreditsNino
      )
      mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummaryNoPartnerDetails
    }

    "return a tax-credits user payload when a payment summary is returned but when there are no children" in {
      val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService = new InformationMessageService(localDateProvider)
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardData(
        dashboardData.copy(personalDetails, Some(partnerDetails), Children(Seq.empty), paymentSummary),
        taxCreditsNino
      )
      mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummaryNoChildren
    }

    "return an excluded user payload when exclusion returns true" in {
      val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService = new InformationMessageService(localDateProvider)
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(true)), taxCreditsNino)
      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe TaxCreditsSummaryResponse(excluded =
                                                                                                              true,
                                                                                                            None)
    }

    "return an error when dashboard data fails and exclusion returns false" in {
      val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService = new InformationMessageService(localDateProvider)
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardDataFailure(upstream5xxException, taxCreditsNino)

      intercept[Upstream5xxResponse] {
        await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId))
      }
    }

    "return an error when exclusion errors" in {
      val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService = new InformationMessageService(localDateProvider)
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusionFailure(Upstream4xxResponse("blows up for excluded users", 400, 400),
                                                       taxCreditsNino)

      intercept[Upstream4xxResponse] {
        await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId))
      }
    }

    val scenarios = Table(
      ("testName", "child", "ftnae"),
      ("with FTNAE", SarahSmithFtnae, true),
      ("without FTNAE", SarahSmith, false)
    )

    forAll(scenarios) { (testName: String, child: Child, ftnae: Boolean) =>
      f"return a tax-credits user payload $testName but date is after 7th September ($currentYear-09-08)" taggedAs Tag(
        f"$currentYear-09-08"
      ) in {
        val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
        val informationMessageService = new InformationMessageService(localDateProvider)
        val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                       taxCreditsRenewalsService,
                                                       reportActualProfitService,
                                                       informationMessageService)
        mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetDashboardData(
          dashboardData.copy(
            personalDetails,
            Some(partnerDetails),
            Children(Seq(child, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
            paymentSummaryFtnae(preSeptember = false, ftnae = ftnae)
          ),
          taxCreditsNino
        )
        mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

        await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummaryWithFtnae(
          ftnae       = ftnae,
          currentYear = false
        )
      }

      f"return a tax-credits user payload $testName but date is after 31st August and before 8th September ($currentYear-09-01)" taggedAs Tag(
        f"$currentYear-09-01"
      ) in {
        val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
        val informationMessageService = new InformationMessageService(localDateProvider)
        val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                       taxCreditsRenewalsService,
                                                       reportActualProfitService,
                                                       informationMessageService)
        mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetDashboardData(
          dashboardData.copy(
            personalDetails,
            Some(partnerDetails),
            Children(Seq(child, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
            paymentSummaryFtnae(preSeptember = false, ftnae = ftnae)
          ),
          taxCreditsNino
        )
        mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

        await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe getExpected(
          testName,
          Some(
            MessageLink(preFtnaeDeadline = false,
                        "Update details",
                        "/tax-credits-service/children/add-child/who-do-you-want-to-add")
          ),
          ftnae,
          preSeptember = false
        )
      }
      f"return a tax-credits user payload $testName but date is after 31st August and before 8th September ($currentYear-09-07)" taggedAs Tag(
        f"$currentYear-09-07"
      ) in {
        val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
        val informationMessageService = new InformationMessageService(localDateProvider)
        val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                       taxCreditsRenewalsService,
                                                       reportActualProfitService,
                                                       informationMessageService)
        mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetDashboardData(
          dashboardData.copy(
            personalDetails,
            Some(partnerDetails),
            Children(Seq(child, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
            paymentSummaryFtnae(preSeptember = false, ftnae = ftnae)
          ),
          taxCreditsNino
        )
        mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

        await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe getExpected(
          testName,
          Some(
            MessageLink(preFtnaeDeadline = false,
                        "Update details",
                        "/tax-credits-service/children/add-child/who-do-you-want-to-add")
          ),
          ftnae,
          preSeptember = false
        )
      }

      f"return a tax-credits user payload $testName but date is after 31st August and before 8th September with no ctc ($currentYear-09-07)" taggedAs Tag(
        f"$currentYear-09-07"
      ) in {
        val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
        val informationMessageService = new InformationMessageService(localDateProvider)
        val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                       taxCreditsRenewalsService,
                                                       reportActualProfitService,
                                                       informationMessageService)
        mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetDashboardData(
          dashboardData.copy(
            personalDetails,
            Some(partnerDetails),
            Children(Seq(child, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
            paymentSummaryFtnae(preSeptember = false, ftnae = ftnae, ctc = false)
          ),
          taxCreditsNino
        )
        mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

        await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe getExpected(
          testName,
          Some(
            MessageLink(preFtnaeDeadline = false,
                        "Update details",
                        "/tax-credits-service/children/add-child/who-do-you-want-to-add")
          ),
          ftnae,
          preSeptember = false,
          ctc          = false
        )
      }

      f"return a tax-credits user payload $testName but date is after 8th September with no ctc ($currentYear-09-09)" taggedAs Tag(
        f"$currentYear-09-09"
      ) in {
        val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
        val informationMessageService = new InformationMessageService(localDateProvider)
        val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                       taxCreditsRenewalsService,
                                                       reportActualProfitService,
                                                       informationMessageService)
        mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetDashboardData(
          dashboardData.copy(
            personalDetails,
            Some(partnerDetails),
            Children(Seq(child, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
            paymentSummaryFtnae(preSeptember = false, ftnae = false, ctc = false)
          ),
          taxCreditsNino
        )
        mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

        await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe getExpected(testName,
                                                                                                None,
                                                                                                ftnae        = false,
                                                                                                preSeptember = false,
                                                                                                ctc          = false)
      }

      f"return a tax-credits user payload $testName but date is before 1st September ($currentYear-08-31)" taggedAs Tag(
        f"$currentYear-08-31"
      ) in {
        val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
        val informationMessageService = new InformationMessageService(localDateProvider)
        val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                       taxCreditsRenewalsService,
                                                       reportActualProfitService,
                                                       informationMessageService)
        mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetDashboardData(
          dashboardData.copy(
            personalDetails,
            Some(partnerDetails),
            Children(Seq(child, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
            paymentSummaryFtnae(preSeptember = true, ftnae = ftnae)
          ),
          taxCreditsNino
        )
        mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

        await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe getExpected(
          testName,
          Some(
            MessageLink(preFtnaeDeadline = true, "Update details", "/tax-credits-service/home/children-and-childcare")
          ),
          ftnae        = ftnae,
          preSeptember = true
        )
      }

      f"return a tax-credits user payload $testName but date is before 1st September but in PY ($lastYear-12-31)" taggedAs Tag(
        f"$lastYear-12-31"
      ) in {
        val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
        val informationMessageService = new InformationMessageService(localDateProvider)
        val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                       taxCreditsRenewalsService,
                                                       reportActualProfitService,
                                                       informationMessageService)
        mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetDashboardData(
          dashboardData.copy(
            personalDetails,
            Some(partnerDetails),
            Children(Seq(child, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
            paymentSummaryFtnae(preSeptember = false, currentYear = false, ftnae = ftnae)
          ),
          taxCreditsNino
        )
        mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

        await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummaryWithFtnae(
          currentYear = false,
          ftnae       = ftnae
        )
      }

    }

    f"return a tax-credits user payload but date is before 1st September but in PY ($lastYear-12-31) and there are multiple FTNAE children" taggedAs Tag(
      f"$lastYear-12-31"
    ) in {
      val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService = new InformationMessageService(localDateProvider)
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardData(
        dashboardData.copy(
          personalDetails,
          Some(partnerDetails),
          Children(Seq(SarahSmithFtnae, SarahSmithFtnae, SarahSmithFtnae, JennySmith, PeterSmith, SimonSmith)),
          paymentSummaryMultipleFtnae(preSeptember = false, currentYear = false)
        ),
        taxCreditsNino
      )
      mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummaryWithMultipleFtnae(currentYear =
        false
      )
    }

    "return the correct actual profit link during a valid period when both the applicant and partner have estimated their income" in {
      val localDateProvider               = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService       = new InformationMessageService(localDateProvider)
      val reportActualProfitPeriodEndDate = currentYear + "-12-31T23:59:59.000Z"
      val reportActualProfitService =
        new ReportActualProfitService(reportActualProfitPeriodStartDate, reportActualProfitPeriodEndDate)
      val reportActualProfit = ReportActualProfit(
        "/tax-credits-service/actual-profit",
        reportActualProfitPeriodEndDate,
        userMustReportIncome    = true,
        partnerMustReportIncome = true
      )
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardData(
        dashboardData.copy(
          personalDetails,
          Some(partnerDetails),
          Children(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
          paymentSummary,
          actualIncomeStatus = actualIncomeBothEligible
        ),
        taxCreditsNino
      )
      mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummaryWithReportActualProfitLink(
        reportActualProfit
      )
    }

    "return the correct actual profit link during a valid period when only the applicant has estimated their income" in {
      val localDateProvider               = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService       = new InformationMessageService(localDateProvider)
      val reportActualProfitPeriodEndDate = currentYear + "-12-31T23:59:59.000Z"
      val reportActualProfitService =
        new ReportActualProfitService(reportActualProfitPeriodStartDate, reportActualProfitPeriodEndDate)
      val reportActualProfit = ReportActualProfit(
        "/tax-credits-service/actual-self-employed-profit-or-loss",
        reportActualProfitPeriodEndDate,
        userMustReportIncome    = true,
        partnerMustReportIncome = false
      )
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardData(
        dashboardData.copy(
          personalDetails,
          Some(partnerDetails),
          Children(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
          paymentSummary,
          actualIncomeStatus = actualIncomeApp1Eligible
        ),
        taxCreditsNino
      )
      mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummaryWithReportActualProfitLink(
        reportActualProfit
      )
    }

    "return the correct actual profit link during a valid period when only the applicant's partner has estimated their income" in {
      val localDateProvider               = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService       = new InformationMessageService(localDateProvider)
      val reportActualProfitPeriodEndDate = currentYear + "-12-31T23:59:59.000Z"
      val reportActualProfitService =
        new ReportActualProfitService(reportActualProfitPeriodStartDate, reportActualProfitPeriodEndDate)
      val reportActualProfit = ReportActualProfit(
        "/tax-credits-service/actual-self-employed-profit-or-loss-partner",
        reportActualProfitPeriodEndDate,
        userMustReportIncome    = false,
        partnerMustReportIncome = true
      )
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardData(
        dashboardData.copy(
          personalDetails,
          Some(partnerDetails),
          Children(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
          paymentSummary,
          actualIncomeStatus = actualIncomeApp2Eligible
        ),
        taxCreditsNino
      )
      mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummaryWithReportActualProfitLink(
        reportActualProfit
      )
    }

    "return the correct actual profit link during a valid period when the logged in user is not the main applicant and their partner has estimated their income" in {
      val localDateProvider               = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService       = new InformationMessageService(localDateProvider)
      val reportActualProfitPeriodEndDate = currentYear + "-12-31T23:59:59.000Z"
      val reportActualProfitService =
        new ReportActualProfitService(reportActualProfitPeriodStartDate, reportActualProfitPeriodEndDate)
      val reportActualProfit = ReportActualProfit(
        "/tax-credits-service/actual-self-employed-profit-or-loss-partner",
        reportActualProfitPeriodEndDate,
        userMustReportIncome    = false,
        partnerMustReportIncome = true
      )
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardData(
        dashboardData.copy(
          personalDetails,
          Some(partnerDetails),
          Children(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
          paymentSummary,
          actualIncomeStatus = actualIncomeApp1Eligible,
          awardDetails       = dashboardData.awardDetails.copy(mainApplicantNino = TaxCreditsNino(incorrectNino.value))
        ),
        taxCreditsNino
      )
      mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummaryWithReportActualProfitLink(
        reportActualProfit
      )
    }

    "return the correct actual profit link during a valid period when the logged in user is not the main applicant, but they have estimated their income" in {
      val localDateProvider               = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService       = new InformationMessageService(localDateProvider)
      val reportActualProfitPeriodEndDate = currentYear + "-12-31T23:59:59.000Z"
      val reportActualProfitService =
        new ReportActualProfitService(reportActualProfitPeriodStartDate, reportActualProfitPeriodEndDate)
      val reportActualProfit = ReportActualProfit(
        "/tax-credits-service/actual-self-employed-profit-or-loss",
        reportActualProfitPeriodEndDate,
        userMustReportIncome    = true,
        partnerMustReportIncome = false
      )
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardData(
        dashboardData.copy(
          personalDetails,
          Some(partnerDetails),
          Children(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
          paymentSummary,
          actualIncomeStatus = actualIncomeApp2Eligible,
          awardDetails       = dashboardData.awardDetails.copy(mainApplicantNino = TaxCreditsNino(incorrectNino.value))
        ),
        taxCreditsNino
      )
      mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummaryWithReportActualProfitLink(
        reportActualProfit
      )
    }

    "return no actual profit link during a valid period when both the applicant and partner have not estimated their income" in {
      val localDateProvider               = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService       = new InformationMessageService(localDateProvider)
      val reportActualProfitPeriodEndDate = currentYear + "-12-31T23:59:59.000Z"
      val reportActualProfitService =
        new ReportActualProfitService(reportActualProfitPeriodStartDate, reportActualProfitPeriodEndDate)
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardData(
        dashboardData.copy(
          personalDetails,
          Some(partnerDetails),
          Children(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
          paymentSummary,
          actualIncomeStatus = actualIncomeNeitherEligible
        ),
        taxCreditsNino
      )
      mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummary
    }

    "return no actual profit link during a valid period when one applicant has estimated their income but the other is excluded" in {
      val localDateProvider               = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService       = new InformationMessageService(localDateProvider)
      val reportActualProfitPeriodEndDate = currentYear + "-12-31T23:59:59.000Z"
      val reportActualProfitService =
        new ReportActualProfitService(reportActualProfitPeriodStartDate, reportActualProfitPeriodEndDate)
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardData(
        dashboardData.copy(
          personalDetails,
          Some(partnerDetails),
          Children(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
          paymentSummary,
          actualIncomeStatus = actualIncomeOneExcluded
        ),
        taxCreditsNino
      )
      mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummary
    }

    "return a tax-credits user payload with information message and correct link  when OLD RATE special circumstance is returned" in {
      val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService = new InformationMessageService(localDateProvider)
      val informationMessage = Some(
        InformationMessage(
          "Tax credit payment amounts increased on 6 April",
          "You should only contact HMRC if you have not received your revised payment by 18 May."
        )
      )
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardData(
        dashboardData.copy(
          personalDetails,
          Some(partnerDetails),
          Children(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
          paymentSummaryWithInfoMessage(Some(OldRate), informationMessage)
        ),
        taxCreditsNino
      )
      mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummaryWithInfoMessage(
        Some(OldRate),
        informationMessage,
        Some(
          MessageLink(false,
                      "Contact tax credits",
                      "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/tax-credits-enquiries")
        )
      )
    }

    "return a tax-credits user payload with information message and correct link when NEW RATE special circumstance is returned" in {
      val localDateProvider         = app.injector.instanceOf[LocalDateProvider]
      val informationMessageService = new InformationMessageService(localDateProvider)
      val informationMessage = Some(
        InformationMessage(
          "Tax credit payment amounts increased on 6 April",
          "Your payments have been revised. You should only contact HMRC if there is a problem with your revised payments."
        )
      )
      val service = new LiveTaxCreditsSummaryService(mockTaxCreditsBrokerConnector,
                                                     taxCreditsRenewalsService,
                                                     reportActualProfitService,
                                                     informationMessageService)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetDashboardData(
        dashboardData.copy(
          personalDetails,
          Some(partnerDetails),
          Children(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith)),
          paymentSummaryWithInfoMessage(Some(NewRate), informationMessage)
        ),
        taxCreditsNino
      )
      mockTaxCreditsRenewalsConnectorNoClaims(tcrNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino), journeyId)) shouldBe taxCreditsSummaryWithInfoMessage(
        Some(NewRate),
        informationMessage,
        Some(
          MessageLink(false,
                      "Contact tax credits",
                      "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/tax-credits-enquiries")
        )
      )
    }

  }

  def getExpected(
    testName:     String,
    link:         Option[MessageLink],
    ftnae:        Boolean,
    preSeptember: Boolean,
    ctc:          Boolean = true
  ): TaxCreditsSummaryResponse =
    if (testName.equals("with FTNAE")) {
      taxCreditsSummaryWithFtnae(preSeptember = preSeptember, link = link, ftnae = ftnae, ctc = ctc)
    } else if (testName.equals("without FTNAE")) {
      taxCreditsSummaryWithFtnae(preSeptember = preSeptember, ftnae = ftnae, ctc = ctc)
    } else {
      throw new IllegalArgumentException("Invalid test name - check tests")
    }

  override def newAppForTest(testData: TestData): Application =
    testData.tags.headOption match {
      case Some(tag) =>
        GuiceApplicationBuilder()
          .configure("dateOverride" -> tag)
          .disable[com.kenshoo.play.metrics.PlayModule]
          .configure("metrics.enabled" -> false)
          .build()
      case _ =>
        GuiceApplicationBuilder()
          .configure("dateOverride" -> LocalDate.now().toString)
          .disable[com.kenshoo.play.metrics.PlayModule]
          .configure("metrics.enabled" -> false)
          .build()
    }
}
