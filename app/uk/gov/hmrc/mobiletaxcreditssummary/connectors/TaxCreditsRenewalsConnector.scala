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

package uk.gov.hmrc.mobiletaxcreditssummary.connectors

import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, StringContextOps}
import uk.gov.hmrc.mobiletaxcreditssummary.domain.TaxCreditsNino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.LegacyClaims
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxCreditsRenewalsConnector @Inject() (
  http:                                            HttpClientV2,
  @Named("mobile-tax-credits-renewal") serviceUrl: String) {

  val logger: Logger = Logger(this.getClass)

  def url(
    journeyId: JourneyId,
    nino:      TaxCreditsNino
  ): URL = url"${s"$serviceUrl/income/${nino.value}/tax-credits/full-claimant-details?journeyId=$journeyId"}"

  def getRenewals(
    journeyId:              JourneyId,
    nino:                   TaxCreditsNino
  )(implicit headerCarrier: HeaderCarrier,
    ex:                     ExecutionContext
  ): Future[Option[LegacyClaims]] =
    http
      .get(url(journeyId, nino))
      .execute[Option[LegacyClaims]]
      .recover {
        case _: NotFoundException => None
        case e =>
          logger.warn(s"Call to mobile-tax-credits-renewals failed:\n $e \n No renewals information available.")
          None
      }
}
