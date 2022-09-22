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

package uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.mobiletaxcreditssummary.domain.ChangeOfCircumstanceLinks

import java.time.{LocalDate, ZoneId, ZonedDateTime}

case class TaxCreditsSummaryResponse(
  excluded:                  Boolean = false,
  taxCreditsSummary:         Option[TaxCreditsSummary],
  changeOfCircumstanceLinks: Option[ChangeOfCircumstanceLinks] = None) {
}

object TaxCreditsSummaryResponse {
  implicit val format: OFormat[TaxCreditsSummaryResponse] = Json.format[TaxCreditsSummaryResponse]
}

//case class PaymentSectionResponse(
//  paymentSeq:         List[FuturePaymentResponse],
//  paymentFrequency:   String,
//  previousPaymentSeq: Option[List[PastPaymentResponse]] = None)
//
//sealed trait PaymentResponse {
//  val amount:        BigDecimal
//  val paymentDate:   ZonedDateTime
//  val oneOffPayment: Boolean
//  val holidayType:   Option[String]
//  val earlyPayment: Boolean = holidayType.isDefined
//}
//
//case class FuturePaymentResponse(
//  amount:        BigDecimal,
//  paymentDate:   ZonedDateTime,
//  oneOffPayment: Boolean,
//  holidayType:   Option[String] = None)
//    extends PaymentResponse {
//
//  def toFuturePaymentResponse(futurePayment: FuturePayment): FuturePaymentResponse =
//    FuturePaymentResponse(
//      amount        = futurePayment.amount,
//      paymentDate   = futurePayment.paymentDate.atStartOfDay(ZoneId.systemDefault()),
//      oneOffPayment = futurePayment.oneOffPayment,
//      holidayType   = futurePayment.holidayType
//    )
//}
//
//case class PastPaymentResponse(
//  amount:        BigDecimal,
//  paymentDate:   ZonedDateTime,
//  oneOffPayment: Boolean,
//  holidayType:   Option[String] = None)
//    extends PaymentResponse {
//
//  def toPastPaymentResponse(pastPayment: PastPayment): PastPaymentResponse =
//    PastPaymentResponse(
//      amount        = pastPayment.amount,
//      paymentDate   = pastPayment.paymentDate.atStartOfDay(ZoneId.systemDefault()),
//      oneOffPayment = pastPayment.oneOffPayment,
//      holidayType   = pastPayment.holidayType
//    )
//}
//
//object FuturePaymentResponse {
//  implicit val formats: OFormat[FuturePayment] = Json.format[FuturePayment]
//}
//
//object PastPaymentResponse {
//  implicit val formats: OFormat[PastPayment] = Json.format[PastPayment]
//
//}
//
//object PaymentSectionResponse {
//  implicit val formats: OFormat[PaymentSection] = Json.format[PaymentSection]
//}
