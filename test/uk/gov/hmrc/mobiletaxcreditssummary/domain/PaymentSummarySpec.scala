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

package uk.gov.hmrc.mobiletaxcreditssummary.domain

import java.time.{LocalDateTime, ZoneOffset}

import org.joda.time.DateTime
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import play.api.libs.json._
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.{FuturePayment, PastPayment, PaymentSummary}

class PaymentSummarySpec extends WordSpecLike with Matchers with OptionValues {

  private val now = LocalDateTime.now

  def millis(ldt: LocalDateTime): Long = ldt.toInstant(ZoneOffset.UTC).toEpochMilli

  def payment(
    amount:          Double,
    paymentDate:     LocalDateTime,
    oneOffPayment:   Boolean,
    holidayType:     Option[String] = None,
    explanatoryText: Option[String] = None): String = {
    val bankHolidayJson     = if (holidayType.isDefined) s"""| "holidayType": "${holidayType.get}", """ else ""
    val explanatoryTextJson = if (explanatoryText.isDefined) s"""|, "explanatoryText": "${explanatoryText.get}" """ else ""

    s"""{
       | "amount": $amount,
       | "paymentDate": ${millis(paymentDate)},
       | "oneOffPayment": $oneOffPayment,
       $bankHolidayJson
       | "earlyPayment": ${holidayType.isDefined}
       $explanatoryTextJson
       |}""".stripMargin
  }

  def total(amount: Double, paymentDate: LocalDateTime): String =
    s"""{
       |"amount": $amount,
       |"paymentDate": ${millis(paymentDate)}
       |}""".stripMargin

  private val futureEarlyPaymentText  = Some("Your payment is early because of UK bank holidays.")
  private val pastEarlyPaymentText    = Some("Your payment was early because of UK bank holidays.")
  private val futureOneOffPaymentText = Some("This is because of a recent change and is to help you get the right amount of tax credits.")
  private val pastOneOffPaymentText   = Some("This was because of a recent change and was to help you get the right amount of tax credits.")

  private val bankHoliday = Some("bankHolidsy")

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
      val expectedResponse = Json.parse(s"""{ $ctc, "paymentEnabled": true, $totalsByDate }""")
      val response         = Json.parse(request).validate[PaymentSummary]
      val paymentSummary   = response.asOpt.value

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
      val expectedResponse = Json.parse(s"""{ $wtc, "paymentEnabled": true, $totalsByDate }""")
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
      val expectedResponse = Json.parse(s"""{ $wtc, $ctc, "paymentEnabled": true, $totalsByDate }""")

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

      val request          = s"""{ $wtc, $ctc, "paymentEnabled": true}""".stripMargin
      val expectedResponse = Json.parse(s"""{ $wtc, $ctc, "paymentEnabled": true, $totalsByDate, $previousTotalsByDate }""")
      val response         = Json.parse(request).validate[PaymentSummary]
      val paymentSummary   = response.asOpt.value
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

      val request          = s"""{ $wtc, $ctc, "paymentEnabled": true}""".stripMargin
      val expectedResponse = Json.parse(s"""{ $wtc, $ctc, "paymentEnabled": true, $totalsByDate, $previousTotalsByDate }""")
      val response         = Json.parse(request).validate[PaymentSummary]
      val paymentSummary   = response.asOpt.value
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

      val request          = s"""{ $wtc, $ctc, "paymentEnabled": true}""".stripMargin
      val expectedResponse = Json.parse(s"""{ $wtc, $ctc, "paymentEnabled": true, $totalsByDate, $previousTotalsByDate }""")
      val response         = Json.parse(request).validate[PaymentSummary]
      val paymentSummary   = response.asOpt.value
      paymentSummary.paymentEnabled.get                             shouldBe true
      paymentSummary.childTaxCredit.isDefined                       shouldBe true
      paymentSummary.workingTaxCredit.isDefined                     shouldBe true
      paymentSummary.totalsByDate.isDefined                         shouldBe true
      paymentSummary.previousTotalsByDate.isDefined                 shouldBe true
      jsonDiff(None, Json.toJson(paymentSummary), expectedResponse) shouldBe 'empty
    }
  }
  "correctly return the informationMessage" in {
    val wtc =
      s"""
         |"workingTaxCredit": {
         |  "paymentSeq": [
         |    ${payment(50.00, now.plusMonths(1), oneOffPayment = false)},
         |    ${payment(82.00, now.plusMonths(2), oneOffPayment = true, None, futureOneOffPaymentText)},
         |    ${payment(82.00, now.plusMonths(3), oneOffPayment = false, bankHoliday, futureEarlyPaymentText)}
         |  ],
         |  "paymentFrequency": "WEEKLY"
         |}
         """.stripMargin
    val ctc =
      s"""
         |"childTaxCredit": {
         |  "paymentSeq": [
         |    ${payment(25.00, now.plusMonths(1), oneOffPayment = false)},
         |    ${payment(25.00, now.plusMonths(2), oneOffPayment = true, None, futureOneOffPaymentText)},
         |    ${payment(50.00, now.plusMonths(3), oneOffPayment = false, bankHoliday, futureEarlyPaymentText)}
         |  ],
         |  "paymentFrequency": "WEEKLY"
         |}
         """.stripMargin
    val totalsByDate =
      s"""
         |"totalsByDate": [
         |  ${total(75.00, now.plusMonths(1))},
         |  ${total(107.00, now.plusMonths(2))},
         |  ${total(132.00, now.plusMonths(3))}
         |]
         """.stripMargin

    val request          = s"""{$wtc, $ctc, "specialCircumstances": "FTNAE", "paymentEnabled": true}""".stripMargin
    val expectedResponse = Json.parse(s"""{
         |$wtc, $ctc,
         |"paymentEnabled": true,
         |"specialCircumstances":"FTNAE",
         |"informationMessage": "We are currently working out your payments as your child is changing their education or training. This should be done by 7 September ${DateTime.now.year.get}. If your child is staying in education or training, update their details on GOV.UK.",
         |$totalsByDate
         |}""".stripMargin)
    val response         = Json.parse(request).validate[PaymentSummary]
    val paymentSummary   = response.asOpt.value

    paymentSummary.paymentEnabled.get           shouldBe true
    paymentSummary.childTaxCredit.isDefined     shouldBe true
    paymentSummary.workingTaxCredit.isDefined   shouldBe true
    paymentSummary.informationMessage.isDefined shouldBe true
    paymentSummary.totalsByDate.isDefined       shouldBe true

    jsonDiff(None, Json.toJson(paymentSummary), expectedResponse) shouldBe 'empty
  }

  "Future Payment " should {
    "return the correct explanatory text for a one-off payment" in {
      FuturePayment(1, now, oneOffPayment = false).explanatoryText shouldBe None
      FuturePayment(1, now, oneOffPayment = true).explanatoryText shouldBe
        Some("This is because of a recent change and is to help you get the right amount of tax credits.")
    }

    "return the correct explanatory text for a bank holiday payment" in {
      FuturePayment(1, now, oneOffPayment = false, holidayType = Some("bankHoliday")).explanatoryText shouldBe
        futureEarlyPaymentText
    }

    "return the one-off payment explanatory text for a one-off payment made early due to a bank holiday" in {
      FuturePayment(1, now, oneOffPayment = true, holidayType = Some("bankHoliday")).explanatoryText shouldBe
        Some("This is because of a recent change and is to help you get the right amount of tax credits.")
    }
  }

  "Past Payment " should {
    "return the correct explanatory text for a one-off payment" in {
      PastPayment(1, now, oneOffPayment = false).explanatoryText shouldBe None
      PastPayment(1, now, oneOffPayment = true).explanatoryText shouldBe
        Some("This was because of a recent change and was to help you get the right amount of tax credits.")
    }

    "return the correct explanatory text for a bank holiday payment" in {
      PastPayment(1, now, oneOffPayment = false, holidayType = Some("bankHoliday")).explanatoryText shouldBe
        Some("Your payment was early because of UK bank holidays.")
    }

    "return the one-off payment explanatory text for a one-off payment made early due to a bank holiday" in {
      PastPayment(1, now, oneOffPayment = true, holidayType = Some("bankHoliday")).explanatoryText shouldBe
        Some("This was because of a recent change and was to help you get the right amount of tax credits.")
    }
  }
}
