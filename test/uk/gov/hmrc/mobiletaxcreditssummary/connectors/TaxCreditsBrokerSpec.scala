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

import java.time.{LocalDate, LocalDateTime}
import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}
import play.api.{Configuration, Environment}
import play.api.libs.json.Json
import play.api.libs.json.Json.parse
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.mobiletaxcreditssummary.domain.TaxCreditsNino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class TaxCreditsBrokerSpec
    extends WordSpecLike
    with Matchers
    with ScalaFutures
    with FutureAwaits
    with DefaultAwaitTimeout
    with FileResource {

  trait Setup extends MockFactory {
    implicit lazy val hc: HeaderCarrier = HeaderCarrier()

    val expectedNextDueDate: LocalDate                = LocalDate.parse("2015-07-16")
    val headers:             Map[String, Seq[String]] = Map("Accept" -> Seq("application/vnd.hmrc.1.0+json"))
    val expectedPaymentWTC:  FuturePayment            = FuturePayment(160.34, expectedNextDueDate, oneOffPayment = false)
    val expectedPaymentCTC:  FuturePayment            = FuturePayment(140.12, expectedNextDueDate, oneOffPayment = false)
    val paymentSectionCTC:   PaymentSection           = PaymentSection(List(expectedPaymentCTC), "WEEKLY")
    val paymentSectionWTC:   PaymentSection           = PaymentSection(List(expectedPaymentWTC), "WEEKLY")

    val paymentSummary: PaymentSummary =
      PaymentSummary(Some(paymentSectionWTC), Some(paymentSectionCTC), paymentEnabled = Some(true))
    val exclusionPaymentSummary: PaymentSummary = PaymentSummary(None, None, None, None, excluded = Some(true))

    val dashboardData: DashboardData =
      Json
        .fromJson[DashboardData](parse(findResource("/resources/taxcreditssummary/CS700100A-dashboard-data.json").get))
        .get

    lazy val response: Future[HttpResponse] = http200Person

    lazy val http200Person: Future[AnyRef with HttpResponse] =
      Future.successful(HttpResponse(200, Json.toJson(personalDetails), headers))

    lazy val http200Exclusion: Future[AnyRef with HttpResponse] =
      Future.successful(HttpResponse(200, Json.toJson(exclusion), headers))

    lazy val http200NotExcluded: Future[AnyRef with HttpResponse] =
      Future.successful(HttpResponse(200, Json.toJson(notExcluded), headers))
    lazy val http404Exclusion: Future[AnyRef with HttpResponse] = Future.successful(HttpResponse(404, "NOT_FOUND"))

    lazy val http200dashboardData: Future[AnyRef with HttpResponse] =
      Future.successful(HttpResponse(200, Json.toJson(dashboardData), headers))
    lazy val http404dashboardData: Future[AnyRef with HttpResponse] = Future.successful(HttpResponse(404, "NOT_FOUND"))

    val AGE17 = "1999-08-31"
    val AGE18 = "1998-01-09"
    val AGE19 = "1997-01-09"

    val SarahSmith: Child =
      Child("Sarah", "Smith", LocalDate.parse(AGE17), hasFTNAE = false, hasConnexions = false, isActive = false, None)

    val JosephSmith: Child =
      Child("Joseph", "Smith", LocalDate.parse(AGE18), hasFTNAE = false, hasConnexions = false, isActive = false, None)

    val MarySmith: Child =
      Child("Mary", "Smith", LocalDate.parse(AGE19), hasFTNAE = false, hasConnexions = false, isActive = false, None)

    val nino:            Nino   = Nino("KM569110B")
    val personalDetails: Person = Person(forename = "Nuala", surname = "O'Shea")
    val partnerDetails:  Person = Person("Frederick", Some("Tarquin"), "Hunter-Smith")

    val children:    Seq[Child] = Seq(SarahSmith, JosephSmith, MarySmith)
    val tcbChildren: Children   = Children(children)

    val exclusion:   Exclusion = Exclusion(true)
    val notExcluded: Exclusion = Exclusion(false)
    val serviceUrl = "https://localhost"

    class TestTaxCreditsBrokerConnector(http: CoreGet) extends TaxCreditsBrokerConnector(http, serviceUrl)

    def TaxCreditsBrokerTestConnector(response: Option[Future[HttpResponse]] = None): TestTaxCreditsBrokerConnector = {

      val http: CoreGet = new CoreGet with HttpGet with GetHttpTransport {
        override val hooks: Seq[HttpHook] = NoneRequired

        override def configuration: Config = Configuration.load(Environment.simple()).underlying

        override def doGet(
          url:         String,
          headers:     Seq[(String, String)] = Seq.empty
        )(implicit ec: ExecutionContext
        ): Future[HttpResponse] =
          response.getOrElse(throw new Exception("No response defined!"))

        override protected def actorSystem: ActorSystem = ActorSystem()
      }

      new TestTaxCreditsBrokerConnector(http)
    }

    val connector: TaxCreditsBrokerConnector = TaxCreditsBrokerTestConnector(Some(response))
  }

  "taxCreditsBroker connector" should {

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

    "return a valid response for getDashboardData when a 200 response is received with a valid json payload" in new Setup {
      override lazy val response: Future[AnyRef with HttpResponse] = http200dashboardData
      val result:                 Option[DashboardData]            = await(connector.getDashboardData(TaxCreditsNino(nino.value)))
      result shouldBe Some(dashboardData)
    }

    "return None when dashboard data response is 404" in new Setup {
      override lazy val response: Future[AnyRef with HttpResponse] = http404dashboardData
      val result:                 Option[DashboardData]            = await(connector.getDashboardData(TaxCreditsNino(nino.value)))
      result shouldBe None
    }
  }
}
