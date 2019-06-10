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

package uk.gov.hmrc.mobiletaxcreditssummary.mocks

import java.time.{LocalDate, LocalTime}

import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobiletaxcreditssummary.connectors.TaxCreditsBrokerConnector
import uk.gov.hmrc.mobiletaxcreditssummary.domain.TaxCreditsNino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._

import scala.concurrent.{ExecutionContext, Future}

trait TaxCreditsBrokerConnectorMock extends MockFactory {

  val thisYear: Int = LocalDate.now().getYear
  val expectedNextDueDate: LocalDate = LocalDate.parse(f"$thisYear-07-16")
  val paymentWithFtnae: LocalDate = LocalDate.parse(f"$thisYear-09-10")
  val expectedPaymentWTC = FuturePayment(160.34, expectedNextDueDate.atStartOfDay(), oneOffPayment = false)
  val expectedPaymentCTC = FuturePayment(140.12, expectedNextDueDate.atStartOfDay(), oneOffPayment = false)
  val expectedFtnaePaymentCTC = FuturePayment(150.12, paymentWithFtnae.atStartOfDay(), oneOffPayment = false)
  val paymentSectionCTC  = PaymentSection(List(expectedPaymentCTC), "WEEKLY")
  val paymentSectionCTCWithFtnae  = PaymentSection(List(expectedFtnaePaymentCTC,expectedPaymentCTC), "WEEKLY")
  val paymentSectionWTC  = PaymentSection(List(expectedPaymentWTC), "WEEKLY")
  val paymentSummary     = PaymentSummary(Some(paymentSectionWTC), Some(paymentSectionCTC), paymentEnabled = Some(true))
  val paymentSummaryFtnae     = PaymentSummary(
    workingTaxCredit = Some(paymentSectionWTC),
    childTaxCredit=  Some(paymentSectionCTCWithFtnae),
    paymentEnabled = Some(true),
    specialCircumstances = Some("FTNAE"),
    informationMessage =  Some(f"We are currently working out your payments as your child is changing their education or training. This should be done by 7 September $thisYear. If your child is staying in education or training, update their details on GOV.UK.")
  )

  val paymentSummaryMultipleFtnae     = PaymentSummary(
    workingTaxCredit = Some(paymentSectionWTC),
    childTaxCredit=  Some(paymentSectionCTCWithFtnae),
    paymentEnabled = Some(true),
    specialCircumstances = Some("FTNAE"),
    informationMessage =  Some(f"We are currently working out your payments as your children are changing their education or training. This should be done by 7 September $thisYear. If your children are staying in education or training, update their details on GOV.UK.")
  )

  val AGE16:         LocalDate = LocalDate.now.minusYears(16)
  val AGE15:         LocalDate = LocalDate.now.minusYears(15)
  val AGE13:         LocalDate = LocalDate.now.minusYears(13)
  val AGE21:         LocalDate = LocalDate.now.minusYears(21)
  val DECEASED_DATE: LocalDate = LocalDate.now.minusYears(1)

  val SarahSmith  = Child("Sarah", "Smith", AGE16, hasFTNAE  = false, hasConnections = false, isActive = true, None)
  val SarahSmithFtnae  = Child("Sarah", "Smith", AGE16, hasFTNAE  = true, hasConnections = false, isActive = true, None)
  val JosephSmith = Child("Joseph", "Smith", AGE15, hasFTNAE = false, hasConnections = false, isActive = true, None)
  val MarySmith   = Child("Mary", "Smith", AGE13, hasFTNAE   = false, hasConnections = false, isActive = true, None)
  val JennySmith  = Child("Jenny", "Smith", AGE21, hasFTNAE  = false, hasConnections = false, isActive = true, None)
  val PeterSmith  = Child("Peter", "Smith", AGE13, hasFTNAE  = false, hasConnections = false, isActive = false, Some(DECEASED_DATE))
  val SimonSmith  = Child("Simon", "Smith", AGE13, hasFTNAE  = false, hasConnections = false, isActive = true, Some(DECEASED_DATE))

  val personalDetails = Person(forename = "firstname", surname = "surname")

  val partnerDetails = Person(forename = "forename", surname = "surname")

  val claimants = Claimants(
    personalDetails,
    Some(partnerDetails),
    Seq(Person(forename = "Sarah", surname = "Smith"), Person(forename = "Joseph", surname = "Smith"), Person(forename = "Mary", surname = "Smith"))
  )

  def claimantsFTNAE(link: Option[String] = None) = Claimants(
    personalDetails,
    Some(partnerDetails),
    Seq(Person(forename = "Sarah", surname = "Smith"), Person(forename = "Joseph", surname = "Smith"), Person(forename = "Mary", surname = "Smith")),
    link
  )

  def claimantsMultipleFTNAE(link: Option[String] = None) = Claimants(
    personalDetails,
    Some(partnerDetails),
    Seq(Person(forename = "Sarah", surname = "Smith"), Person(forename = "Sarah", surname = "Smith"), Person(forename = "Sarah", surname = "Smith")),
    link
  )

  val claimantsNoPartnerDetails = Claimants(
    personalDetails,
    None,
    Seq(Person(forename = "Sarah", surname = "Smith"), Person(forename = "Joseph", surname = "Smith"), Person(forename = "Mary", surname = "Smith")))

  val claimantsNoChildren = Claimants(personalDetails, Some(partnerDetails), Seq.empty)

  def mockTaxCreditsBrokerConnectorGetExclusion(response: Option[Exclusion], nino: TaxCreditsNino)(
    implicit taxCreditsBrokerConnector:                   TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector
      .getExclusion(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(Future successful response)

  def mockTaxCreditsBrokerConnectorGetExclusionFailure(response: Exception, nino: TaxCreditsNino)(
    implicit taxCreditsBrokerConnector:                          TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector
      .getExclusion(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(Future failed response)

  def mockTaxCreditsBrokerConnectorGetPaymentSummary(response: PaymentSummary, nino: TaxCreditsNino)(
    implicit taxCreditsBrokerConnector:                        TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector
      .getPaymentSummary(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(Future successful response)

  def mockTaxCreditsBrokerConnectorGetPaymentFailure(response: Exception, nino: TaxCreditsNino)(
    implicit taxCreditsBrokerConnector:                        TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector
      .getPaymentSummary(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(Future failed response)

  def mockTaxCreditsBrokerConnectorGetChildren(response: Seq[Child], nino: TaxCreditsNino)(
    implicit taxCreditsBrokerConnector:                  TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector
      .getChildren(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(Future successful response)

  def mockTaxCreditsBrokerConnectorGetChildrenFailure(response: Exception, nino: TaxCreditsNino)(
    implicit taxCreditsBrokerConnector:                         TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector
      .getChildren(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(Future failed response)

  def mockTaxCreditsBrokerConnectorGetPartnerDetails(response: Option[Person], nino: TaxCreditsNino)(
    implicit taxCreditsBrokerConnector:                        TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector
      .getPartnerDetails(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(Future successful response)

  def mockTaxCreditsBrokerConnectorGetPartnerDetailsFailure(response: Exception, nino: TaxCreditsNino)(
    implicit taxCreditsBrokerConnector:                               TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector
      .getPartnerDetails(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(Future failed response)

  def mockTaxCreditsBrokerConnectorGetPersonalDetails(response: Person, nino: TaxCreditsNino)(
    implicit taxCreditsBrokerConnector:                         TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector
      .getPersonalDetails(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(Future successful response)

  def mockTaxCreditsBrokerConnectorGetPersonalDetailsFailure(response: Exception, nino: TaxCreditsNino)(
    implicit taxCreditsBrokerConnector:                                TaxCreditsBrokerConnector): Unit =
    (taxCreditsBrokerConnector
      .getPersonalDetails(_: TaxCreditsNino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(Future failed response)
}
