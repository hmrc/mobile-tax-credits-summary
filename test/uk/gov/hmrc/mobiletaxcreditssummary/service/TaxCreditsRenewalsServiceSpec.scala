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

import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.mobiletaxcreditssummary.controllers.TestSetup
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.LegacyRenewalStatus.{AWAITING_BARCODE, COMPLETE, NOT_SUBMITTED, SUBMITTED_AND_PROCESSING}
import uk.gov.hmrc.mobiletaxcreditssummary.services.TaxCreditsRenewalsService

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.mobiletaxcreditssummary.domain.{AutoRenewal, AutoRenewalMultiple, Complete, NotStartedMultiple, NotStartedSingle, OneNotStartedMultiple, PackNotSent, RenewalStatus, RenewalSubmitted, Renewals, ViewOnly}

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
    Renewals(
      ViewOnly,
      1,
      1,
      now.minusMonths(3).toString,
      now.minusMonths(2).toString,
      now.plusMonths(1).toString,
      renewNowLink = Some("/tax-credits-service/renewals/barcode-picker")
    )

  def defaultClaimRenewals(
    status:          RenewalStatus,
    totalClaims:     Int = 1,
    claimsSubmitted: Int = 0
  ): Renewals =
    Renewals(
      status,
      totalClaims,
      claimsSubmitted,
      now.minusMonths(1).toString,
      now.plusMonths(1).toString,
      now.plusMonths(3).toString,
      renewNowLink = Some("/tax-credits-service/renewals/barcode-picker")
    )

  "getTaxCreditsRenewals" should {
    "return nothing if the renewal period is closed" in {
      val serviceClosed = new TaxCreditsRenewalsService(mockTaxCreditsRenewalsConnector,
                                                        now.minusMonths(3).toString,
                                                        now.minusMonths(3).toString,
                                                        now.minusMonths(2).toString,
                                                        now.minusMonths(1).toString,
                                                        now.minusMonths(1).toString)
      await(serviceClosed.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe None
    }

    "return nothing if no claims are found" in {
      val serviceClosed = new TaxCreditsRenewalsService(mockTaxCreditsRenewalsConnector,
                                                        now.minusMonths(3).toString,
                                                        now.minusMonths(3).toString,
                                                        now.minusMonths(2).toString,
                                                        now.minusMonths(1).toString,
                                                        now.minusMonths(1).toString)
      await(serviceClosed.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe None
    }

    "return the householdBreakdown and gracePeriod flags correctly" in {
      val serviceGracePeriod = new TaxCreditsRenewalsService(mockTaxCreditsRenewalsConnector,
                                                             now.minusMonths(3).toString,
                                                             now.minusMonths(3).toString,
                                                             now.minusMonths(2).toString,
                                                             now.plusMonths(1).toString,
                                                             now.plusMonths(2).toString)
      mockTaxCreditsRenewalsConnectorSingleClaimWithHouseholdBreakdown(tcrNino, COMPLETE)
      await(serviceGracePeriod.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(Complete, 1, 1).copy(
          packReceivedDate    = now.minusMonths(3).toString,
          renewalsEndDate     = now.minusMonths(2).toString,
          viewRenewalsEndDate = now.plusMonths(2).toString,
          inGracePeriod       = true,
          householdBreakdown  = true
        )
      )
    }

    "return the ViewOnly response correctly" in {
      val serviceViewOnly = new TaxCreditsRenewalsService(mockTaxCreditsRenewalsConnector,
                                                          now.minusMonths(3).toString,
                                                          now.minusMonths(3).toString,
                                                          now.minusMonths(2).toString,
                                                          now.minusMonths(1).toString,
                                                          now.plusMonths(1).toString)
      mockTaxCreditsRenewalsConnectorSingleClaim(tcrNino, COMPLETE)
      await(serviceViewOnly.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(viewOnlyRenewals)
    }

    "return the PackNotSent response correctly" in {
      mockTaxCreditsRenewalsConnectorSingleClaim(tcrNino, AWAITING_BARCODE)
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(PackNotSent)
      )
    }

    "return the AutoRenewal response correctly" in {
      mockTaxCreditsRenewalsConnectorSingleClaim(tcrNino, NOT_SUBMITTED, autoRenewal = true)
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(AutoRenewal)
      )
    }

    "return the NotStartedSingle response correctly" in {
      mockTaxCreditsRenewalsConnectorSingleClaim(tcrNino, NOT_SUBMITTED)
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(NotStartedSingle)
      )
    }

    "return the RenewalSubmitted response correctly for a single claim" in {
      mockTaxCreditsRenewalsConnectorSingleClaim(tcrNino, SUBMITTED_AND_PROCESSING)
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(RenewalSubmitted, 1, 1)
      )
    }

    "return the Complete response correctly for a single claim" in {
      mockTaxCreditsRenewalsConnectorSingleClaim(tcrNino, COMPLETE)
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(Complete, 1, 1)
      )
    }

    "return the RenewalSubmitted response correctly for a multiple claims" in {
      mockTaxCreditsRenewalsConnectorMultipleClaims(tcrNino,
                                                    Seq(singleClaim(SUBMITTED_AND_PROCESSING),
                                                        singleClaim(SUBMITTED_AND_PROCESSING)))
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(RenewalSubmitted, totalClaims = 2, claimsSubmitted = 2)
      )
    }

    "return the Complete response correctly for a multiple claims" in {
      mockTaxCreditsRenewalsConnectorMultipleClaims(tcrNino, Seq(singleClaim(COMPLETE), singleClaim(COMPLETE)))
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(Complete, totalClaims = 2, claimsSubmitted = 2)
      )
    }

    "return the AutoRenewalMultiple response correctly" in {
      mockTaxCreditsRenewalsConnectorMultipleClaims(tcrNino,
                                                    Seq(singleClaim(NOT_SUBMITTED, autoRenewal = true),
                                                        singleClaim(NOT_SUBMITTED, autoRenewal = true)))
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(AutoRenewalMultiple, totalClaims = 2)
      )
    }

    "return the NotStartedMultiple response correctly" in {
      mockTaxCreditsRenewalsConnectorMultipleClaims(tcrNino,
                                                    Seq(singleClaim(NOT_SUBMITTED), singleClaim(NOT_SUBMITTED)))
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(NotStartedMultiple, totalClaims = 2)
      )
    }

    "return the OneNotStartedMultiple response correctly" in {
      mockTaxCreditsRenewalsConnectorMultipleClaims(tcrNino,
                                                    Seq(singleClaim(NOT_SUBMITTED),
                                                        singleClaim(SUBMITTED_AND_PROCESSING)))
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(OneNotStartedMultiple, totalClaims = 2, claimsSubmitted = 1)
      )
    }

    "return the RenewalSubmitted response if one claim is complete and one is submitted" in {
      mockTaxCreditsRenewalsConnectorMultipleClaims(tcrNino,
                                                    Seq(singleClaim(COMPLETE), singleClaim(SUBMITTED_AND_PROCESSING)))
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(RenewalSubmitted, totalClaims = 2, claimsSubmitted = 2)
      )
    }

    "return the AutoRenewalMultiple response if the user has multiple claims, but only autoRenew claims have a status of NOT_SUBMITTED" in {
      mockTaxCreditsRenewalsConnectorMultipleClaims(tcrNino,
                                                    Seq(singleClaim(COMPLETE),
                                                        singleClaim(SUBMITTED_AND_PROCESSING),
                                                        singleClaim(NOT_SUBMITTED, autoRenewal = true)))
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(AutoRenewalMultiple, totalClaims = 3, claimsSubmitted = 2)
      )
    }

    "return the OneNotStartedMultiple response if the user has multiple claims, and a manual claim has the status of NOT_SUBMITTED" in {
      mockTaxCreditsRenewalsConnectorMultipleClaims(
        tcrNino,
        Seq(singleClaim(COMPLETE), singleClaim(SUBMITTED_AND_PROCESSING), singleClaim(NOT_SUBMITTED))
      )
      await(serviceOpen.getTaxCreditsRenewals(tcrNino, journeyId)) shouldBe Some(
        defaultClaimRenewals(OneNotStartedMultiple, totalClaims = 3, claimsSubmitted = 2)
      )
    }

  }
}
