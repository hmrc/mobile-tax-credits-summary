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
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, StringContextOps}
import uk.gov.hmrc.mobiletaxcreditssummary.domain.TaxCreditsNino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxCreditsBrokerConnector @Inject() (
  http:                                    HttpClientV2,
  @Named("tax-credits-broker") serviceUrl: String) {

  def url(
    nino:  TaxCreditsNino,
    route: String
  ): URL = url"${s"$serviceUrl/tcs/${nino.value}/$route"}"

  def getExclusion(
    nino:                   TaxCreditsNino
  )(implicit headerCarrier: HeaderCarrier,
    ex:                     ExecutionContext
  ): Future[Option[Exclusion]] =
    http
      .get(url(nino, "exclusion"))
      .execute[Option[Exclusion]]
      .recover {
        case _: NotFoundException => None
      }

  def getDashboardData(
    nino:                   TaxCreditsNino
  )(implicit headerCarrier: HeaderCarrier,
    ex:                     ExecutionContext
  ): Future[Option[DashboardData]] =
    http
      .get(url(nino, "dashboard-data"))
      .execute[Option[DashboardData]]
      .recover {
        case _: NotFoundException => None
      }
}
