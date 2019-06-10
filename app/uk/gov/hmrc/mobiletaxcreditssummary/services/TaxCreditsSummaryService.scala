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

package uk.gov.hmrc.mobiletaxcreditssummary.services

import java.time.{LocalDate, LocalDateTime, Month}

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobiletaxcreditssummary.connectors._
import uk.gov.hmrc.mobiletaxcreditssummary.domain._
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._
import uk.gov.hmrc.mobiletaxcreditssummary.utils.LocalDateProvider

import scala.concurrent.{ExecutionContext, Future}

trait TaxCreditsSummaryService {
  def getTaxCreditsSummaryResponse(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[TaxCreditsSummaryResponse]
}

@Singleton
class LiveTaxCreditsSummaryService @Inject()(taxCreditsBrokerConnector: TaxCreditsBrokerConnector) extends TaxCreditsSummaryService {

  override def getTaxCreditsSummaryResponse(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[TaxCreditsSummaryResponse] = {
    val tcNino = TaxCreditsNino(nino.value)
    val now: LocalDate = LocalDateProvider.now

    def buildTaxCreditsSummary(paymentSummary: PaymentSummary): Future[TaxCreditsSummaryResponse] = {
      def getChildrenAge16AndUnder: Future[Seq[Child]] =
        taxCreditsBrokerConnector.getChildren(tcNino).map(children => Child.getEligibleChildren(children))

      def createLocalDate(year: Int, month: Month, day: Int): LocalDate = LocalDate.of(year, month, day)

      def isFtnaeDate(payment: FuturePayment): Boolean =
        payment.paymentDate.isAfter(createLocalDate(now.getYear, Month.AUGUST, 31).atStartOfDay()) && payment.paymentDate.getYear == now.getYear

      def hasAFtnaePayment(paymentSummary: PaymentSummary): Boolean =
        paymentSummary.childTaxCredit.flatMap(payment => payment.paymentSeq.sortWith((a, b) => a.paymentDate.isAfter(b.paymentDate)).headOption) match {
          case Some(payment) if isFtnaeDate(payment) => true
          case _                                     => false
        }

      def getFtnaeLink(children: Seq[Child], paymentSummary: PaymentSummary): Option[String] = {
        val hasFtnaePayment: Boolean = hasAFtnaePayment(paymentSummary)
        Child.hasFtnaeChildren(children) match {
          case _ if !hasFtnaePayment => None
          case true if now.isBefore(createLocalDate(now.getYear, Month.SEPTEMBER, 1)) && hasFtnaePayment =>
            Some("/tax-credits-service/home/children-and-childcare")
          case true
            if now.isAfter(createLocalDate(now.getYear, Month.AUGUST, 31)) &&
              now.isBefore(createLocalDate(now.getYear, Month.SEPTEMBER, 8)) && hasFtnaePayment =>
            Some("/tax-credits-service/children/add-child/who-do-you-want-to-add")
          case _ => None
        }
      }

      def getInformationMessage(children: Seq[Child]): Option[String] =
        if (paymentSummary.specialCircumstances.isDefined){
          val childChildren = Child.countFtnaeChildren(children) match {
            case 0 | 1 =>  "child is"
            case _ => "children are"
          }
          Some(s"We are currently working out your payments as your $childChildren changing their education or training. This should be done by 7 September ${LocalDateTime.now.getYear}. If your $childChildren staying in education or training, update their details on GOV.UK.")
        }
        else None

      val childrenFuture        = getChildrenAge16AndUnder
      val partnerDetailsFuture  = taxCreditsBrokerConnector.getPartnerDetails(tcNino)
      val personalDetailsFuture = taxCreditsBrokerConnector.getPersonalDetails(tcNino)

      (for {
        children        <- childrenFuture
        partnerDetails  <- partnerDetailsFuture
        personalDetails <- personalDetailsFuture
      } yield {
        val childConvertedToPerson = children.map(child => Person(forename = child.firstNames, surname = child.surname))
        val ftnaeLink              = getFtnaeLink(children, paymentSummary)
        val newPayment: PaymentSummary = paymentSummary.copy(informationMessage = getInformationMessage(children)) //Update for child or children
       TaxCreditsSummaryResponse(taxCreditsSummary = Some(TaxCreditsSummary( newPayment, Some(Claimants(personalDetails, partnerDetails, childConvertedToPerson, ftnaeLink)))))
      }).recover {
        case _ => TaxCreditsSummaryResponse(taxCreditsSummary = None)
        }
    }

    def buildResponseFromPaymentSummary: Future[TaxCreditsSummaryResponse] =
      taxCreditsBrokerConnector.getPaymentSummary(tcNino).flatMap { summary =>
        if (summary.excluded.getOrElse(false)) {
          // in the context of getPaymentSummary, 'excluded == true' means a non-tax credits user
          Future successful TaxCreditsSummaryResponse(excluded = false, None)
        } else {
          buildTaxCreditsSummary(summary)
        }
      }

    taxCreditsBrokerConnector.getExclusion(tcNino).flatMap {
      case Some(exclusion) =>
        if (exclusion.excluded) Future successful TaxCreditsSummaryResponse(excluded = true, None)
        else buildResponseFromPaymentSummary
      case None => Future successful TaxCreditsSummaryResponse(excluded = false, None)
    }
  }
}
