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

import java.time.LocalDate

import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobiletaxcreditssummary.connectors.TaxCreditsBrokerConnector
import uk.gov.hmrc.mobiletaxcreditssummary.domain.TaxCreditsNino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._

import scala.concurrent.{ExecutionContext, Future}

trait TaxCreditsBrokerConnectorMock extends MockFactory {

  val thisYear:            Int       = LocalDate.now().getYear
  val expectedNextDueDate: LocalDate = LocalDate.parse(f"$thisYear-07-16")
  val paymentWithFtnae:    LocalDate = LocalDate.parse(f"$thisYear-08-10")

  val expectedPaymentWTC: FuturePayment =
    FuturePayment(160.34, expectedNextDueDate, oneOffPayment = false)

  val expectedPaymentCTC: FuturePayment =
    FuturePayment(140.12, expectedNextDueDate, oneOffPayment = false)

  val expectedFtnaePaymentCTC: FuturePayment =
    FuturePayment(150.12, paymentWithFtnae, oneOffPayment = false)
  val paymentSectionCTC: PaymentSection = PaymentSection(List(expectedPaymentCTC), "WEEKLY")

  val paymentSectionCTCWithFtnae: PaymentSection =
    PaymentSection(List(expectedFtnaePaymentCTC, expectedPaymentCTC), "WEEKLY")
  val paymentSectionWTC: PaymentSection = PaymentSection(List(expectedPaymentWTC), "WEEKLY")

  val paymentSummary: PaymentSummary =
    PaymentSummary(Some(paymentSectionWTC), Some(paymentSectionCTC), paymentEnabled = Some(true))

  def paymentSummaryWithInfoMessage(
    specialCircumstance: Option[SpecialCircumstance],
    informationMessage:  Option[InformationMessage]
  ): PaymentSummary =
    PaymentSummary(Some(paymentSectionWTC),
                   Some(paymentSectionCTC),
                   paymentEnabled = Some(true),
                   specialCircumstance,
                   informationMessage = informationMessage)

  val actualIncomeApp1Eligible: ClaimActualIncomeEligibilityStatus = ClaimActualIncomeEligibilityStatus(
    ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
    ClaimActualIncomeEligibilityStatus.APPLICANT_NOT_APPLICABLE
  )

  val actualIncomeApp2Eligible: ClaimActualIncomeEligibilityStatus = ClaimActualIncomeEligibilityStatus(
    ClaimActualIncomeEligibilityStatus.APPLICANT_NOT_APPLICABLE,
    ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED
  )

  val actualIncomeBothEligible: ClaimActualIncomeEligibilityStatus =
    ClaimActualIncomeEligibilityStatus(ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
                                       ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED)

  val actualIncomeNeitherEligible: ClaimActualIncomeEligibilityStatus = ClaimActualIncomeEligibilityStatus(
    ClaimActualIncomeEligibilityStatus.APPLICANT_NOT_APPLICABLE,
    ClaimActualIncomeEligibilityStatus.APPLICANT_NOT_APPLICABLE
  )

  val actualIncomeOneExcluded: ClaimActualIncomeEligibilityStatus = ClaimActualIncomeEligibilityStatus(
    ClaimActualIncomeEligibilityStatus.APPLICANT_ALLOWED,
    ClaimActualIncomeEligibilityStatus.APPLICANT_EXCLUDED
  )

  val AGE16:         LocalDate = LocalDate.now.minusYears(16)
  val AGE15:         LocalDate = LocalDate.now.minusYears(15)
  val AGE13:         LocalDate = LocalDate.now.minusYears(13)
  val AGE21:         LocalDate = LocalDate.now.minusYears(21)
  val DECEASED_DATE: LocalDate = LocalDate.now.minusYears(1)

  val SarahSmith: Child = Child("Sarah", "Smith", AGE16, hasFTNAE = false, hasConnexions = false, isActive = true, None)

  val SarahSmithFtnae: Child =
    Child("Sarah", "Smith", AGE16, hasFTNAE = true, hasConnexions = false, isActive = true, None)

  val JosephSmith: Child =
    Child("Joseph", "Smith", AGE15, hasFTNAE = false, hasConnexions = false, isActive = true, None)
  val MarySmith:  Child = Child("Mary", "Smith", AGE13, hasFTNAE  = false, hasConnexions = false, isActive = true, None)
  val JennySmith: Child = Child("Jenny", "Smith", AGE21, hasFTNAE = false, hasConnexions = false, isActive = true, None)

  val PeterSmith: Child =
    Child("Peter", "Smith", AGE13, hasFTNAE = false, hasConnexions = false, isActive = false, Some(DECEASED_DATE))

  val SimonSmith: Child =
    Child("Simon", "Smith", AGE13, hasFTNAE = false, hasConnexions = false, isActive = true, Some(DECEASED_DATE))

  val personalDetails: Person = Person(forename = "firstname", surname = "surname")

  val partnerDetails: Person = Person(forename = "forename", surname = "surname")

  val claimants: Claimants = Claimants(
    personalDetails,
    Some(partnerDetails),
    Seq(Person(forename = "Sarah", surname  = "Smith"),
        Person(forename = "Joseph", surname = "Smith"),
        Person(forename = "Mary", surname   = "Smith"))
  )

  def claimantsFtnae(link: Option[MessageLink] = None): Claimants = Claimants(
    personalDetails,
    Some(partnerDetails),
    Seq(Person(forename = "Sarah", surname  = "Smith"),
        Person(forename = "Joseph", surname = "Smith"),
        Person(forename = "Mary", surname   = "Smith")),
    link
  )

  def claimantsMultipleFtnae(link: Option[MessageLink] = None): Claimants = Claimants(
    personalDetails,
    Some(partnerDetails),
    Seq(Person(forename = "Sarah", surname = "Smith"),
        Person(forename = "Sarah", surname = "Smith"),
        Person(forename = "Sarah", surname = "Smith")),
    link
  )

  val claimantsNoPartnerDetails: Claimants = Claimants(personalDetails,
                                                       None,
                                                       Seq(Person(forename = "Sarah", surname  = "Smith"),
                                                           Person(forename = "Joseph", surname = "Smith"),
                                                           Person(forename = "Mary", surname   = "Smith")))

  val claimantsNoChildren: Claimants = Claimants(personalDetails, Some(partnerDetails), Seq.empty)

  def claimantsWithReportActualProfit(reportActualProfit: ReportActualProfit): Claimants =
    claimants.copy(reportActualProfit = Some(reportActualProfit))

  def mockTaxCreditsBrokerConnectorGetExclusion(
    response:                           Option[Exclusion],
    nino:                               TaxCreditsNino
  )(implicit taxCreditsBrokerConnector: TaxCreditsBrokerConnector
  ): Unit =
    (taxCreditsBrokerConnector
      .getExclusion(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(Future successful response)

  def mockTaxCreditsBrokerConnectorGetExclusionFailure(
    response:                           Exception,
    nino:                               TaxCreditsNino
  )(implicit taxCreditsBrokerConnector: TaxCreditsBrokerConnector
  ): Unit =
    (taxCreditsBrokerConnector
      .getExclusion(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(Future failed response)

  def mockTaxCreditsBrokerConnectorGetDashboardData(
    response:                           DashboardData,
    nino:                               TaxCreditsNino
  )(implicit taxCreditsBrokerConnector: TaxCreditsBrokerConnector
  ): Unit =
    (taxCreditsBrokerConnector
      .getDashboardData(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(Future successful Some(response))

  def mockTaxCreditsBrokerConnectorGetDashboardDataFailure(
    response:                           Exception,
    nino:                               TaxCreditsNino
  )(implicit taxCreditsBrokerConnector: TaxCreditsBrokerConnector
  ): Unit =
    (taxCreditsBrokerConnector
      .getDashboardData(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(Future failed response)
}
