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

package uk.gov.hmrc.mobiletaxcreditssummary.controllers

import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpecLike}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobiletaxcreditssummary.connectors.{ShutteringConnector, TaxCreditsBrokerConnector}
import uk.gov.hmrc.mobiletaxcreditssummary.domain._
import uk.gov.hmrc.mobiletaxcreditssummary.mocks.{AuditMock, AuthorisationMock, ShutteringMock, TaxCreditsBrokerConnectorMock}
import uk.gov.hmrc.mobiletaxcreditssummary.services.LiveTaxCreditsSummaryService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

trait TestSetup
    extends WordSpecLike
    with Matchers
    with MockFactory
    with TaxCreditsBrokerConnectorMock
    with AuthorisationMock
    with AuditMock
    with ShutteringMock {

  implicit val hc:                            HeaderCarrier                = HeaderCarrier()
  implicit val mockAuthConnector:             AuthConnector                = mock[AuthConnector]
  implicit val mockTaxCreditsBrokerConnector: TaxCreditsBrokerConnector    = mock[TaxCreditsBrokerConnector]
  implicit val mockAuditConnector:            AuditConnector               = mock[AuditConnector]
  implicit val mockService:                   LiveTaxCreditsSummaryService = mock[LiveTaxCreditsSummaryService]
  implicit val mockConfiguration:             Configuration                = mock[Configuration]
  implicit val mockShutteringConnector:       ShutteringConnector          = mock[ShutteringConnector]

  val shuttered =
    Shuttering(shuttered = true, Some("Shuttered"), Some("Tax Credits Summary is currently not available"))
  val notShuttered = Shuttering.shutteringDisabled

  val noNinoFoundOnAccount: JsValue =
    Json.parse("""{"code":"UNAUTHORIZED","message":"NINO does not exist on account"}""")

  val lowConfidenceLevelError: JsValue =
    Json.parse("""{"code":"LOW_CONFIDENCE_LEVEL","message":"Confidence Level on account does not allow access"}""")

  val nino             = "CS700100A"
  val incorrectNino    = Nino("SC100700A")
  val renewalReference = RenewalReference("111111111111111")
  val acceptHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(
      "AuthToken" -> "Some Header"
    )
    .withHeaders(
      acceptHeader,
      "Authorization" -> "Some Header"
    )

  lazy val requestInvalidHeaders: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(
      "AuthToken" -> "Some Header"
    )
    .withHeaders(
      "Authorization" -> "Some Header"
    )

  def emptyRequestWithAcceptHeader(
    renewalsRef: RenewalReference,
    nino:        Nino
  ): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withHeaders(acceptHeader)

}
