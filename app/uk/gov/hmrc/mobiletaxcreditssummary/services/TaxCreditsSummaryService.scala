/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.{LocalDate, LocalDateTime, Month, ZonedDateTime}

import com.google.inject.{Inject, Singleton}
import javax.inject.Named
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobiletaxcreditssummary.connectors._
import uk.gov.hmrc.mobiletaxcreditssummary.domain._
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.{Child, ClaimActualIncomeEligibilityStatus, Claimants, FtnaeLink, FuturePayment, InformationMessage, PaymentSummary, Person, ReportActualProfit, TaxCreditsSummary, TaxCreditsSummaryResponse}
import uk.gov.hmrc.mobiletaxcreditssummary.utils.LocalDateProvider

import scala.concurrent.{ExecutionContext, Future}

trait TaxCreditsSummaryService {

  def getTaxCreditsSummaryResponse(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[TaxCreditsSummaryResponse]
}

@Singleton
class LiveTaxCreditsSummaryService @Inject() (
  taxCreditsBrokerConnector:                                                TaxCreditsBrokerConnector,
  localDateProvider:                                                        LocalDateProvider,
  @Named("reportActualProfitPeriod.startDate") reportActualProfitStartDate: String,
  @Named("reportActualProfitPeriod.endDate") reportActualProfitEndDate:     String,
  @Named("featureFlag.covid") showCovidNotFtnae:                            Boolean = false)
    extends TaxCreditsSummaryService {

  override def getTaxCreditsSummaryResponse(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[TaxCreditsSummaryResponse] = {
    val tcNino = TaxCreditsNino(nino.value)
    val now: LocalDate = localDateProvider.now

    def buildTaxCreditsSummary(paymentSummary: PaymentSummary): Future[TaxCreditsSummaryResponse] = {
      def getChildrenAge16AndUnder: Future[Seq[Person]] =
        taxCreditsBrokerConnector.getChildren(tcNino).map(children => Child.getEligibleChildren(children))

      def createLocalDate(
        year:  Int,
        month: Month,
        day:   Int
      ): LocalDate = LocalDate.of(year, month, day)

      def isFtnaeDate(payment: FuturePayment): Boolean =
        payment.paymentDate.isAfter(createLocalDate(now.getYear, Month.AUGUST, 31).atStartOfDay()) && payment.paymentDate.getYear == now.getYear

      // Payments after August 31st are hidden if special circumstances is "FTNAE" to work out if payments are FTNAE we check there
      // are no payments after the 31st and this is coupled with other logic in the match
      def hasAFtnaePayment(paymentSummary: PaymentSummary): Boolean =
        paymentSummary.childTaxCredit match {
          case None if (now.isBefore(createLocalDate(now.getYear, Month.SEPTEMBER, 8))) => true
          case None                                                                     => false
          case Some(ctc)                                                                => ctc.paymentSeq.count(payment => isFtnaeDate(payment)) > 0 //TODO check if should be >
        }

      def getFtnaeLink(paymentSummary: PaymentSummary): Option[FtnaeLink] =
        if (showCovidNotFtnae) {
          None
        } else {

          val hasFtnaePayment: Boolean = hasAFtnaePayment(paymentSummary)
          val hasSpecialCircumstances = paymentSummary.specialCircumstances.isDefined

          (hasFtnaePayment, hasSpecialCircumstances) match {
            case (false, _) => None
            case (true, true) if now.isBefore(createLocalDate(now.getYear, Month.SEPTEMBER, 1)) =>
              Some(FtnaeLink(preFtnaeDeadline = true, "/tax-credits-service/home/children-and-childcare"))
            case (true, true)
                if now.isAfter(createLocalDate(now.getYear, Month.AUGUST, 31)) &&
                now.isBefore(createLocalDate(now.getYear, Month.SEPTEMBER, 8)) =>
              Some(
                FtnaeLink(preFtnaeDeadline = false, "/tax-credits-service/children/add-child/who-do-you-want-to-add")
              )
            case _ => None
          }
        }

      def getReportActualProfitDetails: Future[Option[ReportActualProfit]] =
        if (!reportActualProfitPeriodOpen)
          Future.successful(None)
        else
          taxCreditsBrokerConnector.getDashboardData(tcNino).flatMap {
            case None => Future successful None
            case Some(dashboardData) =>
              buildReportActualProfit(dashboardData.actualIncomeStatus, dashboardData.awardDetails.mainApplicantNino)
          }

      def buildReportActualProfit(
        status:            ClaimActualIncomeEligibilityStatus,
        mainApplicantNino: TaxCreditsNino
      ): Future[Option[ReportActualProfit]] = {
        val userLoggedInIsMainApplicant = tcNino.value == mainApplicantNino.value
        (status.applicant1, status.applicant2, userLoggedInIsMainApplicant) match {
          case (ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
                ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
                _) =>
            Future successful Some(
              ReportActualProfit(
                "/tax-credits-service/actual-profit",
                reportActualProfitEndDate,
                userMustReportIncome    = true,
                partnerMustReportIncome = true
              )
            )
          case (ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
                ClaimActualIncomeEligibilityStatus.APPLICANT_NOT_APPLICABLE,
                true) =>
            Future successful Some(
              ReportActualProfit(
                "/tax-credits-service/actual-self-employed-profit-or-loss",
                reportActualProfitEndDate,
                userMustReportIncome    = true,
                partnerMustReportIncome = false
              )
            )
          case (ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
                ClaimActualIncomeEligibilityStatus.APPLICANT_NOT_APPLICABLE,
                false) =>
            Future successful Some(
              ReportActualProfit(
                "/tax-credits-service/actual-self-employed-profit-or-loss-partner",
                reportActualProfitEndDate,
                userMustReportIncome    = false,
                partnerMustReportIncome = true
              )
            )
          case (ClaimActualIncomeEligibilityStatus.APPLICANT_NOT_APPLICABLE,
                ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
                false) =>
            Future successful Some(
              ReportActualProfit(
                "/tax-credits-service/actual-self-employed-profit-or-loss",
                reportActualProfitEndDate,
                userMustReportIncome    = true,
                partnerMustReportIncome = false
              )
            )
          case (ClaimActualIncomeEligibilityStatus.APPLICANT_NOT_APPLICABLE,
                ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
                true) =>
            Future successful Some(
              ReportActualProfit(
                "/tax-credits-service/actual-self-employed-profit-or-loss-partner",
                reportActualProfitEndDate,
                userMustReportIncome    = false,
                partnerMustReportIncome = true
              )
            )
          case (_, _, _) => Future successful None
        }
      }

      def reportActualProfitPeriodOpen: Boolean = {
        val startDate = ZonedDateTime.parse(reportActualProfitStartDate)
        val endDate   = ZonedDateTime.parse(reportActualProfitEndDate)
        (LocalDateTime.now().isAfter(startDate.toLocalDateTime) && LocalDateTime
          .now()
          .isBefore(endDate.toLocalDateTime))
      }

      def getInformationMessage: Option[InformationMessage] =
        if (showCovidNotFtnae) {
          Some(
            InformationMessage(
              "Do not update working hours if you or your partner are working less because of coronavirus (COVID-19)",
              "We are treating this as a temporary exceptional change. Your working tax credits will not go down because you are working less.\n\nYou can still report other changes in income, childcare and increased hours as normal.\n\nSearch COVID-19 on gov.uk for the latest advice."
            )
          )
        } else {

          if (paymentSummary.specialCircumstances.isDefined) {
            val isMultipleFTNAE: Boolean = paymentSummary.isMultipleFTNAE.getOrElse(false)
            val childChildren           = if (isMultipleFTNAE) "children are" else "child is"
            val hasSpecialCircumstances = paymentSummary.specialCircumstances.isDefined

            if (now.isBefore(createLocalDate(now.getYear, Month.SEPTEMBER, 1)) && hasSpecialCircumstances) {
              Some(
                InformationMessage(
                  f"We are currently working out your payments as your $childChildren changing their education or training. This should be done by 7 September ${now.getYear}.",
                  f"If your $childChildren staying in education or training, you should update their details."
                )
              )
            } else if (now.isAfter(createLocalDate(now.getYear, Month.AUGUST, 31)) &&
                       now.isBefore(createLocalDate(now.getYear, Month.SEPTEMBER, 8)) && hasSpecialCircumstances) {
              Some(
                InformationMessage(
                  f"We are currently working out your payments as your $childChildren changing their education or training. This should be done by 7 September ${now.getYear}.",
                  f"If you have let us know that your $childChildren staying in education or training, they will be added back automatically. Otherwise, you can add them back to your claim."
                )
              )
            } else {
              None
            }
          } else None
        }

      val childrenFuture: Future[Seq[Person]] = getChildrenAge16AndUnder
      val partnerDetailsFuture  = taxCreditsBrokerConnector.getPartnerDetails(tcNino)
      val personalDetailsFuture = taxCreditsBrokerConnector.getPersonalDetails(tcNino)

      val claimants: Future[Option[Claimants]] = (for {
        children           <- childrenFuture
        partnerDetails     <- partnerDetailsFuture
        personalDetails    <- personalDetailsFuture
        reportActualProfit <- getReportActualProfitDetails
      } yield {
        val ftnaeLink: Option[FtnaeLink] = getFtnaeLink(paymentSummary)
        Some(Claimants(personalDetails, partnerDetails, children, ftnaeLink, reportActualProfit))
      }).recover {
        case _ => None
      }

      val newPayment = if (showCovidNotFtnae) {
        paymentSummary.copy(informationMessage = getInformationMessage, specialCircumstances = Some("COVID"))
      } else {
        paymentSummary.copy(informationMessage = getInformationMessage)
      }

      claimants.map(c => TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary(newPayment, c))))
    }

    def buildResponseFromPaymentSummary: Future[TaxCreditsSummaryResponse] =
      taxCreditsBrokerConnector.getPaymentSummary(tcNino).flatMap {
        case Some(summary) =>
          if (summary.excluded.getOrElse(false)) {
            // in the context of getPaymentSummary, 'excluded == true' means a non-tax credits user
            // as the app treats excluded false and not other body as no TC
            Future successful TaxCreditsSummaryResponse(excluded = false, None)
          } else {
            buildTaxCreditsSummary(summary)
          }
        case None => Future successful TaxCreditsSummaryResponse(excluded = false, None)
      }

    taxCreditsBrokerConnector.getExclusion(tcNino).flatMap {
      case Some(exclusion) =>
        if (exclusion.excluded) Future successful TaxCreditsSummaryResponse(excluded = true, None)
        else buildResponseFromPaymentSummary
      case None => Future successful TaxCreditsSummaryResponse(excluded = false, None)
    }
  }
}
