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

package uk.gov.hmrc.mobiletaxcreditssummary.domain

import play.api.libs.json.{Json, OFormat}

case class ChangeOfCircumstanceLinks(
  changePersonalDetails:    String = "/tax-credits-service/home/your-details",
  changeJobsOrIncome:       String = "/tax-credits-service/home/jobs-and-income",
  addEditChildrenChildcare: String = "/tax-credits-service/home/children-and-childcare",
  otherChanges:             String = "/tax-credits-service/home/other-changes")

object ChangeOfCircumstanceLinks {
  implicit val format: OFormat[ChangeOfCircumstanceLinks] = Json.format[ChangeOfCircumstanceLinks]
}
