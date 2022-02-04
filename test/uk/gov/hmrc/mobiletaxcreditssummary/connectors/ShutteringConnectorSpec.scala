/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http._
import uk.gov.hmrc.mobiletaxcreditssummary.controllers.TestSetup
import uk.gov.hmrc.mobiletaxcreditssummary.domain.Shuttering

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ShutteringConnectorSpec extends TestSetup with FutureAwaits with DefaultAwaitTimeout {
  val mockCoreGet: CoreGet             = mock[CoreGet]
  val connector:   ShutteringConnector = new ShutteringConnector(mockCoreGet, "")

  def mockShutteringGet[T](f: Future[T]) =
    (mockCoreGet
      .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(_: HttpReads[T],
                                                                          _: HeaderCarrier,
                                                                          _: ExecutionContext))
      .expects(
        "/mobile-shuttering/service/mobile-tax-credits-summary/shuttered-status?journeyId=f65442f9-8716-4de8-9e17-3bfe4ba50a93",
        *,
        *,
        *,
        *,
        *
      )
      .returning(f)

  "getShutteredStatus" should {
    "Assume unshuttered for InternalServerException response" in {
      mockShutteringGet(Future.successful(new InternalServerException("")))

      val result: Shuttering = await(connector.getShutteringStatus("f65442f9-8716-4de8-9e17-3bfe4ba50a93"))
      result shouldBe Shuttering.shutteringDisabled
    }

    "Assume unshuttered for BadGatewayException response" in {
      mockShutteringGet(Future.successful(new BadGatewayException("")))

      val result: Shuttering = await(connector.getShutteringStatus("f65442f9-8716-4de8-9e17-3bfe4ba50a93"))
      result shouldBe Shuttering.shutteringDisabled
    }
  }
}
