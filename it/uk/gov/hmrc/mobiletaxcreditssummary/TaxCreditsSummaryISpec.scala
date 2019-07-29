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

import play.api.libs.json.JsArray
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobiletaxcreditssummary.stubs.AuthStub.grantAccess
import uk.gov.hmrc.mobiletaxcreditssummary.stubs.TaxCreditsBrokerStub._
import uk.gov.hmrc.mobiletaxcreditssummary.support.BaseISpec

class TaxCreditsSummaryISpec extends BaseISpec with FileResource {

  "GET /income/:nino/tax-credits/tax-credits-summary " should {
    def request(nino: Nino): WSRequest = wsUrl(s"/income/${nino.value}/tax-credits/tax-credits-summary?journeyId=journeyId").addHttpHeaders(acceptJsonHeader)

    "return a valid response for TAX-CREDITS-USER - check more details on github.com/hmrc/mobile-tax-credits-summary" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                                                                                               shouldBe 200
      (response.json \ "excluded").as[Boolean]                                                                      shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency").as[String] shouldBe "WEEKLY"
      ((response.json \\ "claimants").head \ "personalDetails" \ "forename").as[String]                             shouldBe "Nuala"
      ((response.json \\ "claimants").head \ "personalDetails" \ "surname").as[String]                              shouldBe "O'Shea"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "forename").as[String]                              shouldBe "Frederick"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "otherForenames").as[String]                        shouldBe "Tarquin"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "surname").as[String]                               shouldBe "Hunter-Smith"
      (((response.json \\ "claimants").head \ "children")(0) \ "forename").as[String]                               shouldBe "Sarah"
      (((response.json \\ "claimants").head \ "children")(0) \ "surname").as[String]                                shouldBe "Smith"
    }

    "return a valid response for EXCLUDED-TAX-CREDITS-USER" in {
      grantAccess(nino1.value)
      exclusionFlagIsFound(nino1, excluded = true)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe true
    }

    "return a valid response for NON-TAX-CREDITS-USER" in {
      exclusionFlagIsNotFound(nino1)
      grantAccess(nino1.value)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
    }

    "return a valid response for EXCLUDED USER" in {
      grantAccess(nino1.value)
      paymntSummary500(nino1)
      exclusionFlagIsFound(nino1, excluded = true)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe true
    }

    "return a valid response for ERROR-500 - tcs/:nino/exclusion call returns 500" in {
      grantAccess(nino1.value)
      exclusion500(nino1)

      val response = await(request(nino1).get())
      response.status shouldBe 500
    }

    "return a valid response for ERROR-503 - tcs/:nino/paymentSummary call returns 503" in {
      grantAccess(nino1.value)
      paymntSummary503(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status shouldBe 500
    }

    "return a valid response for ERROR-503 - tcs/:nino/exclusion call returns 503" in {
      grantAccess(nino1.value)
      exclusion503(nino1)

      val response = await(request(nino1).get())
      response.status shouldBe 500
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/personal-details call returns 404" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreNotFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                                                                                               shouldBe 200
      (response.json \ "excluded").as[Boolean]                                                                      shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency").as[String] shouldBe "WEEKLY"
      (response.json \\ "claimants").isEmpty                                                                        shouldBe true
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/personal-details call returns 500" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetails500(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                                                                                               shouldBe 200
      (response.json \ "excluded").as[Boolean]                                                                      shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency").as[String] shouldBe "WEEKLY"
      (response.json \\ "claimants").isEmpty                                                                        shouldBe true
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/personal-details call returns 503" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetails503(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                                                                                               shouldBe 200
      (response.json \ "excluded").as[Boolean]                                                                      shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency").as[String] shouldBe "WEEKLY"
      (response.json \\ "claimants").isEmpty                                                                        shouldBe true
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/partner-details call returns 404" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetailsAreNotFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                                                                   shouldBe 200
      (response.json \ "excluded").as[Boolean]                                          shouldBe false
      ((response.json \\ "claimants").head \ "personalDetails" \ "forename").as[String] shouldBe "Nuala"
      ((response.json \\ "claimants").head \ "personalDetails" \ "surname").as[String]  shouldBe "O'Shea"
      (response.json \\ "partnerDetails").isEmpty                                       shouldBe true
      (((response.json \\ "claimants").head \ "children")(0) \ "forename").as[String]   shouldBe "Sarah"
      (((response.json \\ "claimants").head \ "children")(0) \ "surname").as[String]    shouldBe "Smith"
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/partner-details call returns 500" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetails500(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \\ "claimants").isEmpty   shouldBe true
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/partner-details call returns 503" in {
      grantAccess(nino1.value)
      childrenAreFound(nino1)
      partnerDetails503(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \\ "claimants").isEmpty   shouldBe true
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/children call returns OK with no children" in {
      grantAccess(nino1.value)
      childrenAreNotFound(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                                                                   shouldBe 200
      (response.json \ "excluded").as[Boolean]                                          shouldBe false
      (response.json \\ "claimants").isEmpty                                            shouldBe false
      ((response.json \\ "claimants").head \ "personalDetails" \ "forename").as[String] shouldBe "Nuala"
      ((response.json \\ "claimants").head \ "personalDetails" \ "surname").as[String]  shouldBe "O'Shea"
      (response.json \\ "children").isEmpty                                             shouldBe false
      (response.json \\ "children").head.asInstanceOf[JsArray].value.isEmpty
      ((response.json \\ "claimants").head \ "partnerDetails" \ "forename").as[String]       shouldBe "Frederick"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "otherForenames").as[String] shouldBe "Tarquin"
      ((response.json \\ "claimants").head \ "partnerDetails" \ "surname").as[String]        shouldBe "Hunter-Smith"
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/children call returns 500" in {
      grantAccess(nino1.value)
      children500(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \\ "claimants").isEmpty   shouldBe true
    }

    "return a valid response for CLAIMANTS_FAILURE - /tcs/:nino/children call returns 503" in {
      grantAccess(nino1.value)
      children503(nino1)
      partnerDetailsAreFound(nino1, nino2)
      paymntSummaryIsFound(nino1)
      personalDetailsAreFound(nino1)
      exclusionFlagIsFound(nino1, excluded = false)

      val response = await(request(nino1).get())
      response.status                          shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \\ "claimants").isEmpty   shouldBe true
    }
  }
}
