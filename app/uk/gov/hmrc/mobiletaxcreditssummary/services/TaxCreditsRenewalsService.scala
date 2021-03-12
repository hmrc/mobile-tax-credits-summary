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

package uk.gov.hmrc.mobiletaxcreditssummary.services

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobiletaxcreditssummary.connectors.RenewalsConnector
import uk.gov.hmrc.mobiletaxcreditssummary.domain.{AutoRenewal, Complete, NotStartedSingle, PackNotSent, RenewalStatus, RenewalSubmitted, Renewals, TaxCreditsNino, ViewOnly}
import uk.gov.hmrc.mobiletaxcreditssummary.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.LegacyClaims

import java.time.{LocalDateTime, ZoneId}
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxCreditsRenewalsService @Inject() (
  renewalsConnector:                                               RenewalsConnector,
  @Named("renewalsStartDate") renewalsStartDate:                   String,
  @Named("renewalsPackReceivedDate") renewalsPackReceivedDate:     String,
  @Named("renewalsEndDate") renewalsEndDate:                       String,
  @Named("renewalsGracePeriodEndDate") renewalsGracePeriodEndDate: String,
  @Named("renewalsEndViewDate") renewalsEndViewDate:               String) {

  val currentTime:        LocalDateTime = LocalDateTime.now(ZoneId.of("Europe/London"))
  val startDate:          LocalDateTime = LocalDateTime.parse(renewalsStartDate)
  val endDate:            LocalDateTime = LocalDateTime.parse(renewalsEndDate)
  val gracePeriodEndDate: LocalDateTime = LocalDateTime.parse(renewalsGracePeriodEndDate)
  val endViewDate:        LocalDateTime = LocalDateTime.parse(renewalsEndViewDate)

  def getTaxCreditsRenewals(
    nino:        TaxCreditsNino,
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[Option[Renewals]] = {

    if (renewalsClosed) Future successful None

    val renewals: Future[Option[LegacyClaims]] = renewalsConnector.getRenewals(journeyId, nino)
    renewals.map(response =>
      response.flatMap { claims =>
        claims.references.map {
          references =>
            references.size match {
              case 1 =>
                val singleClaim        = references.head
                val householdBreakdown = singleClaim.household.householdEndReason.getOrElse("") == "HOUSEHOLD BREAKDOWN"
                if (viewOnlyPeriod) buildRenewalsResponse(ViewOnly, 1, 0, householdBreakdown)
                else {
                  singleClaim.renewal.renewalStatus match {
                    case Some("AWAITING_BARCODE") => buildRenewalsResponse(PackNotSent, 1, 0, householdBreakdown)

                    case Some("NOT_SUBMITTED") =>
                      if (singleClaim.renewal.renewalFormType.getOrElse("") == "R")
                        buildRenewalsResponse(AutoRenewal, 1, 0, householdBreakdown)
                      else buildRenewalsResponse(NotStartedSingle, 1, 0, householdBreakdown)

                    case Some("SUBMITTED_AND_PROCESSING") =>
                      buildRenewalsResponse(RenewalSubmitted, 1, 1, householdBreakdown)
                    case Some("COMPLETE") => buildRenewalsResponse(Complete, 1, 1, householdBreakdown)
                  }
                }
              case _ => {
                val totalClaims = references.size
                val submittedClaims = references.count(claim =>
                  claim.renewal.renewalStatus.getOrElse("") == "SUBMITTED_AND_PROCESSING" || claim.renewal.renewalStatus
                    .getOrElse("") == "COMPLETE"
                )
                val householdBreakdown =
                  references.exists(_.household.householdEndReason.getOrElse("") == "HOUSEHOLD BREAKDOWN")
                if (viewOnlyPeriod) buildRenewalsResponse(ViewOnly, totalClaims, submittedClaims, householdBreakdown)
                //TODO Implement logic for multi-statuses
              }
            }
        }
      }
    )

  }

  private def buildRenewalsResponse(
    status:             RenewalStatus,
    totalClaims:        Int,
    claimsSubmitted:    Int,
    householdBreakdown: Boolean = false
  ): Renewals = {
    val inGracePeriod = currentTime.isAfter(endDate) && currentTime.isBefore(gracePeriodEndDate)
    Renewals(status,
             totalClaims,
             claimsSubmitted,
             renewalsPackReceivedDate,
             renewalsEndDate,
             renewalsEndViewDate,
             householdBreakdown,
             inGracePeriod)
  }

  private def renewalsClosed: Boolean = currentTime.isBefore(startDate) || currentTime.isAfter(endViewDate)

  private def viewOnlyPeriod: Boolean = currentTime.isAfter(endDate) && currentTime.isBefore(endViewDate)

}
