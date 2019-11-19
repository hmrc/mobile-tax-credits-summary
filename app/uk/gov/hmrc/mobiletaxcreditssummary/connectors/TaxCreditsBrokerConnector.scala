/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxCreditsBrokerConnector @Inject()(http: CoreGet,
                                          @Named("tax-credits-broker") serviceUrl: String) {
  val externalServiceName = "tax-credits-broker"

  def url(nino: TaxCreditsNino, route: String) = s"$serviceUrl/tcs/${nino.value}/$route"

  def getPaymentSummary(nino: TaxCreditsNino)(implicit headerCarrier: HeaderCarrier, ex: ExecutionContext): Future[Option[PaymentSummary]] =
    http.GET[Option[PaymentSummary]](url(nino, "payment-summary")).recover {
      case _: NotFoundException => None
    }

  def getPersonalDetails(nino: TaxCreditsNino)(implicit headerCarrier: HeaderCarrier, ex: ExecutionContext): Future[Person] =
    http.GET[Person](url(nino, "personal-details"))

  def getPartnerDetails(nino: TaxCreditsNino)(implicit headerCarrier: HeaderCarrier, ex: ExecutionContext): Future[Option[Person]] =
    http.GET[Option[Person]](url(nino, "partner-details")).recover {
      case _: NotFoundException => None
    }

  def getChildren(nino: TaxCreditsNino)(implicit headerCarrier: HeaderCarrier, ex: ExecutionContext): Future[Seq[Child]] =
    http.GET[Children](url(nino, "children")).map(children => children.child)

  def getExclusion(nino: TaxCreditsNino)(implicit headerCarrier: HeaderCarrier, ex: ExecutionContext): Future[Option[Exclusion]] =
    http.GET[Option[Exclusion]](url(nino, "exclusion")).recover {
      case _: NotFoundException => None
    }
}