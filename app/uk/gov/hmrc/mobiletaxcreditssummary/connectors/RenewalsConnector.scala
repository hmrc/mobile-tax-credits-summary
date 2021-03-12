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

package uk.gov.hmrc.mobiletaxcreditssummary.connectors

import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.mobiletaxcreditssummary.domain.TaxCreditsNino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.LegacyClaims

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RenewalsConnector @Inject() (
  http:                                            CoreGet,
  @Named("mobile-tax-credits-renewal") serviceUrl: String) {

  def url(
    journeyId: JourneyId,
    nino:      TaxCreditsNino
  ) = s"$serviceUrl/income/${nino.value}/tax-credits/full-claimant-details?journeyId=$journeyId"

  def getRenewals(
    journeyId:              JourneyId,
    nino:                   TaxCreditsNino
  )(implicit headerCarrier: HeaderCarrier,
    ex:                     ExecutionContext
  ): Future[Option[LegacyClaims]] = {
    println("HEADERS = " + headerCarrier)

    http.GET[Option[LegacyClaims]](url(journeyId, nino)).recover {
      case _: NotFoundException => None
    }
  }
}
