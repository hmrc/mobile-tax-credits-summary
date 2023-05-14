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

package uk.gov.hmrc.mobiletaxcreditssummary.services

import java.time.{LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime}
import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.mobiletaxcreditssummary.domain.TaxCreditsNino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.{ClaimActualIncomeEligibilityStatus, ReportActualProfit}

import java.time.format.DateTimeFormatter

@Singleton
class ReportActualProfitService @Inject() (
  @Named("reportActualProfitPeriod.startDate") reportActualProfitStartDate: String,
  @Named("reportActualProfitPeriod.endDate") reportActualProfitEndDate:     String) {

  def getReportActualProfitDetails(
    tcNino:            TaxCreditsNino,
    status:            ClaimActualIncomeEligibilityStatus,
    mainApplicantNino: TaxCreditsNino
  ): Option[ReportActualProfit] =
    if (!reportActualProfitPeriodOpen)
      None
    else {
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
      val endDate   = LocalDateTime.parse(reportActualProfitEndDate).format(formatter)
      buildReportActualProfit(tcNino: TaxCreditsNino, status, mainApplicantNino, endDate)
    }

  private def buildReportActualProfit(
    tcNino:            TaxCreditsNino,
    status:            ClaimActualIncomeEligibilityStatus,
    mainApplicantNino: TaxCreditsNino,
    endDate:           String
  ): Option[ReportActualProfit] = {
    val userLoggedInIsMainApplicant = tcNino.value == mainApplicantNino.value
    (status.applicant1, status.applicant2, userLoggedInIsMainApplicant) match {
      case (ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
            ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
            _) =>
        Some(
          ReportActualProfit(
            "/tax-credits-service/actual-profit",
            endDate,
            userMustReportIncome    = true,
            partnerMustReportIncome = true
          )
        )
      case (ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
            ClaimActualIncomeEligibilityStatus.APPLICANT_NOT_APPLICABLE,
            true) =>
        Some(
          ReportActualProfit(
            "/tax-credits-service/actual-self-employed-profit-or-loss",
            endDate,
            userMustReportIncome    = true,
            partnerMustReportIncome = false
          )
        )
      case (ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
            ClaimActualIncomeEligibilityStatus.APPLICANT_NOT_APPLICABLE,
            false) =>
        Some(
          ReportActualProfit(
            "/tax-credits-service/actual-self-employed-profit-or-loss-partner",
            endDate,
            userMustReportIncome    = false,
            partnerMustReportIncome = true
          )
        )
      case (ClaimActualIncomeEligibilityStatus.APPLICANT_NOT_APPLICABLE,
            ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
            false) =>
        Some(
          ReportActualProfit(
            "/tax-credits-service/actual-self-employed-profit-or-loss",
            endDate,
            userMustReportIncome    = true,
            partnerMustReportIncome = false
          )
        )
      case (ClaimActualIncomeEligibilityStatus.APPLICANT_NOT_APPLICABLE,
            ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
            true) =>
        Some(
          ReportActualProfit(
            "/tax-credits-service/actual-self-employed-profit-or-loss-partner",
            endDate,
            userMustReportIncome    = false,
            partnerMustReportIncome = true
          )
        )
      case (_, _, _) => None
    }
  }

  private def reportActualProfitPeriodOpen: Boolean = {
    val currentTime: LocalDateTime = LocalDateTime.now(ZoneId.of("Europe/London"))
    val startDate = LocalDateTime.parse(reportActualProfitStartDate)
    val endDate   = LocalDateTime.parse(reportActualProfitEndDate)
    (currentTime.isAfter(startDate) && currentTime.isBefore(endDate))
  }

}
