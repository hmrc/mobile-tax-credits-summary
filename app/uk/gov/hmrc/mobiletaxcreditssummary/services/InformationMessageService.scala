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

import java.time.{LocalDate, Month}

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.{FTNAE, FuturePayment, InformationMessage, MessageLink, NewRate, OldRate, PXP5, PaymentSummary, SpecialCircumstance}
import uk.gov.hmrc.mobiletaxcreditssummary.utils.LocalDateProvider

@Singleton
class InformationMessageService @Inject() (localDateProvider: LocalDateProvider) {

  val now: LocalDate = localDateProvider.now

  def getMessageLink(paymentSummary: PaymentSummary): Option[MessageLink] =
    paymentSummary.specialCircumstances match {

      case Some(FTNAE) =>
        val hasFtnaePayment: Boolean = hasAFtnaePayment(paymentSummary)

        if (hasFtnaePayment) {
          if (now.isBefore(createLocalDate(now.getYear, Month.SEPTEMBER, 1))) {
            Some(
              MessageLink(preFtnaeDeadline = true, "Update details", "/tax-credits-service/home/children-and-childcare")
            )
          } else if (now.isAfter(createLocalDate(now.getYear, Month.AUGUST, 31)) &&
                     now.isBefore(createLocalDate(now.getYear, Month.SEPTEMBER, 8))) {
            Some(
              MessageLink(preFtnaeDeadline = false,
                          "Update details",
                          "/tax-credits-service/children/add-child/who-do-you-want-to-add")
            )
          } else None
        } else None

      case Some(NewRate) | Some(OldRate) =>
        Some(
          MessageLink(
            preFtnaeDeadline = false,
            "Contact tax credits",
            "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/tax-credits-enquiries"
          )
        )

      case _ => None
    }

  def getInformationMessage(
    specialCircumstances: Option[SpecialCircumstance],
    isMultipleFTNAE:      Boolean
  ): Option[InformationMessage] =
    specialCircumstances match {

      case Some(FTNAE) =>
        val childChildren = if (isMultipleFTNAE) "children are" else "child is"

        if (now.isBefore(createLocalDate(now.getYear, Month.SEPTEMBER, 1))) {
          Some(
            InformationMessage(
              f"We are currently working out your payments as your $childChildren changing their education or training. This should be done by 7 September ${now.getYear}.",
              f"If your $childChildren staying in education or training, you should update their details."
            )
          )
        } else if (now.isAfter(createLocalDate(now.getYear, Month.AUGUST, 31)) &&
                   now.isBefore(createLocalDate(now.getYear, Month.SEPTEMBER, 8))) {
          Some(
            InformationMessage(
              f"We are currently working out your payments as your $childChildren changing their education or training. This should be done by 7 September ${now.getYear}.",
              f"If you have let us know that your $childChildren staying in education or training, they will be added back automatically. Otherwise, you can add them back to your claim."
            )
          )
        } else {
          None
        }

      case Some(OldRate) =>
        Some(
          InformationMessage(
            "Tax credit payment amounts increased on 6 April",
            "You should only contact HMRC if you have not received your revised payment by 18 May."
          )
        )

      case Some(NewRate) =>
        Some(
          InformationMessage(
            "Tax credit payment amounts increased on 6 April",
            "Your payments have been revised. You should only contact HMRC if there is a problem with your revised payments."
          )
        )

      case Some(PXP5) =>
        Some(
          InformationMessage(
            "Your payments are being processed",
            "It can take up to 2 days for your payments to show."
          )
        )

      case _ => None
    }

  private def isFtnaeDate(payment: FuturePayment): Boolean =
    payment.paymentDate.isAfter(createLocalDate(now.getYear, Month.AUGUST, 31).atStartOfDay()) && payment.paymentDate.getYear == now.getYear

  // Payments after August 31st are hidden if special circumstances is "FTNAE" to work out if payments are FTNAE we check there
  // are no payments after the 31st and this is coupled with other logic in the match
  private def hasAFtnaePayment(paymentSummary: PaymentSummary): Boolean =
    paymentSummary.childTaxCredit match {
      case None if (now.isBefore(createLocalDate(now.getYear, Month.SEPTEMBER, 8))) => true
      case None                                                                     => false
      case Some(ctc)                                                                => ctc.paymentSeq.count(payment => isFtnaeDate(payment)) == 0
    }

  private def createLocalDate(
    year:  Int,
    month: Month,
    day:   Int
  ): LocalDate = LocalDate.of(year, month, day)

}
