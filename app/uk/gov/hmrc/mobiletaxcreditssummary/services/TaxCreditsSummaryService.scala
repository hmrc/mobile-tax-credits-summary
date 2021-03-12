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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobiletaxcreditssummary.connectors._
import uk.gov.hmrc.mobiletaxcreditssummary.domain._
import uk.gov.hmrc.mobiletaxcreditssummary.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.{Child, Claimants, DashboardData, MessageLink, PaymentSummary, Person, TaxCreditsSummary, TaxCreditsSummaryResponse, UnknownCircumstance}

import scala.concurrent.{ExecutionContext, Future}

trait TaxCreditsSummaryService {

  def getTaxCreditsSummaryResponse(
    nino:        Nino,
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[TaxCreditsSummaryResponse]
}

@Singleton
class LiveTaxCreditsSummaryService @Inject() (
  taxCreditsBrokerConnector: TaxCreditsBrokerConnector,
  taxCreditsRenewalsService: TaxCreditsRenewalsService,
  reportActualProfitService: ReportActualProfitService,
  informationMessageService: InformationMessageService)
    extends TaxCreditsSummaryService {

  override def getTaxCreditsSummaryResponse(
    nino:        Nino,
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[TaxCreditsSummaryResponse] = {
    val tcNino = TaxCreditsNino(nino.value)

    def buildTaxCreditsSummary(dashboardData: DashboardData): TaxCreditsSummaryResponse = {
      def getChildrenAge16AndUnder: Seq[Person] =
        Child.getEligibleChildren(dashboardData.childrenDetails.child)

      val children: Seq[Person] = getChildrenAge16AndUnder
      val partnerDetails  = dashboardData.partnerDetails
      val personalDetails = dashboardData.personalDetails
      val specialCircumstance = dashboardData.paymentSummary.specialCircumstances match {
        case Some(UnknownCircumstance) => None
        case e                         => e
      }
      val reportActualProfit = reportActualProfitService.getReportActualProfitDetails(
        tcNino,
        dashboardData.actualIncomeStatus,
        dashboardData.awardDetails.mainApplicantNino
      )

      val messageLink: Option[MessageLink] = informationMessageService.getMessageLink(dashboardData.paymentSummary)
      val claimants       = Some(Claimants(personalDetails, partnerDetails, children, messageLink, reportActualProfit))
      val isMultipleFTNAE = dashboardData.paymentSummary.isMultipleFTNAE.getOrElse(false)

      val newPayment: PaymentSummary =
        dashboardData.paymentSummary.copy(
          informationMessage   = informationMessageService.getInformationMessage(specialCircumstance, isMultipleFTNAE),
          specialCircumstances = specialCircumstance
        )
      TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary(newPayment, claimants, None)))
    }

    def buildResponseFromPaymentSummary: Future[TaxCreditsSummaryResponse] =
      taxCreditsBrokerConnector.getDashboardData(tcNino).flatMap {
        case Some(dashboardData) =>
          if (dashboardData.paymentSummary.excluded.getOrElse(false)) {
            // in the context of getPaymentSummary, 'excluded == true' means a non-tax credits user
            // as the app treats excluded false and not other body as no TC
            Future successful TaxCreditsSummaryResponse(excluded = false, None)
          } else {
            Future successful buildTaxCreditsSummary(dashboardData)
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
