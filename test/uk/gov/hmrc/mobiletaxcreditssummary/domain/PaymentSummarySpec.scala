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

package uk.gov.hmrc.mobiletaxcreditssummary.domain

import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import play.api.libs.json._
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.{FuturePayment, PastPayment, PaymentSummary}

import java.time.{LocalDate, ZoneId, ZoneOffset}

class PaymentSummarySpec extends WordSpecLike with Matchers with OptionValues {

  private val now = LocalDate.now

  def payment(
    amount:          Double,
    paymentDate:     LocalDate,
    oneOffPayment:   Boolean,
    holidayType:     Option[String] = None,
    explanatoryText: Option[String] = None
  ): String = {
    val bankHolidayJson = if (holidayType.isDefined) s"""| "holidayType": "${holidayType.get}", """ else ""
    val explanatoryTextJson =
      if (explanatoryText.isDefined) s"""|, "explanatoryText": "${explanatoryText.get}" """ else ""

    s"""{
       | "amount": $amount,
       | "paymentDate": "${paymentDate.toString}",
       | "oneOffPayment": $oneOffPayment,
       $bankHolidayJson
       | "earlyPayment": ${holidayType.isDefined}
       $explanatoryTextJson
       |}""".stripMargin
  }

  def total(
    amount:      Double,
    paymentDate: LocalDate
  ): String =
    s"""{
       |"amount": $amount,
       |"paymentDate": "${paymentDate.toString}"
       |}""".stripMargin

  private val futureEarlyPaymentText = Some("Your payment is early because of UK bank holidays.")
  private val pastEarlyPaymentText   = Some("Your payment was early because of UK bank holidays.")

  private val futureOneOffPaymentText = Some(
    "One-off payment because of a recent change to help you get the right amount of tax credits."
  )

  private val pastOneOffPaymentText = Some(
    "One-off payment because of a recent change to help you get the right amount of tax credits."
  )

  private val bankHoliday = Some("bankHoliday")

  def convertDates(json: String): String =
    json
      .replaceAll("\"" + now.plusMonths(1) + "\"",
                  now.plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli.toString)
      .replaceAll("\"" + now.plusMonths(2) + "\"",
                  now.plusMonths(2).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli.toString)
      .replaceAll("\"" + now.plusMonths(3) + "\"",
                  now.plusMonths(3).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli.toString)
      .replaceAll("\"" + now.minusMonths(1) + "\"",
                  now.minusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli.toString)
      .replaceAll("\"" + now.minusMonths(2) + "\"",
                  now.minusMonths(2).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli.toString)
      .replaceAll("\"" + now.minusMonths(3) + "\"",
                  now.minusMonths(3).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli.toString)
      .replaceAll("\"" + now.minusMonths(5) + "\"",
                  now.minusMonths(5).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli.toString)

  "PaymentSummary" should {
    "parse correctly if no wtc or ctc is provided" in {
      val request =
        """{"paymentEnabled":true}""".stripMargin

      val response       = Json.parse(request).validate[PaymentSummary]
      val paymentSummary = response.asOpt.value
      paymentSummary.paymentEnabled.get     shouldBe true
      paymentSummary.childTaxCredit         shouldBe None
      paymentSummary.workingTaxCredit       shouldBe None
      paymentSummary.totalsByDate.isDefined shouldBe false

      Json.stringify(Json.toJson(paymentSummary)) shouldBe request
    }
    "parse correctly if no wtc is provided" in {
      val ctc =
        s"""
           |"childTaxCredit": {
           |  "paymentSeq": [
           |    ${payment(55.00, now.plusMonths(1), oneOffPayment = false)},
           |    ${payment(55.00, now.plusMonths(2), oneOffPayment = true, None, futureOneOffPaymentText)},
           |    ${payment(55.00, now.plusMonths(3), oneOffPayment = false, bankHoliday, futureEarlyPaymentText)}
           |  ],
           |  "paymentFrequency": "WEEKLY"
           |}
         """.stripMargin

      val totalsByDate =
        s"""
           |"totalsByDate": [
           |  ${total(55.00, now.plusMonths(1))},
           |  ${total(55.00, now.plusMonths(2))},
           |  ${total(55.00, now.plusMonths(3))}
           |]
         """.stripMargin

      val request          = s"""{$ctc, "paymentEnabled": true}""".stripMargin
      val expectedResponse = Json.parse(convertDates(s"""{ $ctc, "paymentEnabled": true, $totalsByDate }"""))
      val response         = Json.parse(request).validate[PaymentSummary]
      val paymentSummary: PaymentSummary = response.asOpt.value

      paymentSummary.paymentEnabled.get         shouldBe true
      paymentSummary.childTaxCredit.isDefined   shouldBe true
      paymentSummary.workingTaxCredit.isDefined shouldBe false
      paymentSummary.totalsByDate.isDefined     shouldBe true

      jsonDiff(None, Json.toJson(paymentSummary), expectedResponse) shouldBe 'empty
    }
    "parse correctly if no ctc is provided" in {

      val wtc =
        s"""
           |"workingTaxCredit": {
           |  "paymentSeq": [
           |    ${payment(55.00, now.plusMonths(1), oneOffPayment = false)},
           |    ${payment(55.00, now.plusMonths(2), oneOffPayment = true, None, futureOneOffPaymentText)},
           |    ${payment(55.00, now.plusMonths(3), oneOffPayment = false, bankHoliday, futureEarlyPaymentText)}
           |  ],
           |  "paymentFrequency": "WEEKLY"
           |}
         """.stripMargin

      val totalsByDate =
        s"""
           |"totalsByDate": [
           |  ${total(55.00, now.plusMonths(1))},
           |  ${total(55.00, now.plusMonths(2))},
           |  ${total(55.00, now.plusMonths(3))}
           |]
         """.stripMargin

      val request          = s"""{$wtc,"paymentEnabled": true}""".stripMargin
      val expectedResponse = Json.parse(convertDates(s"""{ $wtc, "paymentEnabled": true, $totalsByDate }"""))
      val response         = Json.parse(request).validate[PaymentSummary]
      val paymentSummary   = response.asOpt.value
      paymentSummary.paymentEnabled.get         shouldBe true
      paymentSummary.childTaxCredit.isDefined   shouldBe false
      paymentSummary.workingTaxCredit.isDefined shouldBe true
      paymentSummary.totalsByDate.isEmpty       shouldBe false

      jsonDiff(None, Json.toJson(paymentSummary), expectedResponse) shouldBe 'empty
    }
    "parse correctly and sort calculated totalsByDate by Date" in {

      val wtc =
        s"""
           |"workingTaxCredit": {
           |  "paymentSeq": [
           |    ${payment(55.00, now.plusMonths(1), oneOffPayment = false)},
           |    ${payment(55.00, now.plusMonths(2), oneOffPayment = true, None, futureOneOffPaymentText)},
           |    ${payment(55.00, now.plusMonths(3), oneOffPayment = false, bankHoliday, futureEarlyPaymentText)}
           |  ],
           |  "paymentFrequency": "WEEKLY"
           |}
         """.stripMargin

      val ctc =
        s"""
           |"childTaxCredit": {
           |  "paymentSeq": [
           |    ${payment(55.00, now.plusMonths(1), oneOffPayment = false)},
           |    ${payment(55.00, now.plusMonths(2), oneOffPayment = true, None, futureOneOffPaymentText)},
           |    ${payment(55.00, now.plusMonths(3), oneOffPayment = false, bankHoliday, futureEarlyPaymentText)}
           |  ],
           |  "paymentFrequency": "WEEKLY"
           |}
         """.stripMargin

      val totalsByDate =
        s"""
           |"totalsByDate": [
           |  ${total(110.00, now.plusMonths(1))},
           |  ${total(110.00, now.plusMonths(2))},
           |  ${total(110.00, now.plusMonths(3))}
           |]
         """.stripMargin

      val request          = s"""{ $wtc, $ctc, "paymentEnabled": true}""".stripMargin
      val expectedResponse = Json.parse(convertDates(s"""{ $wtc, $ctc, "paymentEnabled": true, $totalsByDate }"""))

      val response       = Json.parse(request).validate[PaymentSummary]
      val paymentSummary = response.asOpt.value
      paymentSummary.paymentEnabled.get         shouldBe true
      paymentSummary.childTaxCredit.isDefined   shouldBe true
      paymentSummary.workingTaxCredit.isDefined shouldBe true
      paymentSummary.totalsByDate.isDefined     shouldBe true

      jsonDiff(None, Json.toJson(paymentSummary), expectedResponse) shouldBe 'empty
    }
    "correctly parse the previous payments and associated totals for wtc" in {

      val wtc =
        s"""
           |"workingTaxCredit": {
           |  "paymentSeq": [
           |    ${payment(55.00, now.plusMonths(1), oneOffPayment = false)},
           |    ${payment(55.00, now.plusMonths(2), oneOffPayment = true, None, futureOneOffPaymentText)},
           |    ${payment(55.00, now.plusMonths(3), oneOffPayment = false, bankHoliday, futureEarlyPaymentText)}
           |  ],
           |  "paymentFrequency": "WEEKLY",
           |  "previousPaymentSeq": [
           |    ${payment(33.00, now.minusMonths(1), oneOffPayment = false)},
           |    ${payment(43.00, now.minusMonths(2), oneOffPayment = true, None, pastOneOffPaymentText)},
           |    ${payment(53.00, now.minusMonths(3), oneOffPayment = false, bankHoliday, pastEarlyPaymentText)}
           |  ]
           |}
         """.stripMargin

      val ctc =
        s"""
           |"childTaxCredit": {
           |  "paymentSeq": [
           |    ${payment(55.00, now.plusMonths(1), oneOffPayment = false)},
           |    ${payment(55.00, now.plusMonths(2), oneOffPayment = true, None, futureOneOffPaymentText)},
           |    ${payment(55.00, now.plusMonths(3), oneOffPayment = false, bankHoliday, futureEarlyPaymentText)}
           |  ],
           |  "paymentFrequency": "WEEKLY"
           |}
         """.stripMargin

      val totalsByDate =
        s"""
           |"totalsByDate": [
           |  ${total(110.00, now.plusMonths(1))},
           |  ${total(110.00, now.plusMonths(2))},
           |  ${total(110.00, now.plusMonths(3))}
           |]
         """.stripMargin

      val previousTotalsByDate =
        s"""
           |"previousTotalsByDate": [
           | ${total(53.00, now.minusMonths(3))},
           | ${total(43.00, now.minusMonths(2))},
           | ${total(33.00, now.minusMonths(1))}
           |]
         """.stripMargin

      val request = s"""{ $wtc, $ctc, "paymentEnabled": true}""".stripMargin
      val expectedResponse =
        Json.parse(convertDates(s"""{ $wtc, $ctc, "paymentEnabled": true, $totalsByDate, $previousTotalsByDate }"""))
      val response       = Json.parse(request).validate[PaymentSummary]
      val paymentSummary = response.asOpt.value
      paymentSummary.paymentEnabled.get                             shouldBe true
      paymentSummary.childTaxCredit.isDefined                       shouldBe true
      paymentSummary.workingTaxCredit.isDefined                     shouldBe true
      paymentSummary.totalsByDate.isDefined                         shouldBe true
      paymentSummary.previousTotalsByDate.isDefined                 shouldBe true
      jsonDiff(None, Json.toJson(paymentSummary), expectedResponse) shouldBe 'empty
    }
    "correctly parse the previous payments and associated totals for ctc" in {

      val wtc =
        s"""
           |"workingTaxCredit": {
           |  "paymentSeq": [
           |    ${payment(55.00, now.plusMonths(1), oneOffPayment = false)},
           |    ${payment(55.00, now.plusMonths(2), oneOffPayment = true, None, futureOneOffPaymentText)},
           |    ${payment(55.00, now.plusMonths(3), oneOffPayment = false, bankHoliday, futureEarlyPaymentText)}
           |  ],
           |  "paymentFrequency": "WEEKLY"
           |}
         """.stripMargin

      val ctc =
        s"""
           |"childTaxCredit": {
           |  "paymentSeq": [
           |    ${payment(55.00, now.plusMonths(1), oneOffPayment = false)},
           |    ${payment(55.00, now.plusMonths(2), oneOffPayment = true, None, futureOneOffPaymentText)},
           |    ${payment(55.00, now.plusMonths(3), oneOffPayment = false, bankHoliday, futureEarlyPaymentText)}
           |  ],
           |  "paymentFrequency": "WEEKLY",
           |  "previousPaymentSeq": [
           |    ${payment(33.00, now.minusMonths(1), oneOffPayment = false)},
           |    ${payment(43.00, now.minusMonths(2), oneOffPayment = true, None, pastOneOffPaymentText)},
           |    ${payment(53.00, now.minusMonths(3), oneOffPayment = false, bankHoliday, pastEarlyPaymentText)}
           |  ]
           |}
         """.stripMargin

      val totalsByDate =
        s"""
           |"totalsByDate": [
           |  ${total(110.00, now.plusMonths(1))},
           |  ${total(110.00, now.plusMonths(2))},
           |  ${total(110.00, now.plusMonths(3))}
           |]
         """.stripMargin

      val previousTotalsByDate =
        s"""
           |"previousTotalsByDate": [
           | ${total(53.00, now.minusMonths(3))},
           | ${total(43.00, now.minusMonths(2))},
           | ${total(33.00, now.minusMonths(1))}
           |]
         """.stripMargin

      val request = s"""{ $wtc, $ctc, "paymentEnabled": true}""".stripMargin
      val expectedResponse =
        Json.parse(convertDates(s"""{ $wtc, $ctc, "paymentEnabled": true, $totalsByDate, $previousTotalsByDate }"""))
      val response       = Json.parse(request).validate[PaymentSummary]
      val paymentSummary = response.asOpt.value
      paymentSummary.paymentEnabled.get                             shouldBe true
      paymentSummary.childTaxCredit.isDefined                       shouldBe true
      paymentSummary.workingTaxCredit.isDefined                     shouldBe true
      paymentSummary.totalsByDate.isDefined                         shouldBe true
      paymentSummary.previousTotalsByDate.isDefined                 shouldBe true
      jsonDiff(None, Json.toJson(paymentSummary), expectedResponse) shouldBe 'empty
    }
    "totals are calculated correctly for wtc and ctc with future and previous payments" in {
      val wtc =
        s"""
           |"workingTaxCredit": {
           |  "paymentSeq": [
           |    ${payment(21.33, now.plusMonths(1), oneOffPayment = false)},
           |    ${payment(33.33, now.plusMonths(2), oneOffPayment = false)},
           |    ${payment(33.33, now.plusMonths(2), oneOffPayment = false)},
           |    ${payment(22.95, now.plusMonths(2), oneOffPayment = true, None, futureOneOffPaymentText)},
           |    ${payment(89.61, now.plusMonths(3), oneOffPayment = false, bankHoliday, futureEarlyPaymentText)}
           |  ],
           |  "paymentFrequency": "WEEKLY",
           |  "previousPaymentSeq": [
           |    ${payment(33.12, now.minusMonths(2), oneOffPayment = false)},
           |    ${payment(33.56, now.minusMonths(2), oneOffPayment = false)},
           |    ${payment(53.65, now.minusMonths(5), oneOffPayment = false)},
           |    ${payment(50.35, now.minusMonths(5), oneOffPayment = true, None, pastOneOffPaymentText)},
           |    ${payment(53.00, now.minusMonths(5), oneOffPayment = false, bankHoliday, pastEarlyPaymentText)}
           |  ]
           |}
         """.stripMargin

      val ctc =
        s"""
           |"childTaxCredit": {
           |  "paymentSeq": [
           |    ${payment(105.88, now.plusMonths(1), oneOffPayment = false)},
           |    ${payment(100.55, now.plusMonths(2), oneOffPayment = false)},
           |    ${payment(5.33, now.plusMonths(2), oneOffPayment = false)},
           |    ${payment(100.55, now.plusMonths(3), oneOffPayment = false)},
           |    ${payment(2.66, now.plusMonths(3), oneOffPayment = true, None, futureOneOffPaymentText)},
           |    ${payment(2.67, now.plusMonths(3), oneOffPayment = false, bankHoliday, futureEarlyPaymentText)}
           |  ],
           |  "paymentFrequency": "WEEKLY",
           |  "previousPaymentSeq": [
           |    ${payment(333.33, now.minusMonths(1), oneOffPayment = false)},
           |    ${payment(333.33, now.minusMonths(1), oneOffPayment = false)},
           |    ${payment(333.33, now.minusMonths(1), oneOffPayment = false)},
           |    ${payment(213.00, now.minusMonths(2), oneOffPayment = false)},
           |    ${payment(213.00, now.minusMonths(2), oneOffPayment = false)},
           |    ${payment(213.00, now.minusMonths(2), oneOffPayment = false)},
           |    ${payment(360.99, now.minusMonths(2), oneOffPayment = false)},
           |    ${payment(153.12, now.minusMonths(3), oneOffPayment = true, None, pastOneOffPaymentText)},
           |    ${payment(846.87, now.minusMonths(3), oneOffPayment = false, bankHoliday, pastEarlyPaymentText)}
           |  ]
           |}
         """.stripMargin

      val totalsByDate =
        s"""
           |"totalsByDate": [
           |  ${total(127.21, now.plusMonths(1))},
           |  ${total(195.49, now.plusMonths(2))},
           |  ${total(195.49, now.plusMonths(3))}
           |]
         """.stripMargin

      val previousTotalsByDate =
        s"""
           |"previousTotalsByDate": [
           | ${total(157.00, now.minusMonths(5))},
           | ${total(999.99, now.minusMonths(3))},
           | ${total(1066.67, now.minusMonths(2))},
           | ${total(999.99, now.minusMonths(1))}
           |]
         """.stripMargin

      val request = s"""{ $wtc, $ctc, "paymentEnabled": true}""".stripMargin
      val expectedResponse =
        Json.parse(convertDates(s"""{ $wtc, $ctc, "paymentEnabled": true, $totalsByDate, $previousTotalsByDate }"""))
      val response       = Json.parse(request).validate[PaymentSummary]
      val paymentSummary = response.asOpt.value
      paymentSummary.paymentEnabled.get                             shouldBe true
      paymentSummary.childTaxCredit.isDefined                       shouldBe true
      paymentSummary.workingTaxCredit.isDefined                     shouldBe true
      paymentSummary.totalsByDate.isDefined                         shouldBe true
      paymentSummary.previousTotalsByDate.isDefined                 shouldBe true
      jsonDiff(None, Json.toJson(paymentSummary), expectedResponse) shouldBe 'empty
    }
  }

  "Future Payment " should {
    "return the correct explanatory text for a one-off payment" in {
      FuturePayment(1, now, oneOffPayment = false).explanatoryText shouldBe None
      FuturePayment(1, now, oneOffPayment = true).explanatoryText shouldBe
      Some("One-off payment because of a recent change to help you get the right amount of tax credits.")
    }

    "return the correct explanatory text for a bank holiday payment" in {
      FuturePayment(1, now, oneOffPayment = false, holidayType = Some("bankHoliday")).explanatoryText shouldBe
      futureEarlyPaymentText
    }

    "return the one-off payment explanatory text for a one-off payment made early due to a bank holiday" in {
      FuturePayment(1, now, oneOffPayment = true, holidayType = Some("bankHoliday")).explanatoryText shouldBe
      Some("One-off payment because of a recent change to help you get the right amount of tax credits.")
    }
  }

  "Past Payment " should {
    "return the correct explanatory text for a one-off payment" in {
      PastPayment(1, now, oneOffPayment = false).explanatoryText shouldBe None
      PastPayment(1, now, oneOffPayment = true).explanatoryText shouldBe
      Some("One-off payment because of a recent change to help you get the right amount of tax credits.")
    }

    "return the correct explanatory text for a bank holiday payment" in {
      PastPayment(1, now, oneOffPayment = false, holidayType = Some("bankHoliday")).explanatoryText shouldBe
      Some("Your payment was early because of UK bank holidays.")
    }

    "return the one-off payment explanatory text for a one-off payment made early due to a bank holiday" in {
      PastPayment(1, now, oneOffPayment = true, holidayType = Some("bankHoliday")).explanatoryText shouldBe
      Some("One-off payment because of a recent change to help you get the right amount of tax credits.")
    }
  }
}
