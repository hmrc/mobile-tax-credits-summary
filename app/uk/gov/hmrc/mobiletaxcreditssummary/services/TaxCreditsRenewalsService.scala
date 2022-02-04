/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.mobiletaxcreditssummary.connectors.TaxCreditsRenewalsConnector
import uk.gov.hmrc.mobiletaxcreditssummary.domain.{AutoRenewal, AutoRenewalMultiple, Complete, NotStartedMultiple, NotStartedSingle, OneNotStartedMultiple, PackNotSent, RenewalStatus, RenewalSubmitted, Renewals, TaxCreditsNino, ViewOnly}
import uk.gov.hmrc.mobiletaxcreditssummary.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.LegacyClaims
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.LegacyRenewalStatus.{AWAITING_BARCODE, COMPLETE, NOT_SUBMITTED, SUBMITTED_AND_PROCESSING}

import java.time.{LocalDateTime, ZoneId}
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxCreditsRenewalsService @Inject() (
  renewalsConnector:                                               TaxCreditsRenewalsConnector,
  @Named("renewalsStartDate") renewalsStartDate:                   String,
  @Named("renewalsPackReceivedDate") renewalsPackReceivedDate:     String,
  @Named("renewalsEndDate") renewalsEndDate:                       String,
  @Named("renewalsGracePeriodEndDate") renewalsGracePeriodEndDate: String,
  @Named("renewalsEndViewDate") renewalsEndViewDate:               String) {

  val startDate:                LocalDateTime = LocalDateTime.parse(renewalsStartDate)
  val endDate:                  LocalDateTime = LocalDateTime.parse(renewalsEndDate)
  val gracePeriodEndDate:       LocalDateTime = LocalDateTime.parse(renewalsGracePeriodEndDate)
  val endViewDate:              LocalDateTime = LocalDateTime.parse(renewalsEndViewDate)
  val householdBreakdownString: String        = "HOUSEHOLD BREAKDOWN"
  val autoRenewalFormType:      String        = "R"

  def getTaxCreditsRenewals(
    nino:        TaxCreditsNino,
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[Option[Renewals]] =
    if (renewalsClosed) Future successful None
    else {
      val renewals: Future[Option[LegacyClaims]] = renewalsConnector.getRenewals(journeyId, nino)
      renewals.map(response =>
        response.flatMap { claims =>
          claims.references.map {
            references =>
              if (viewOnlyPeriod) buildRenewalsResponse(ViewOnly, 1, 1)
              else {
                references.size match {
                  case 1 =>
                    val singleClaim = references.head
                    val householdBreakdown = singleClaim.household.householdEndReason
                        .getOrElse("") == householdBreakdownString
                    singleClaim.renewal.renewalStatus match {
                      case Some(AWAITING_BARCODE) => buildRenewalsResponse(PackNotSent, 1, 0, householdBreakdown)

                      case Some(NOT_SUBMITTED) =>
                        if (singleClaim.renewal.renewalFormType.getOrElse("") == autoRenewalFormType)
                          buildRenewalsResponse(AutoRenewal, 1, 0, householdBreakdown)
                        else buildRenewalsResponse(NotStartedSingle, 1, 0, householdBreakdown)

                      case Some(SUBMITTED_AND_PROCESSING) =>
                        buildRenewalsResponse(RenewalSubmitted, 1, 1, householdBreakdown)
                      case Some(COMPLETE) => buildRenewalsResponse(Complete, 1, 1, householdBreakdown)
                    }
                  case _ =>
                    val totalClaims = references.size
                    val statuses    = references.map(_.renewal.renewalStatus.getOrElse(NOT_SUBMITTED))
                    val submittedClaims =
                      statuses.count(status => status == SUBMITTED_AND_PROCESSING || status == COMPLETE)
                    val householdBreakdown =
                      references.exists(_.household.householdEndReason.getOrElse("") == householdBreakdownString)
                    if (statuses.distinct.size == 1)
                      statuses.head match {
                        case NOT_SUBMITTED =>
                          val formTypes = references.map(_.renewal.renewalFormType.getOrElse("D"))
                          if (formTypes.distinct.size == 1 && formTypes.head == autoRenewalFormType)
                            buildRenewalsResponse(AutoRenewalMultiple, totalClaims, submittedClaims, householdBreakdown)
                          else
                            buildRenewalsResponse(NotStartedMultiple, totalClaims, submittedClaims, householdBreakdown)
                        case SUBMITTED_AND_PROCESSING =>
                          buildRenewalsResponse(RenewalSubmitted, totalClaims, submittedClaims, householdBreakdown)
                        case COMPLETE =>
                          buildRenewalsResponse(Complete, totalClaims, submittedClaims, householdBreakdown)
                        case _ => buildRenewalsResponse(PackNotSent, totalClaims, submittedClaims, householdBreakdown)
                      }
                    else {
                      (statuses.contains(NOT_SUBMITTED), statuses.contains(SUBMITTED_AND_PROCESSING)) match {
                        case (true, _) =>
                          val notSubmittedManualRenewals = references.filter(ref =>
                            ref.renewal.renewalFormType
                              .getOrElse("") != autoRenewalFormType && ref.renewal.renewalStatus
                              .getOrElse("") == NOT_SUBMITTED
                          )
                          if (notSubmittedManualRenewals.isEmpty)
                            buildRenewalsResponse(AutoRenewalMultiple, totalClaims, submittedClaims, householdBreakdown)
                          else
                            buildRenewalsResponse(OneNotStartedMultiple,
                                                  totalClaims,
                                                  submittedClaims,
                                                  householdBreakdown)
                        case (_, true) =>
                          buildRenewalsResponse(RenewalSubmitted, totalClaims, submittedClaims, householdBreakdown)
                        case _ => buildRenewalsResponse(PackNotSent, totalClaims, submittedClaims, householdBreakdown)
                      }
                    }
                }
              }
          }
        }
      )
    }

  private def currentTime: LocalDateTime = LocalDateTime.now(ZoneId.of("Europe/London"))

  private def buildRenewalsResponse(
    status:             RenewalStatus,
    totalClaims:        Int,
    claimsSubmitted:    Int,
    householdBreakdown: Boolean = false
  ): Renewals = {
    val inGracePeriod = currentTime.isAfter(endDate) && currentTime.isBefore(gracePeriodEndDate)
    Renewals(
      status,
      totalClaims,
      claimsSubmitted,
      renewalsPackReceivedDate,
      renewalsEndDate,
      renewalsEndViewDate,
      householdBreakdown,
      inGracePeriod,
      renewNowLink = Some("/tax-credits-service/renewals/barcode-picker")
    )
  }

  private def renewalsClosed: Boolean = currentTime.isBefore(startDate) || currentTime.isAfter(endViewDate)

  private def viewOnlyPeriod: Boolean = currentTime.isAfter(gracePeriodEndDate) && currentTime.isBefore(endViewDate)

}
