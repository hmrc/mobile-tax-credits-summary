/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.mobiletaxcreditssummary.mocks

import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobiletaxcreditssummary.connectors.TaxCreditsRenewalsConnector
import uk.gov.hmrc.mobiletaxcreditssummary.domain.TaxCreditsNino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.{Applicant, Household, LegacyClaim, LegacyClaims, LegacyRenewal}

import scala.concurrent.{ExecutionContext, Future}

trait TaxCreditsRenewalsConnectorMock extends MockFactory {
  val applicantNino: Nino      = Nino("CS700100A")
  val applicant:     Applicant = Applicant(applicantNino.nino, "MR", "BOB", None, "ROBSON", None)
  val household:     Household = Household("123456789", "applicationId", applicant, None, None, None)

  def mockTaxCreditsRenewalsConnectorSingleClaim(
    nino:                                 TaxCreditsNino,
    status:                               String,
    autoRenewal:                          Boolean = false
  )(implicit taxCreditsRenewalsConnector: TaxCreditsRenewalsConnector
  ): Unit = mockTaxCreditsRenewalsConnector(Some(LegacyClaims(Some(Seq(singleClaim(status, autoRenewal))))), nino)

  def mockTaxCreditsRenewalsConnectorSingleClaimWithHouseholdBreakdown(
    nino:                                 TaxCreditsNino,
    status:                               String,
    autoRenewal:                          Boolean = false
  )(implicit taxCreditsRenewalsConnector: TaxCreditsRenewalsConnector
  ): Unit =
    mockTaxCreditsRenewalsConnector(
      Some(
        LegacyClaims(
          Some(
            Seq(
              singleClaim(status, autoRenewal)
                .copy(household = household.copy(householdEndReason = Some("HOUSEHOLD BREAKDOWN")))
            )
          )
        )
      ),
      nino
    )

  def mockTaxCreditsRenewalsConnectorMultipleClaims(
    nino:                                 TaxCreditsNino,
    claims:                               Seq[LegacyClaim]
  )(implicit taxCreditsRenewalsConnector: TaxCreditsRenewalsConnector
  ): Unit = mockTaxCreditsRenewalsConnector(Some(LegacyClaims(Some(claims))), nino)

  def mockTaxCreditsRenewalsConnectorNoClaims(
    nino:                                 TaxCreditsNino
  )(implicit taxCreditsRenewalsConnector: TaxCreditsRenewalsConnector
  ): Unit = mockTaxCreditsRenewalsConnector(None, nino)

  def singleClaim(
    status:      String,
    autoRenewal: Boolean = false
  ): LegacyClaim =
    (LegacyClaim(household, legacyRenewal(status, if (autoRenewal) Some("R") else None)))

  def legacyRenewal(
    status:   String,
    formType: Option[String] = None
  ): LegacyRenewal = LegacyRenewal(None, None, Some(status), None, None, formType)

  def mockTaxCreditsRenewalsConnector(
    response:                             Option[LegacyClaims],
    nino:                                 TaxCreditsNino
  )(implicit taxCreditsRenewalsConnector: TaxCreditsRenewalsConnector
  ): Unit =
    (taxCreditsRenewalsConnector
      .getRenewals(_: JourneyId, _: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, nino, *, *)
      .returning(Future successful response)
}
