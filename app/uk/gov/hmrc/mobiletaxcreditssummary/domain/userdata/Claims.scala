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

import play.api.libs.json.{Format, Json}

case class LegacyClaim(
  household: Household,
  renewal:   LegacyRenewal)

object LegacyClaim {
  implicit val formats: Format[LegacyClaim] = Json.format[LegacyClaim]
}

case class LegacyClaims(references: Option[Seq[LegacyClaim]])

object LegacyClaims {
  implicit val formats: Format[LegacyClaims] = Json.format[LegacyClaims]
}

case class Applicant(
  nino:                            String,
  title:                           String,
  firstForename:                   String,
  secondForename:                  Option[String],
  surname:                         String,
  previousYearRtiEmployedEarnings: Option[Double])

object Applicant {
  implicit val formats: Format[Applicant] = Json.format[Applicant]
}

case class Household(
  barcodeReference:    String,
  applicationID:       String,
  applicant1:          Applicant,
  applicant2:          Option[Applicant],
  householdCeasedDate: Option[String],
  householdEndReason:  Option[String])

object Household {
  implicit val formats: Format[Household] = Json.format[Household]
}

case class LegacyRenewal(
  awardStartDate:                  Option[String],
  awardEndDate:                    Option[String],
  renewalStatus:                   Option[String],
  renewalNoticeIssuedDate:         Option[String],
  renewalNoticeFirstSpecifiedDate: Option[String],
  renewalFormType:                 Option[String] = None)

object LegacyRenewal {
  implicit val formats: Format[LegacyRenewal] = Json.format[LegacyRenewal]
}

object LegacyRenewalStatus {

  val AWAITING_BARCODE         = "AWAITING_BARCODE"
  val NOT_SUBMITTED            = "NOT_SUBMITTED"
  val SUBMITTED_AND_PROCESSING = "SUBMITTED_AND_PROCESSING"
  val COMPLETE                 = "COMPLETE"

}
