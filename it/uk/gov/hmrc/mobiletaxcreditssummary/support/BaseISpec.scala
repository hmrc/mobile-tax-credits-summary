/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.mobiletaxcreditssummary.support

import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate

class BaseISpec
    extends AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with FutureAwaits
    with DefaultAwaitTimeout
    with IntegrationPatience
    with OptionValues
    with WsScalaTestClient
    with GuiceOneServerPerSuite
    with WireMockSupport {
  override implicit lazy val app: Application = appBuilder.build()

  protected val nino1: Nino = Nino("AA000000A")
  protected val nino2: Nino = Nino("AP412713B")
  protected val sandboxNino: Nino = Nino("CS700100A")
  protected val acceptJsonHeader:        (String, String) = "Accept"        -> "application/vnd.hmrc.1.0+json"
  protected val authorisationJsonHeader: (String, String) = "AUTHORIZATION" -> "Bearer 123"

  def configuration: Map[String, Any] =
    Map(
      "auditing.enabled"                                      -> false,
      "microservice.services.auth.port"                       -> wireMockPort,
      "microservice.services.datastream.port"                 -> wireMockPort,
      "microservice.services.tax-credits-broker.port"         -> wireMockPort,
      "microservice.services.mobile-tax-credits-renewal.port" -> wireMockPort,
      "microservice.services.mobile-shuttering.port"          -> wireMockPort,
      "auditing.consumer.baseUri.port"                        -> wireMockPort,
      "microservice.renewals.startDate"                       -> LocalDate.now().minusMonths(2).atStartOfDay().toString,
      "microservice.renewals.packReceivedDate"                -> LocalDate.now().minusMonths(1).atStartOfDay().toString,
      "microservice.renewals.endDate"                         -> LocalDate.now().plusMonths(1).atStartOfDay().toString,
      "microservice.renewals.gracePeriodEndDate"              -> LocalDate.now().plusMonths(2).atStartOfDay().toString,
      "microservice.renewals.endViewRenewalsDate"             -> LocalDate.now().plusMonths(3).atStartOfDay().toString
    )

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(configuration)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

}
