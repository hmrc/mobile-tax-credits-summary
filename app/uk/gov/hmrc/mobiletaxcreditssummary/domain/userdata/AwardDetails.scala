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

import play.api.libs.json.Json
import uk.gov.hmrc.mobiletaxcreditssummary.domain.TaxCreditsNino

case class AwardDetails(applicationId: String,
                        isJoint: Boolean,
                        claimantNumber: Int,
                        mainApplicantNino: TaxCreditsNino,
                        availableForCOCAutomation: Boolean,
                        renewalsJourney: Boolean)

object AwardDetails {
  implicit val formats = Json.format[AwardDetails]
}

