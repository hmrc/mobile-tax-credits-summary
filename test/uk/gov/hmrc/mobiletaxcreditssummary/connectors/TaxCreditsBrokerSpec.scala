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

import java.time.{LocalDate, LocalDateTime}

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, TestData, WordSpecLike}
import org.scalatestplus.play.OneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.mobiletaxcreditssummary.domain.TaxCreditsNino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxCreditsBrokerSpec extends WordSpecLike with Matchers with ScalaFutures with FutureAwaits with DefaultAwaitTimeout {

  trait Setup extends MockFactory {
    implicit lazy val hc: HeaderCarrier = HeaderCarrier()

    val expectedNextDueDate: LocalDateTime = LocalDate.parse("2015-07-16").atStartOfDay()

    val expectedPaymentWTC      = FuturePayment(160.34, expectedNextDueDate, oneOffPayment = false)
    val expectedPaymentCTC      = FuturePayment(140.12, expectedNextDueDate, oneOffPayment = false)
    val paymentSectionCTC       = PaymentSection(List(expectedPaymentCTC), "WEEKLY")
    val paymentSectionWTC       = PaymentSection(List(expectedPaymentWTC), "WEEKLY")
    val paymentSummary          = PaymentSummary(Some(paymentSectionWTC), Some(paymentSectionCTC), paymentEnabled = Some(true))
    val exclusionPaymentSummary = PaymentSummary(None, None, None, None, excluded = Some(true))

    lazy val http500Response: Future[Nothing]      = Future.failed(Upstream5xxResponse("Error", 500, 500))
    lazy val response:        Future[HttpResponse] = http200Person

    lazy val http200Person:      Future[AnyRef with HttpResponse] = Future.successful(HttpResponse(200, Some(Json.toJson(personalDetails))))
    lazy val http200Partner:     Future[AnyRef with HttpResponse] = Future.successful(HttpResponse(200, Some(Json.toJson(partnerDetails))))
    lazy val http400Exception:   Future[AnyRef with HttpResponse] = Future.successful(HttpResponse(400, None))
    lazy val http404NoPartner:   Future[AnyRef with HttpResponse] = Future.successful(HttpResponse(404, None))
    lazy val http200Children:    Future[AnyRef with HttpResponse] = Future.successful(HttpResponse(200, Some(Json.toJson(tcbChildren))))
    lazy val http200Payment:     Future[AnyRef with HttpResponse] = Future.successful(HttpResponse(200, Some(Json.toJson(paymentSummary))))
    lazy val http200Exclusion:   Future[AnyRef with HttpResponse] = Future.successful(HttpResponse(200, Some(Json.toJson(exclusion))))
    lazy val http200NotExcluded: Future[AnyRef with HttpResponse] = Future.successful(HttpResponse(200, Some(Json.toJson(notExcluded))))
    lazy val http404Exclusion:   Future[AnyRef with HttpResponse] = Future.successful(HttpResponse(404, None))

    val AGE17 = "1999-08-31"
    val AGE18 = "1998-01-09"
    val AGE19 = "1997-01-09"

    val SarahSmith  = Child("Sarah", "Smith", LocalDate.parse(AGE17), hasFTNAE  = false, hasConnexions = false, isActive = false, None)
    val JosephSmith = Child("Joseph", "Smith", LocalDate.parse(AGE18), hasFTNAE = false, hasConnexions = false, isActive = false, None)
    val MarySmith   = Child("Mary", "Smith", LocalDate.parse(AGE19), hasFTNAE   = false, hasConnexions = false, isActive = false, None)

    val nino            = Nino("KM569110B")
    val personalDetails = Person(forename = "Nuala", surname = "O'Shea")
    val partnerDetails  = Person("Frederick", Some("Tarquin"), "Hunter-Smith")

    val children: Seq[Child] = Seq(SarahSmith, JosephSmith, MarySmith)
    val tcbChildren = Children(children)

    val exclusion   = Exclusion(true)
    val notExcluded = Exclusion(false)
    val serviceUrl  = "someUrl"

    class TestTaxCreditsBrokerConnector(http: CoreGet) extends TaxCreditsBrokerConnector(http, serviceUrl)

    def TaxCreditsBrokerTestConnector(response: Option[Future[HttpResponse]] = None): TestTaxCreditsBrokerConnector = {

      val http: CoreGet = new CoreGet with HttpGet {
        override val hooks: Seq[HttpHook] = NoneRequired

        override def configuration: Option[Config] = None

        override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
          response.getOrElse(throw new Exception("No response defined!"))
        override protected def actorSystem: ActorSystem = ActorSystem()
      }

      new TestTaxCreditsBrokerConnector(http)
    }

    val connector: TaxCreditsBrokerConnector = TaxCreditsBrokerTestConnector(Some(response))
  }

  "taxCreditsBroker connector" should {

    "return a valid response for getPersonalDetails when a 200 response is received with a valid json payload" in new Setup {
      override lazy val response: Future[AnyRef with HttpResponse] = http200Person

      await(connector.getPersonalDetails(TaxCreditsNino(nino.value))) shouldBe personalDetails
    }

    "return a valid response for getPartnerDetails when a 200 response is received with a valid json payload" in new Setup {
      override lazy val response: Future[AnyRef with HttpResponse] = http200Partner

      await(connector.getPartnerDetails(TaxCreditsNino(nino.value))) shouldBe Some(partnerDetails)
    }

    "return a valid response for getPartnerDetails when a 404 response is received for no partner" in new Setup {
      override lazy val response: Future[AnyRef with HttpResponse] = http404NoPartner

      await(connector.getPartnerDetails(TaxCreditsNino(nino.value))) shouldBe None
    }

    "return an error response for getPartnerDetails when a 4xx response is returned (excluding 404)" in new Setup {
      override lazy val response: Future[AnyRef with HttpResponse] = http400Exception

      intercept[BadRequestException] {
        await(connector.getPartnerDetails(TaxCreditsNino(nino.value)))
      }
    }

    "return a valid response for getChildren when a 200 response is received with a valid json payload" in new Setup {
      override lazy val response: Future[AnyRef with HttpResponse] = http200Children

      await(connector.getChildren(TaxCreditsNino(nino.value))) shouldBe children
    }

    "return exclusion = true when 200 response is received with a valid json payload of exclusion = true" in new Setup {
      override lazy val response: Future[AnyRef with HttpResponse] = http200Exclusion

      await(connector.getExclusion(TaxCreditsNino(nino.value))) shouldBe Some(exclusion)
    }

    "return exclusion = false when 200 response is received with a valid json payload of exclusion = false" in new Setup {
      override lazy val response: Future[AnyRef with HttpResponse] = http200NotExcluded

      await(connector.getExclusion(TaxCreditsNino(nino.value))) shouldBe Some(notExcluded)
    }

    "return exclusion = None when 404 response is received" in new Setup {
      override lazy val response: Future[AnyRef with HttpResponse] = http404Exclusion

      await(connector.getExclusion(TaxCreditsNino(nino.value))) shouldBe None
    }

    "return a valid response for getPaymentSummary when a 200 response is received with a valid json payload" in new Setup {
      override lazy val response: Future[AnyRef with HttpResponse] = http200Payment
      val result:                 PaymentSummary                   = await(connector.getPaymentSummary(TaxCreditsNino(nino.value)))
      result shouldBe paymentSummary
    }

    "return excluded payment summary response" in new Setup {
      override lazy val response: Future[AnyRef with HttpResponse] = http200Exclusion
      val result:                 PaymentSummary                   = await(connector.getPaymentSummary(TaxCreditsNino(nino.value)))
      result shouldBe exclusionPaymentSummary
    }
  }
}
