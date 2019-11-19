/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Inject
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{Tag, TestData}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.{Application, Configuration}
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.mobiletaxcreditssummary.connectors.TaxCreditsBrokerConnector
import uk.gov.hmrc.mobiletaxcreditssummary.controllers.TestSetup
import uk.gov.hmrc.mobiletaxcreditssummary.domain.TaxCreditsNino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._
import uk.gov.hmrc.mobiletaxcreditssummary.services.LiveTaxCreditsSummaryService
import uk.gov.hmrc.mobiletaxcreditssummary.utils.LocalDateProvider
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global

class TaxCreditsSummaryServiceSpec extends TestSetup with FileResource with FutureAwaits with DefaultAwaitTimeout with GuiceOneAppPerTest {
  implicit val taxCreditsBrokerConnector: TaxCreditsBrokerConnector = mock[TaxCreditsBrokerConnector]
  implicit val auditConnector:            AuditConnector            = mock[AuditConnector]
  val configuration:                      Configuration             = mock[Configuration]

  val currentYear: Int = LocalDate.now().getYear
  val lastYear:    Int = currentYear - 1

  val exclusionPaymentSummary = PaymentSummary(None, None, None, None, excluded = Some(true))
  val taxCreditsNino          = TaxCreditsNino(nino)

  val upstream4xxException = Upstream4xxResponse("blows up for excluded users", 405, 405)
  val upstream5xxException = Upstream5xxResponse("blows up for excluded users", 500, 500)

  val taxCreditsSummary =
    TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary(paymentSummary, Some(claimants))))

  def taxCreditsSummaryWithFtnae(
    link:         Option[FtnaeLink] = None,
    preSeptember: Boolean           = false,
    currentYear:  Boolean           = true,
    ftnae:        Boolean           = true,
    ctc:          Boolean           = true) =
    TaxCreditsSummaryResponse(
      taxCreditsSummary = Some(TaxCreditsSummary(paymentSummaryFtnae(preSeptember, currentYear, ftnae, ctc), Some(claimantsFtnae(link)))))

  def taxCreditsSummaryWithMultipleFtnae(
    link:         Option[FtnaeLink] = None,
    preSeptember: Boolean           = false,
    currentYear:  Boolean           = true,
    ftnae:        Boolean           = true,
    ctc:          Boolean           = true) =
    TaxCreditsSummaryResponse(
      taxCreditsSummary =
        Some(TaxCreditsSummary(paymentSummaryMultipleFtnae(preSeptember, currentYear, ftnae, ctc), Some(claimantsMultipleFtnae(link)))))

  val taxCreditsSummaryNoPartnerDetails =
    TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary(paymentSummary, Some(claimantsNoPartnerDetails))))

  val taxCreditsSummaryNoChildren =
    TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary(paymentSummary, Some(claimantsNoChildren))))

  val taxCreditsSummaryNoClaimants =
    TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary(paymentSummary, None)))

  val taxCreditsSummaryEmpty = TaxCreditsSummaryResponse(taxCreditsSummary = None)

  "getTaxCreditsSummaryResponse" should {
    "return a non-tax-credits user payload when exclusion returns None" in {
      val localDateProvider = app.injector.instanceOf[LocalDateProvider]
      val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
      mockTaxCreditsBrokerConnectorGetExclusion(None, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe TaxCreditsSummaryResponse(excluded = false, None)
    }

    "return a tax-credits user payload when a payment summary is returned" in {
      val localDateProvider = app.injector.instanceOf[LocalDateProvider]
      val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentSummary(Some(paymentSummary), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetChildren(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummary
    }

    "return a tax-credits user payload when a payment summary is returned but when there are no partner details" in {
      val localDateProvider = app.injector.instanceOf[LocalDateProvider]
      val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentSummary(Some(paymentSummary), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetChildren(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPartnerDetails(None, taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummaryNoPartnerDetails
    }

    "return a tax-credits user payload when a payment summary is returned but when there are no children" in {
      val localDateProvider = app.injector.instanceOf[LocalDateProvider]
      val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentSummary(Some(paymentSummary), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetChildren(Seq.empty, taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummaryNoChildren
    }

    "return an excluded user payload when exclusion returns true" in {
      val localDateProvider = app.injector.instanceOf[LocalDateProvider]
      val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(true)), taxCreditsNino)
      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe TaxCreditsSummaryResponse(excluded = true, None)
    }

    "return TaxCreditsSummaryResponse with payment summary but empty claimants when Get Children fails" in {
      val localDateProvider = app.injector.instanceOf[LocalDateProvider]
      val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentSummary(Some(paymentSummary), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetChildrenFailure(upstream5xxException, taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummaryNoClaimants
    }

    "return TaxCreditsSummaryResponse with payment summary but empty claimants when Get Personal Details fails" in {
      val localDateProvider = app.injector.instanceOf[LocalDateProvider]
      val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentSummary(Some(paymentSummary), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetChildren(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPersonalDetailsFailure(upstream4xxException, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummaryNoClaimants
    }

    "return TaxCreditsSummaryResponse with payment summary but empty claimants when Get Partner Details fails" in {
      val localDateProvider = app.injector.instanceOf[LocalDateProvider]
      val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentSummary(Some(paymentSummary), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetChildren(Seq(SarahSmith, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPartnerDetailsFailure(upstream5xxException, taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummaryNoClaimants
    }

    "return an error when payment summary fails and exclusion returns false" in {
      val localDateProvider = app.injector.instanceOf[LocalDateProvider]
      val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentFailure(upstream5xxException, taxCreditsNino)

      intercept[Upstream5xxResponse] {
        await(service.getTaxCreditsSummaryResponse(Nino(nino)))
      }
    }

    "return an error when exclusion errors" in {
      val localDateProvider = app.injector.instanceOf[LocalDateProvider]
      val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
      mockTaxCreditsBrokerConnectorGetExclusionFailure(Upstream4xxResponse("blows up for excluded users", 400, 400), taxCreditsNino)

      intercept[Upstream4xxResponse] {
        await(service.getTaxCreditsSummaryResponse(Nino(nino)))
      }
    }

    val scenarios = Table(
      ("testName", "child", "ftnae"),
      ("with FTNAE", SarahSmithFtnae, true),
      ("without FTNAE", SarahSmith, false)
    )

    forAll(scenarios) { (testName: String, child: Child, ftnae: Boolean) =>
      f"return a tax-credits user payload $testName but date is after 7th September ($currentYear-09-08)" taggedAs Tag(f"$currentYear-09-08") in {
        val localDateProvider = app.injector.instanceOf[LocalDateProvider]
        val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
        mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPaymentSummary(Some(paymentSummaryFtnae(preSeptember = false, ftnae = ftnae)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetChildren(Seq(child, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

        await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummaryWithFtnae(ftnae = ftnae, currentYear = false)
      }

      f"return a tax-credits user payload $testName but date is after 31st August and before 8th September ($currentYear-09-01)" taggedAs Tag(
        f"$currentYear-09-01") in {
        val localDateProvider = app.injector.instanceOf[LocalDateProvider]
        val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
        mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPaymentSummary(Some(paymentSummaryFtnae(preSeptember = false, ftnae = ftnae)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetChildren(Seq(child, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

        await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe getExpected(
          testName,
          Some(FtnaeLink(preFtnaeDeadline = false, "/tax-credits-service/children/add-child/who-do-you-want-to-add")),
          ftnae,
          preSeptember = false)
      }
      f"return a tax-credits user payload $testName but date is after 31st August and before 8th September ($currentYear-09-07)" taggedAs Tag(
        f"$currentYear-09-07") in {
        val localDateProvider = app.injector.instanceOf[LocalDateProvider]
        val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
        mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPaymentSummary(Some(paymentSummaryFtnae(preSeptember = false, ftnae = ftnae)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetChildren(Seq(child, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

        await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe getExpected(
          testName,
          Some(FtnaeLink(preFtnaeDeadline = false, "/tax-credits-service/children/add-child/who-do-you-want-to-add")),
          ftnae,
          preSeptember = false)
      }

      f"return a tax-credits user payload $testName but date is after 31st August and before 8th September with no ctc ($currentYear-09-07)" taggedAs Tag(
        f"$currentYear-09-07") in {
        val localDateProvider = app.injector.instanceOf[LocalDateProvider]
        val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
        mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPaymentSummary(Some(paymentSummaryFtnae(preSeptember = false, ftnae = ftnae, ctc = false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetChildren(Seq(child, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

        await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe getExpected(
          testName,
          Some(FtnaeLink(preFtnaeDeadline = false, "/tax-credits-service/children/add-child/who-do-you-want-to-add")),
          ftnae,
          preSeptember = false,
          ctc = false)
      }

      f"return a tax-credits user payload $testName but date is after 8th September with no ctc ($currentYear-09-09)" taggedAs Tag(
        f"$currentYear-09-09") in {
        val localDateProvider = app.injector.instanceOf[LocalDateProvider]
        val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
        mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPaymentSummary(Some(paymentSummaryFtnae(preSeptember = false, ftnae = false, ctc = false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetChildren(Seq(child, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

        await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe getExpected(
          testName,
          None,
          ftnae = false,
          preSeptember = false,
          ctc = false)
      }

      f"return a tax-credits user payload $testName but date is before 1st September ($currentYear-08-31)" taggedAs Tag(f"$currentYear-08-31") in {
        val localDateProvider = app.injector.instanceOf[LocalDateProvider]
        val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
        mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPaymentSummary(Some(paymentSummaryFtnae(preSeptember = true, ftnae = ftnae)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetChildren(Seq(child, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)

        await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe getExpected(
          testName,
          Some(FtnaeLink(preFtnaeDeadline = true, "/tax-credits-service/home/children-and-childcare")),
          ftnae        = ftnae,
          preSeptember = true)
      }

      f"return a tax-credits user payload $testName but date is before 1st September but in PY ($lastYear-12-31)" taggedAs Tag(f"$lastYear-12-31") in {
        val localDateProvider = app.injector.instanceOf[LocalDateProvider]
        val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
        mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPaymentSummary(Some(paymentSummaryFtnae(preSeptember = false, currentYear = false, ftnae = ftnae)), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetChildren(Seq(child, JosephSmith, MarySmith, JennySmith, PeterSmith, SimonSmith), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
        mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)
        await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummaryWithFtnae(currentYear = false, ftnae = ftnae)
      }

    }
    f"return a tax-credits user payload but date is before 1st September but in PY ($lastYear-12-31) and there are multiple FTNAE children" taggedAs Tag(
      f"$lastYear-12-31") in {
      val localDateProvider = app.injector.instanceOf[LocalDateProvider]
      val service           = new LiveTaxCreditsSummaryService(taxCreditsBrokerConnector, localDateProvider)
      mockTaxCreditsBrokerConnectorGetExclusion(Some(Exclusion(false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPaymentSummary(Some(paymentSummaryMultipleFtnae(preSeptember = false, currentYear = false)), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetChildren(
        Seq(SarahSmithFtnae, SarahSmithFtnae, SarahSmithFtnae, JennySmith, PeterSmith, SimonSmith),
        taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPartnerDetails(Some(partnerDetails), taxCreditsNino)
      mockTaxCreditsBrokerConnectorGetPersonalDetails(personalDetails, taxCreditsNino)
      await(service.getTaxCreditsSummaryResponse(Nino(nino))) shouldBe taxCreditsSummaryWithMultipleFtnae(currentYear = false)
    }

  }

  def getExpected(testName: String, link: Option[FtnaeLink], ftnae: Boolean, preSeptember: Boolean, ctc: Boolean = true): TaxCreditsSummaryResponse =
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
