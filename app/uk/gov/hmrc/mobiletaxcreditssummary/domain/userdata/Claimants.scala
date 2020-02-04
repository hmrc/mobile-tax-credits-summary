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

package uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata

import play.api.libs.json.{Json, OFormat}

case class Claimants(
                      personalDetails: Person,
                      partnerDetails: Option[Person],
                      children: Seq[Person],
                      ftnaeLink: Option[FtnaeLink] = None,
                      reportActualProfit: Option[ReportActualProfit] = None)

object Claimants {
  implicit val format: OFormat[Claimants] = Json.format[Claimants]
}

case class ReportActualProfit(
                               link: String,
                               endDate: String,
                               userMustReportIncome: Boolean,
                               partnerMustReportIncome: Boolean)

object ReportActualProfit {
  implicit val formats: OFormat[ReportActualProfit] = Json.format[ReportActualProfit]
}

case class FtnaeLink(
                      preFtnaeDeadline: Boolean,
                      link: String)

object FtnaeLink {
  implicit val formats: OFormat[FtnaeLink] = Json.format[FtnaeLink]
}
