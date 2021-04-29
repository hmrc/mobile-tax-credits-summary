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
import uk.gov.hmrc.mobiletaxcreditssummary.connectors.{ShutteringConnector, TaxCreditsBrokerConnector, TaxCreditsRenewalsConnector}
import uk.gov.hmrc.mobiletaxcreditssummary.domain._
import uk.gov.hmrc.mobiletaxcreditssummary.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobiletaxcreditssummary.mocks.{AuditMock, AuthorisationMock, ShutteringMock, TaxCreditsBrokerConnectorMock, TaxCreditsRenewalsConnectorMock}
import uk.gov.hmrc.mobiletaxcreditssummary.services.{LiveTaxCreditsSummaryService, ReportActualProfitService}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import eu.timepit.refined.auto._
import java.time.LocalDateTime

trait TestSetup
    extends WordSpecLike
    with Matchers
    with MockFactory
    with TaxCreditsBrokerConnectorMock
    with AuthorisationMock
    with AuditMock
    with ShutteringMock
    with TaxCreditsRenewalsConnectorMock {

  implicit val hc:                              HeaderCarrier                = HeaderCarrier()
  implicit val mockAuthConnector:               AuthConnector                = mock[AuthConnector]
  implicit val mockTaxCreditsBrokerConnector:   TaxCreditsBrokerConnector    = mock[TaxCreditsBrokerConnector]
  implicit val mockAuditConnector:              AuditConnector               = mock[AuditConnector]
  implicit val mockService:                     LiveTaxCreditsSummaryService = mock[LiveTaxCreditsSummaryService]
  implicit val mockConfiguration:               Configuration                = mock[Configuration]
  implicit val mockShutteringConnector:         ShutteringConnector          = mock[ShutteringConnector]
  implicit val mockReportActualProfitService:   ReportActualProfitService    = mock[ReportActualProfitService]
  implicit val mockTaxCreditsRenewalsConnector: TaxCreditsRenewalsConnector  = mock[TaxCreditsRenewalsConnector]

  val shuttered: Shuttering =
    Shuttering(shuttered = true, Some("Shuttered"), Some("Tax Credits Summary is currently not available"))
  val notShuttered: Shuttering = Shuttering.shutteringDisabled

  val noNinoFoundOnAccount: JsValue =
    Json.parse("""{"code":"UNAUTHORIZED","message":"NINO does not exist on account"}""")

  val lowConfidenceLevelError: JsValue =
    Json.parse("""{"code":"LOW_CONFIDENCE_LEVEL","message":"Confidence Level on account does not allow access"}""")

  val now:              LocalDateTime    = LocalDateTime.now()
  val journeyId:        JourneyId        = "17d2420c-4fc6-4eee-9311-a37325066704"
  val nino:             String           = "CS700100A"
  val tcrNino:          TaxCreditsNino   = TaxCreditsNino("CS700100A")
  val incorrectNino:    Nino             = Nino("SC100700A")
  val renewalReference: RenewalReference = RenewalReference("111111111111111")
  val acceptHeader:     (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"

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
