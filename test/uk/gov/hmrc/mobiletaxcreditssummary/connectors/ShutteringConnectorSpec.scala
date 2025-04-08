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

import eu.timepit.refined.auto._
import org.scalamock.handlers.CallHandler
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.mobiletaxcreditssummary.controllers.TestSetup
import uk.gov.hmrc.mobiletaxcreditssummary.domain.Shuttering

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ShutteringConnectorSpec extends TestSetup with FutureAwaits with DefaultAwaitTimeout {
  val mockHttpClient:     HttpClientV2   = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val shutteringBaseUrl = "https://someUrl"
  val connector: ShutteringConnector = new ShutteringConnector(mockHttpClient, shutteringBaseUrl)

  def mockShutteringGet[T]: CallHandler[Future[T]] = {
    (mockHttpClient
      .get(_: URL)(_: HeaderCarrier))
      .expects(
        url"${s"$shutteringBaseUrl/mobile-shuttering/service/mobile-tax-credits-summary/shuttered-status?journeyId=f65442f9-8716-4de8-9e17-3bfe4ba50a93"}",
        *
      )
      .returns(mockRequestBuilder)

    (mockRequestBuilder
      .execute[T](_: HttpReads[T], _: ExecutionContext))
      .expects(*, *)

  }

  "getShutteredStatus" should {
    "Assume unshuttered for InternalServerException response" in {
      mockShutteringGet.returns(Future failed new InternalServerException(""))

      val result: Shuttering = await(connector.getShutteringStatus("f65442f9-8716-4de8-9e17-3bfe4ba50a93"))
      result shouldBe Shuttering.shutteringEnabled
    }

    "Assume unshuttered for BadGatewayException response" in {
      mockShutteringGet.returns(Future failed new BadGatewayException(""))

      val result: Shuttering = await(connector.getShutteringStatus("f65442f9-8716-4de8-9e17-3bfe4ba50a93"))
      result shouldBe Shuttering.shutteringEnabled
    }
  }
}
