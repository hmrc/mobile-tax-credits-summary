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

package uk.gov.hmrc.mobiletaxcreditssummary

import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobiletaxcreditssummary.stubs.AuthStub.grantAccess
import uk.gov.hmrc.mobiletaxcreditssummary.stubs.TaxCreditsBrokerStub._
import uk.gov.hmrc.mobiletaxcreditssummary.support.BaseISpec

class TaxCreditsSummaryISpec extends BaseISpec with FileResource {

  "GET /income/:nino/tax-credits/tax-credits-summary " should {
    def request(nino: Nino) = wsUrl(s"/income/${nino.value}/tax-credits/tax-credits-summary").withHeaders(acceptJsonHeader)

    "return a tax credit summary " in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exlusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status shouldBe 200
      (response.json \ "taxCreditSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency").as[String] shouldBe "weekly"
    }

    "return empty tax summary response if excluded" in {
      grantAccess(nino1.value)
      exlusionFlagIsFound(nino1, excluded = true)

      val response = await(request(nino1).get())
      response.status shouldBe 200
      (response.json \ "taxCreditSummary").get.toString shouldBe "{}"
    }
  }

  "GET /income/:nino/tax-credits/tax-credits-decision" should {
    def request(nino: Nino) = wsUrl(s"/income/${nino.value}/tax-credits/tax-credits-decision").withHeaders(acceptJsonHeader)

    "return showData == false if excluded" in {
      grantAccess(nino1.value)
      exlusionFlagIsFound(nino1, excluded = true)

      val response = await(request(nino1).get())
      response.status shouldBe 200
      (response.json \ "showData").as[Boolean] shouldBe false
    }

    "return showData == true if not excluded" in {
      grantAccess(nino1.value)
      exlusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status shouldBe 200
      (response.json \ "showData").as[Boolean] shouldBe true
    }
  }
}
