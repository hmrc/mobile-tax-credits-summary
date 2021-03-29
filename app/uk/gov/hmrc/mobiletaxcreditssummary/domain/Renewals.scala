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

package uk.gov.hmrc.mobiletaxcreditssummary.domain

import play.api.libs.json.{Format, JsString, JsSuccess, Json, Reads, Writes}

import java.time.LocalDate

case class Renewals(
  status:              RenewalStatus,
  totalClaims:         Int,
  claimsSubmitted:     Int,
  packReceivedDate:    String,
  renewalsEndDate:     String,
  viewRenewalsEndDate: String,
  householdBreakdown:  Boolean = false,
  inGracePeriod:       Boolean = false,
  currentYear:         String = LocalDate.now().getYear.toString
  )

object Renewals {
  implicit val formats: Format[Renewals] = Json.format[Renewals]
}

sealed trait RenewalStatus

case object PackNotSent extends RenewalStatus
case object AutoRenewal extends RenewalStatus
case object AutoRenewalMultiple extends RenewalStatus
case object NotStartedSingle extends RenewalStatus
case object NotStartedMultiple extends RenewalStatus
case object RenewalSubmitted extends RenewalStatus
case object OneNotStartedMultiple extends RenewalStatus
case object Complete extends RenewalStatus
case object ViewOnly extends RenewalStatus

object RenewalStatus {

  implicit val reads: Reads[RenewalStatus] = Reads {
    case JsString("pack_not_sent")            => JsSuccess(PackNotSent)
    case JsString("auto_renewal")             => JsSuccess(AutoRenewal)
    case JsString("auto_renewal_multiple")    => JsSuccess(AutoRenewalMultiple)
    case JsString("not_started_single")       => JsSuccess(NotStartedSingle)
    case JsString("renewal_submitted")        => JsSuccess(RenewalSubmitted)
    case JsString("one_not_started_multiple") => JsSuccess(OneNotStartedMultiple)
    case JsString("not_started_multiple")     => JsSuccess(NotStartedMultiple)
    case JsString("complete")                 => JsSuccess(Complete)
    case JsString("view_only")                => JsSuccess(ViewOnly)
  }

  implicit val writes: Writes[RenewalStatus] = Writes {
    case PackNotSent           => JsString("pack_not_sent")
    case AutoRenewal           => JsString("auto_renewal")
    case AutoRenewalMultiple   => JsString("auto_renewal_multiple")
    case NotStartedSingle      => JsString("not_started_single")
    case RenewalSubmitted      => JsString("renewal_submitted")
    case OneNotStartedMultiple => JsString("one_not_started_multiple")
    case NotStartedMultiple    => JsString("not_started_multiple")
    case Complete              => JsString("complete")
    case ViewOnly              => JsString("view_only")
    case e                     => throw new IllegalStateException(s"$e is an Invalid Renewal Status")
  }
}
