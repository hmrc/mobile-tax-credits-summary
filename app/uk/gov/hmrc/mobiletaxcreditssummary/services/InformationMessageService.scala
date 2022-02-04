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

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.{InformationMessage, MessageLink, NewRate, OldRate, PXP5, PaymentSummary, SpecialCircumstance}
import uk.gov.hmrc.mobiletaxcreditssummary.utils.LocalDateProvider

@Singleton
class InformationMessageService @Inject() (localDateProvider: LocalDateProvider) {

  val now: LocalDate = localDateProvider.now

  def getMessageLink(paymentSummary: PaymentSummary): Option[MessageLink] =
    paymentSummary.specialCircumstances match {

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

  def getInformationMessage(specialCircumstances: Option[SpecialCircumstance]): Option[InformationMessage] =
    specialCircumstances match {

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

}
