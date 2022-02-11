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

import java.time.LocalDateTime

import play.api.Logger
import play.api.libs.functional.FunctionalBuilder
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.PaymentReadWriteUtils.{paymentReads, paymentWrites}

case class InformationMessage(
  title:   String,
  message: String)

object InformationMessage {
  implicit val formats: OFormat[InformationMessage] = Json.format[InformationMessage]
}

case class PaymentSummary(
  workingTaxCredit:     Option[PaymentSection],
  childTaxCredit:       Option[PaymentSection],
  paymentEnabled:       Option[Boolean] = Some(false),
  specialCircumstances: Option[SpecialCircumstance] = None,
  excluded:             Option[Boolean] = None,
  isMultipleFTNAE:      Option[Boolean] = None,
  informationMessage:   Option[InformationMessage] = None) {

  def totalsByDate: Option[List[Total]] =
    total(
      workingTaxCredit.map(_.paymentSeq).getOrElse(Seq.empty)
      ++ childTaxCredit.map(_.paymentSeq).getOrElse(Seq.empty)
    )

  def previousTotalsByDate: Option[List[Total]] =
    total(
      workingTaxCredit.flatMap(_.previousPaymentSeq).getOrElse(Seq.empty)
      ++ childTaxCredit.flatMap(_.previousPaymentSeq).getOrElse(Seq.empty)
    )

  private def total(payments: Seq[Payment]): Option[List[Total]] =
    if (payments.isEmpty) None
    else {
      val distinctDate = payments.map(_.paymentDate).distinct.sortWith((d1, d2) => d1.isBefore(d2))
      Option(
        distinctDate
          .map(date =>
            Total(payments
                    .filter(_.paymentDate == date)
                    .foldLeft(BigDecimal(0))(_ + _.amount),
                  date)
          )
          .toList
      )
    }
}

sealed trait SpecialCircumstance

case object OldRate extends SpecialCircumstance

case object NewRate extends SpecialCircumstance

case object PXP5 extends SpecialCircumstance

case object UnknownCircumstance extends SpecialCircumstance

object SpecialCircumstance {
  val logger: Logger = Logger(this.getClass)

  implicit val reads: Reads[SpecialCircumstance] = Reads {
    case JsString("OLD RATE") => JsSuccess(OldRate)
    case JsString("NEW RATE") => JsSuccess(NewRate)
    case JsString("PXP5")     => JsSuccess(PXP5)
    case e =>
      logger.warn(s"Unknown special circumstance received: $e")
      JsSuccess(UnknownCircumstance)
  }

  implicit val writes: Writes[SpecialCircumstance] = Writes {
    case OldRate             => JsString("OLD RATE")
    case NewRate             => JsString("NEW RATE")
    case PXP5                => JsString("PXP5")
    case UnknownCircumstance => JsNull
    case e                   => throw new IllegalStateException(s"$e is an Invalid SpecialCircumstance")
  }
}

case class PaymentSection(
  paymentSeq:         List[FuturePayment],
  paymentFrequency:   String,
  previousPaymentSeq: Option[List[PastPayment]] = None)

sealed trait Payment {
  val amount:        BigDecimal
  val paymentDate:   LocalDateTime
  val oneOffPayment: Boolean
  val holidayType:   Option[String]
  val earlyPayment: Boolean = holidayType.isDefined

  def oneOffPaymentText: String

  def bankHolidayPaymentText: String

  def explanatoryText: Option[String] =
    if (oneOffPayment) Some(oneOffPaymentText)
    else if (earlyPayment) Some(bankHolidayPaymentText)
    else None
}

case class FuturePayment(
  amount:        BigDecimal,
  paymentDate:   LocalDateTime,
  oneOffPayment: Boolean,
  holidayType:   Option[String] = None)
    extends Payment {

  override def oneOffPaymentText: String =
    "One-off payment because of a recent change to help you get the right amount of tax credits."

  override def bankHolidayPaymentText: String = "Your payment is early because of UK bank holidays."
}

case class PastPayment(
  amount:        BigDecimal,
  paymentDate:   LocalDateTime,
  oneOffPayment: Boolean,
  holidayType:   Option[String] = None)
    extends Payment {

  override def oneOffPaymentText: String =
    "One-off payment because of a recent change to help you get the right amount of tax credits."

  override def bankHolidayPaymentText: String = "Your payment was early because of UK bank holidays."
}

case class Total(
  amount:      BigDecimal,
  paymentDate: LocalDateTime)

object PaymentReadWriteUtils {

  val paymentReads: FunctionalBuilder[Reads]#CanBuild4[BigDecimal, LocalDateTime, Boolean, Option[String]] =
    (JsPath \ "amount").read[BigDecimal] and
    (JsPath \ "paymentDate").read[LocalDateTime] and
    (JsPath \ "oneOffPayment").read[Boolean] and
    (JsPath \ "holidayType").readNullable[String]

  val paymentWrites: OWrites[(BigDecimal, LocalDateTime, Boolean, Option[String], Boolean, Option[String])] = (
    (__ \ "amount").write[BigDecimal] ~
    (__ \ "paymentDate").write[LocalDateTime] ~
    (__ \ "oneOffPayment").write[Boolean] ~
    (__ \ "holidayType").writeNullable[String] ~
    (__ \ "earlyPayment").write[Boolean] ~
    (__ \ "explanatoryText").writeNullable[String]
  ).tupled
}

object FuturePayment {
  implicit val reads: Reads[FuturePayment] = paymentReads(FuturePayment.apply _)

  implicit val writes: Writes[FuturePayment] = new Writes[FuturePayment] {

    def writes(payment: FuturePayment): JsObject =
      paymentWrites.writes(
        (payment.amount,
         payment.paymentDate,
         payment.oneOffPayment,
         payment.holidayType,
         payment.earlyPayment,
         payment.explanatoryText)
      )
  }
}

object PastPayment {
  implicit val reads: Reads[PastPayment] = paymentReads(PastPayment.apply _)

  implicit val writes: Writes[PastPayment] = new Writes[PastPayment] {

    def writes(payment: PastPayment): JsObject =
      paymentWrites.writes(
        (payment.amount,
         payment.paymentDate,
         payment.oneOffPayment,
         payment.holidayType,
         payment.earlyPayment,
         payment.explanatoryText)
      )
  }
}

object PaymentSection {
  implicit val formats: OFormat[PaymentSection] = Json.format[PaymentSection]
}

object Total {
  implicit val formats: OFormat[Total] = Json.format[Total]
}

object PaymentSummary {

  def key: String = "payment-data"

  implicit val reads: Reads[PaymentSummary] = (
    (JsPath \ "workingTaxCredit").readNullable[PaymentSection] and
    (JsPath \ "childTaxCredit").readNullable[PaymentSection] and
    (JsPath \ "paymentEnabled").readNullable[Boolean] and
    (JsPath \ "specialCircumstances").readNullable[SpecialCircumstance] and
    (JsPath \ "excluded").readNullable[Boolean] and
    (JsPath \ "isMultipleFTNAE").readNullable[Boolean] and
    (JsPath \ "informationMessage").readNullable[InformationMessage]
  )(PaymentSummary.apply _)

  implicit val writes: Writes[PaymentSummary] = new Writes[PaymentSummary] {

    def writes(paymentSummary: PaymentSummary): JsObject = {
      val paymentSummaryWrites = (
        (__ \ "workingTaxCredit").writeNullable[PaymentSection] ~
        (__ \ "childTaxCredit").writeNullable[PaymentSection] ~
        (__ \ "paymentEnabled").writeNullable[Boolean] ~
        (__ \ "specialCircumstances").writeNullable[SpecialCircumstance] ~
        (__ \ "excluded").writeNullable[Boolean] ~
        (__ \ "informationMessage").writeNullable[InformationMessage] ~
        (__ \ "totalsByDate").writeNullable[List[Total]] ~
        (__ \ "previousTotalsByDate").writeNullable[List[Total]]
      ).tupled

      paymentSummaryWrites.writes(
        (
          paymentSummary.workingTaxCredit,
          paymentSummary.childTaxCredit,
          paymentSummary.paymentEnabled,
          paymentSummary.specialCircumstances,
          paymentSummary.excluded,
          paymentSummary.informationMessage,
          paymentSummary.totalsByDate,
          paymentSummary.previousTotalsByDate
        )
      )
    }
  }
}
