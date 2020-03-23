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

import play.api.libs.json.Json
import play.api.libs.ws.{WSRequest, WSResponse}
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.Shuttering
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.Person
import uk.gov.hmrc.mobiletaxcreditssummary.support.BaseISpec

class SandboxTaxCreditsSummaryISpec extends BaseISpec with FileResource {

  private val mobileHeader = "X-MOBILE-USER-ID" -> "208606423740"

  "GET /sandbox/income/:nino/tax-credits/tax-credits-summary " should {
    def request(nino: Nino): WSRequest =
      wsUrl(s"/income/${nino.value}/tax-credits/tax-credits-summary?journeyId=17d2420c-4fc6-4eee-9311-a37325066704")
        .addHttpHeaders(acceptJsonHeader)

    def assertEmptyTaxCreditsSummary(response: WSResponse): RuntimeException =
      intercept[RuntimeException] {
        (response.json \ "taxCreditsSummary").get
      }

    "return excluded = false and a tax credit summary where no SANDBOX-CONTROL header is set" in {
      val response = await(request(sandboxNino).addHttpHeaders(mobileHeader).get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false

      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String] shouldBe "WEEKLY"
      (response.json \ "taxCreditsSummary" \ "claimants" \ "personalDetails" \ "forename").as[String] shouldBe "Nuala"
      (response.json \ "taxCreditsSummary" \ "claimants" \ "children").as[List[Person]].head.forename shouldBe "Sarah"

    }

    "return excluded = false and a tax credit summary where SANDBOX-CONTROL header is set to PRE-FTNAE" in {
      val response = await(request(sandboxNino).addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "PRE-FTNAE").get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false

      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "informationMessage" \ "title")
        .as[String] shouldBe "We are currently working out your payments as your children are changing their education or training. This should be done by 7 September 2019."
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "informationMessage" \ "message")
        .as[String] shouldBe "If your children are staying in education or training, you should update their details."
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String] shouldBe "WEEKLY"
      (response.json \ "taxCreditsSummary" \ "claimants" \ "personalDetails" \ "forename").as[String] shouldBe "Betty"
      (response.json \ "taxCreditsSummary" \ "claimants" \ "children").as[List[Person]].head.forename shouldBe "Maya"
      (response.json \ "taxCreditsSummary" \ "claimants" \ "ftnaeLink" \ "link")
        .as[String] shouldBe "/tax-credits-service/home/children-and-childcare"
    }

    "return excluded = false and a tax credit summary where SANDBOX-CONTROL header is set to COVID" in {
      val response = await(request(sandboxNino).addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "COVID").get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false

      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "informationMessage" \ "title")
        .as[String] shouldBe "Do not update working hours if you or your partner are working less because of coronavirus (COVID-19)"
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "informationMessage" \ "message")
        .as[String] shouldBe "We are treating this as a temporary exceptional change. Your working tax credits will not go down because you are working less.\n\nYou can still report other changes in income, childcare and increased hours as normal.\n\nSearch COVID-19 on gov.uk for the latest advice."
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "specialCircumstances")
        .as[String] shouldBe "COVID"
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String] shouldBe "WEEKLY"
      (response.json \ "taxCreditsSummary" \ "claimants" \ "personalDetails" \ "forename").as[String] shouldBe "Betty"
      (response.json \ "taxCreditsSummary" \ "claimants" \ "children").as[List[Person]].head.forename shouldBe "Maya"
      (response.json \ "taxCreditsSummary" \ "claimants" \ "ftnaeLink").isEmpty shouldBe true
    }

    "return excluded = false and a tax credit summary where SANDBOX-CONTROL header is set to POST-FTNAE" in {
      val response = await(request(sandboxNino).addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "POST-FTNAE").get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false

      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "informationMessage" \ "title")
        .as[String] shouldBe "We are currently working out your payments as your children are changing their education or training. This should be done by 7 September 2019."
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "informationMessage" \ "message")
        .as[String] shouldBe "If you have let us know that your children are staying in education or training, they will be added back automatically. Otherwise, you can add them back to your claim."
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String] shouldBe "WEEKLY"
      (response.json \ "taxCreditsSummary" \ "claimants" \ "personalDetails" \ "forename").as[String] shouldBe "Betty"
      (response.json \ "taxCreditsSummary" \ "claimants" \ "children").as[List[Person]].head.forename shouldBe "Maya"
      (response.json \ "taxCreditsSummary" \ "claimants" \ "ftnaeLink" \ "link")
        .as[String] shouldBe "/tax-credits-service/children/add-child/who-do-you-want-to-add"
    }

    "return excluded = false and a tax credit summary with only working tax credit data where SANDBOX-CONTROL is WORKING-TAX-CREDIT-ONLY" in {
      val response =
        await(request(sandboxNino).addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "WORKING-TAX-CREDIT-ONLY").get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String] shouldBe "WEEKLY"
      (response.json \\ "childTaxCredit") shouldBe empty
      (response.json \ "taxCreditsSummary" \ "claimants" \ "personalDetails" \ "forename").as[String] shouldBe "Nuala"
      (response.json \ "taxCreditsSummary" \ "claimants" \\ "ftnaeLink") shouldBe empty
      (response.json \ "taxCreditsSummary" \ "claimants" \ "children").as[List[Person]].head.forename shouldBe "Sarah"
    }

    "return excluded = false and a tax credit summary with only working tax credit data where SANDBOX-CONTROL is CHILD-TAX-CREDIT-ONLY" in {
      val response =
        await(request(sandboxNino).addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "CHILD-TAX-CREDIT-ONLY").get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "childTaxCredit" \ "paymentFrequency")
        .as[String] shouldBe "WEEKLY"
      (response.json \\ "workingTaxCredit") shouldBe empty
      (response.json \ "taxCreditsSummary" \ "claimants" \ "personalDetails" \ "forename").as[String] shouldBe "Nuala"
      (response.json \ "taxCreditsSummary" \ "claimants" \\ "ftnaeLink") shouldBe empty
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \\ "informationMessage") shouldBe empty

    }

    "return excluded = false and a tax credit summary where SANDBOX-CONTROL is any other value" in {
      val response = await(request(sandboxNino).addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "RANDOMVALUE").get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String] shouldBe "WEEKLY"
      (response.json \ "taxCreditsSummary" \ "claimants" \ "personalDetails" \ "forename").as[String] shouldBe "Nuala"
    }

    "return excluded = false and a tax credit summary with no claimants section where SANDBOX-CONTROL is CLAIMANTS_FAILURE" in {
      val response =
        await(request(sandboxNino).addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "CLAIMANTS_FAILURE").get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      (response.json \ "taxCreditsSummary" \ "paymentSummary" \ "workingTaxCredit" \ "paymentFrequency")
        .as[String] shouldBe "WEEKLY"
      (response.json \ "taxCreditsSummary" \ "claimants").toOption shouldBe None
    }

    "return excluded = true and no tax credit summary data if excluded where SANDBOX-CONTROL is EXCLUDED-TAX-CREDITS-USER" in {
      val response =
        await(request(sandboxNino).addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "EXCLUDED-TAX-CREDITS-USER").get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe true
      assertEmptyTaxCreditsSummary(response)
    }

    "return excluded = false and no tax credit summary data if non tax credit user where SANDBOX-CONTROL is NON-TAX-CREDITS-USER" in {
      val response =
        await(request(sandboxNino).addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "NON-TAX-CREDITS-USER").get())
      response.status shouldBe 200
      (response.json \ "excluded").as[Boolean] shouldBe false
      assertEmptyTaxCreditsSummary(response)
    }

    "return 401 if unauthenticated where SANDBOX-CONTROL is ERROR-401" in {
      val response = await(request(sandboxNino).addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "ERROR-401").get())
      response.status shouldBe 401
    }

    "return 403 if forbidden where SANDBOX-CONTROL is ERROR-403" in {
      val response = await(request(sandboxNino).addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "ERROR-403").get())
      response.status shouldBe 403
    }

    "return 500 if there is an error where SANDBOX-CONTROL is ERROR-500" in {
      val response = await(request(sandboxNino).addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "ERROR-500").get())
      response.status shouldBe 500
    }

    "return 400 if journeyId not supplied" in {
      val response =
        await(wsUrl(s"/income/$sandboxNino/tax-credits/tax-credits-summary").addHttpHeaders(mobileHeader).get())
      response.status shouldBe 400
    }

    "return 400 if journeyId invalid" in {
      val response = await(
        wsUrl(s"/income/$sandboxNino/tax-credits/tax-credits-summary?journeyId=XXXXXXXXXXXXXXXXXX")
          .addHttpHeaders(mobileHeader)
          .get()
      )
      response.status shouldBe 400
    }

    "return 521 if there is an error where SANDBOX-CONTROL is SHUTTERED" in {
      val response = await(request(sandboxNino).addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "SHUTTERED").get())
      response.status shouldBe 521

      val shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe Some("Shuttered")
      shuttering.message   shouldBe Some("Tax Credits Summary is currently shuttered")
    }
  }
}
