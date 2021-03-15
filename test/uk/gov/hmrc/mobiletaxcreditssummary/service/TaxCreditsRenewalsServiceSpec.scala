package uk.gov.hmrc.mobiletaxcreditssummary.service

import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.mobiletaxcreditssummary.controllers.TestSetup
import uk.gov.hmrc.mobiletaxcreditssummary.services.TaxCreditsRenewalsService

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.mobiletaxcreditssummary.domain.{AutoRenewal, AutoRenewalMultiple, Complete, NotStartedMultiple, NotStartedSingle, PackNotSent, RenewalStatus, RenewalSubmitted, Renewals, ViewOnly}

class TaxCreditsRenewalsServiceSpec
    extends TestSetup
    with FileResource
    with FutureAwaits
    with DefaultAwaitTimeout
    with GuiceOneAppPerTest {

  val serviceOpen = new TaxCreditsRenewalsService(mockTaxCreditsRenewalsConnector,
                                                  now.minusMonths(1).toString,
                                                  now.minusMonths(1).toString,
                                                  now.plusMonths(1).toString,
                                                  now.plusMonths(2).toString,
                                                  now.plusMonths(3).toString)

  val viewOnlyRenewals: Renewals =
    Renewals(ViewOnly, 1, 1, now.minusMonths(3).toString, now.minusMonths(2).toString, now.plusMonths(1).toString)

  def defaultClaimRenewals(
    status:          RenewalStatus,
    totalClaims:     Int = 1,
    claimsSubmitted: Int = 0
  ): Renewals =
    Renewals(status,
             totalClaims,
             claimsSubmitted,
             now.minusMonths(1).toString,
             now.plusMonths(1).toString,
             now.plusMonths(3).toString)

  "getTaxCreditsRenewals" should {
    "return the ViewOnly response correctly" in {
      val serviceViewOnly = new TaxCreditsRenewalsService(mockTaxCreditsRenewalsConnector,
                                                          now.minusMonths(3).toString,
                                                          now.minusMonths(3).toString,
                                                          now.minusMonths(2).toString,
                                                          now.minusMonths(1).toString,
                                                          now.plusMonths(1).toString)
      mockTaxCreditsRenewalsConnectorSingleClaim(tcrNino, "COMPLETE")
      await(serviceViewOnly.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(viewOnlyRenewals)

    }

    "return the PackNotSent response correctly" in {
      mockTaxCreditsRenewalsConnectorSingleClaim(tcrNino, "AWAITING_BARCODE")
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(PackNotSent)
      )
    }

    "return the AutoRenewal response correctly" in {
      mockTaxCreditsRenewalsConnectorSingleClaim(tcrNino, "NOT_SUBMITTED", autoRenewal = true)
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(AutoRenewal)
      )
    }

    "return the NotStartedSingle response correctly" in {
      mockTaxCreditsRenewalsConnectorSingleClaim(tcrNino, "NOT_SUBMITTED")
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(NotStartedSingle)
      )
    }

    "return the RenewalSubmitted response correctly for a single claim" in {
      mockTaxCreditsRenewalsConnectorSingleClaim(tcrNino, "SUBMITTED_AND_PROCESSING")
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(RenewalSubmitted, 1, 1)
      )
    }

    "return the Complete response correctly for a single claim" in {
      mockTaxCreditsRenewalsConnectorSingleClaim(tcrNino, "COMPLETE")
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(Complete, 1, 1)
      )
    }

    "return the RenewalSubmitted response correctly for a multiple claims" in {
      mockTaxCreditsRenewalsConnectorMultipleClaims(tcrNino,
                                                    Seq(singleClaim("SUBMITTED_AND_PROCESSING"),
                                                        singleClaim("SUBMITTED_AND_PROCESSING")))
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(RenewalSubmitted, totalClaims = 2, claimsSubmitted = 2)
      )
    }

    "return the Complete response correctly for a multiple claims" in {
      mockTaxCreditsRenewalsConnectorMultipleClaims(tcrNino, Seq(singleClaim("COMPLETE"), singleClaim("COMPLETE")))
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(Complete, totalClaims = 2, claimsSubmitted = 2)
      )
    }

    "return the AutoRenewalMultiple response correctly" in {
      mockTaxCreditsRenewalsConnectorMultipleClaims(tcrNino,
                                                    Seq(singleClaim("NOT_SUBMITTED", autoRenewal = true),
                                                        singleClaim("NOT_SUBMITTED", autoRenewal = true)))
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(AutoRenewalMultiple, totalClaims = 2)
      )
    }

    "return the NotSTartedMultiple response correctly" in {
      mockTaxCreditsRenewalsConnectorMultipleClaims(tcrNino,
                                                    Seq(singleClaim("NOT_SUBMITTED"), singleClaim("NOT_SUBMITTED")))
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(NotStartedMultiple, totalClaims = 2)
      )
    }

  }
}
