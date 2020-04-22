package uk.gov.hmrc.mobiletaxcreditssummary.stubs

import java.time.{LocalDate, ZoneId}

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlPathEqualTo}
import uk.gov.hmrc.domain.Nino

object TaxCreditsBrokerStub {

  val childrenJson =
    s"""{ "child": [
        {
          "firstNames": "Sarah",
          "surname": "Smith",
          "dateOfBirth": ${
      LocalDate
        .of(LocalDate.now.minusYears(19).getYear, 8, 31)
        .atStartOfDay(ZoneId.systemDefault())
        .toEpochSecond() * 1000
    },
          "hasFTNAE": false,
          "hasConnexions": false,
          "isActive": true
        },
        {
          "firstNames": "Joseph",
          "surname": "Smith",
          "dateOfBirth": 884304000000,
          "hasFTNAE": false,
          "hasConnexions": false,
          "isActive": true
        },
        {
          "firstNames": "Mary",
          "surname": "Smith",
          "dateOfBirth": 852768000000,
          "hasFTNAE": false,
          "hasConnexions": false,
          "isActive": true
        } ] }"""

  def childrenAreFound(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/children"))
        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(childrenJson))
    )

  def childrenAreNotFound(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/children"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("""{ "child": [] }""")
        )
    )

  def children500(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/children"))
        .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json"))
    )

  def children503(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/children"))
        .willReturn(aResponse().withStatus(503).withHeader("Content-Type", "application/json"))
    )

  def partnerJson(nino: Nino): String =
    s"""{
        "forename": "Frederick",
        "otherForenames": "Tarquin",
        "surname": "Hunter-Smith",
        "nino": "${nino.value}",
        "address": {
          "addressLine1": "999 Big Street",
          "addressLine2": "Worthing",
          "addressLine3": "West Sussex",
          "postCode": "BN99 8IG"
        }
      }""".stripMargin

  def partnerDetailsAreFound(
                              climantsNino: Nino,
                              partnersNino: Nino
                            ): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${climantsNino.value}/partner-details"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(partnerJson(partnersNino))
        )
    )

  def partnerDetailsAreNotFound(
                                 climantsNino: Nino,
                                 partnersNino: Nino
                               ): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${climantsNino.value}/partner-details"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json"))
    )

  def partnerDetails500(
                         climantsNino: Nino,
                         partnersNino: Nino
                       ): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${climantsNino.value}/partner-details"))
        .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json"))
    )

  def partnerDetails503(
                         climantsNino: Nino,
                         partnersNino: Nino
                       ): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${climantsNino.value}/partner-details"))
        .willReturn(aResponse().withStatus(503).withHeader("Content-Type", "application/json"))
    )

  val paymentSummaryJson: String =
    """
      |{
      |    "workingTaxCredit": {
      |      "paymentSeq": [
      |        {
      |          "amount": 55,
      |          "paymentDate": 1509008158781,
      |          "oneOffPayment": false,
      |          "earlyPayment": false
      |        },
      |        {
      |          "amount": 55,
      |          "paymentDate": 1511690158781,
      |          "oneOffPayment": false,
      |          "holidayType": "bankHoliday",
      |          "earlyPayment": true,
      |          "explanatoryText" : "Your payment is early because of UK bank holidays."
      |        },
      |        {
      |          "amount": 55,
      |          "paymentDate": 1514282158781,
      |          "oneOffPayment": true,
      |          "earlyPayment": false,
      |          "explanatoryText" : "One-off payment because of a recent change to help you get the right amount of tax credits."
      |        }
      |      ],
      |      "paymentFrequency": "WEEKLY",
      |      "previousPaymentSeq": [
      |        {
      |          "amount": 33,
      |          "paymentDate": 1503737758781,
      |          "oneOffPayment": false,
      |          "earlyPayment": false
      |        },
      |        {
      |          "amount": 43,
      |          "paymentDate": 1501059358781,
      |          "oneOffPayment": false,
      |          "holidayType": "bankHoliday",
      |          "earlyPayment": true,
      |          "explanatoryText" : "Your payment was early because of UK bank holidays."
      |        },
      |        {
      |          "amount": 53,
      |          "paymentDate": 1498467358781,
      |          "oneOffPayment": true,
      |          "earlyPayment": false,
      |          "explanatoryText" : "This was because of a recent change and was to help you get the right amount of tax credits."
      |        }
      |      ]
      |    },
      |    "childTaxCredit": {
      |      "paymentSeq": [
      |        {
      |          "amount": 55,
      |          "paymentDate": 1509008158781,
      |          "oneOffPayment": false,
      |          "earlyPayment": false
      |        },
      |        {
      |          "amount": 55,
      |          "paymentDate": 1511690158781,
      |          "oneOffPayment": false,
      |          "holidayType": "bankHoliday",
      |          "earlyPayment": true,
      |          "explanatoryText" : "Your payment is early because of UK bank holidays."
      |        },
      |        {
      |          "amount": 55,
      |          "paymentDate": 1514282158781,
      |          "oneOffPayment": true,
      |          "earlyPayment": false,
      |          "explanatoryText" : "One-off payment because of a recent change to help you get the right amount of tax credits."
      |        }
      |      ],
      |      "paymentFrequency": "WEEKLY"
      |    },
      |    "paymentEnabled": true,
      |    "totalsByDate": [
      |      {
      |        "amount": 110,
      |        "paymentDate": 1509008158781
      |      },
      |      {
      |        "amount": 110,
      |        "paymentDate": 1511690158781
      |      },
      |      {
      |        "amount": 110,
      |        "paymentDate": 1514282158781
      |      }
      |    ],
      |    "previousTotalsByDate": [
      |      {
      |        "amount": 53,
      |        "paymentDate": 1498467358781
      |      },
      |      {
      |        "amount": 43,
      |        "paymentDate": 1501059358781
      |      },
      |      {
      |        "amount": 33,
      |        "paymentDate": 1503737758781
      |      }
      |    ]
      |  }
    """.stripMargin

  def paymentSummaryWithSpecialCircumstancesJson(specialCircumstance: String): String =
    s"""
       |{
       |    "workingTaxCredit": {
       |      "paymentSeq": [
       |        {
       |          "amount": 55,
       |          "paymentDate": 1509008158781,
       |          "oneOffPayment": false,
       |          "earlyPayment": false
       |        },
       |        {
       |          "amount": 55,
       |          "paymentDate": 1511690158781,
       |          "oneOffPayment": false,
       |          "holidayType": "bankHoliday",
       |          "earlyPayment": true,
       |          "explanatoryText" : "Your payment is early because of UK bank holidays."
       |        },
       |        {
       |          "amount": 55,
       |          "paymentDate": 1514282158781,
       |          "oneOffPayment": true,
       |          "earlyPayment": false,
       |          "explanatoryText" : "One-off payment because of a recent change to help you get the right amount of tax credits."
       |        }
       |      ],
       |      "paymentFrequency": "WEEKLY",
       |      "previousPaymentSeq": [
       |        {
       |          "amount": 33,
       |          "paymentDate": 1503737758781,
       |          "oneOffPayment": false,
       |          "earlyPayment": false
       |        },
       |        {
       |          "amount": 43,
       |          "paymentDate": 1501059358781,
       |          "oneOffPayment": false,
       |          "holidayType": "bankHoliday",
       |          "earlyPayment": true,
       |          "explanatoryText" : "Your payment was early because of UK bank holidays."
       |        },
       |        {
       |          "amount": 53,
       |          "paymentDate": 1498467358781,
       |          "oneOffPayment": true,
       |          "earlyPayment": false,
       |          "explanatoryText" : "This was because of a recent change and was to help you get the right amount of tax credits."
       |        }
       |      ]
       |    },
       |    "childTaxCredit": {
       |      "paymentSeq": [
       |        {
       |          "amount": 55,
       |          "paymentDate": 1509008158781,
       |          "oneOffPayment": false,
       |          "earlyPayment": false
       |        },
       |        {
       |          "amount": 55,
       |          "paymentDate": 1511690158781,
       |          "oneOffPayment": false,
       |          "holidayType": "bankHoliday",
       |          "earlyPayment": true,
       |          "explanatoryText" : "Your payment is early because of UK bank holidays."
       |        },
       |        {
       |          "amount": 55,
       |          "paymentDate": 1514282158781,
       |          "oneOffPayment": true,
       |          "earlyPayment": false,
       |          "explanatoryText" : "One-off payment because of a recent change to help you get the right amount of tax credits."
       |        }
       |      ],
       |      "paymentFrequency": "WEEKLY"
       |    },
       |    "paymentEnabled": true,
       |    "specialCircumstances": "$specialCircumstance",
       |    "totalsByDate": [
       |      {
       |        "amount": 110,
       |        "paymentDate": 1509008158781
       |      },
       |      {
       |        "amount": 110,
       |        "paymentDate": 1511690158781
       |      },
       |      {
       |        "amount": 110,
       |        "paymentDate": 1514282158781
       |      }
       |    ],
       |    "previousTotalsByDate": [
       |      {
       |        "amount": 53,
       |        "paymentDate": 1498467358781
       |      },
       |      {
       |        "amount": 43,
       |        "paymentDate": 1501059358781
       |      },
       |      {
       |        "amount": 33,
       |        "paymentDate": 1503737758781
       |      }
       |    ]
       |  }
    """.stripMargin

  def paymntSummaryIsFound(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/payment-summary"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(paymentSummaryJson)
        )
    )

  def paymntSummaryWithSpecialCircumstanceIsFound(nino: Nino, specialCircumstance: String): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/payment-summary"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(paymentSummaryWithSpecialCircumstancesJson(specialCircumstance))
        )
    )

  def paymntSummaryNonTCUser(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/payment-summary"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("""{ "excluded": true }""")
        )
    )

  def paymntSummary500(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/payment-summary"))
        .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json"))
    )

  def paymntSummary503(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/payment-summary"))
        .willReturn(aResponse().withStatus(503).withHeader("Content-Type", "application/json"))
    )

  def paymentSummary404(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/payment-summary"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json"))
    )

  def personalDetailsJson(nino: Nino) =
    s"""
      {
          "forename": "Nuala",
          "surname": "O'Shea",
          "nino": "${nino.value}",
          "address": {
            "addressLine1": "999 Big Street",
            "addressLine2": "Worthing",
            "addressLine3": "West Sussex",
            "postCode": "BN99 8IG"
         }
        }
    """

  def personalDetailsAreFound(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/personal-details"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(personalDetailsJson(nino))
        )
    )

  def personalDetailsAreNotFound(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/personal-details"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json"))
    )

  def personalDetails500(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/personal-details"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json"))
    )

  def personalDetails503(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/personal-details"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json"))
    )

  def exclusionFlagIsFound(
                            nino: Nino,
                            excluded: Boolean
                          ): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/exclusion"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(s"""{ "excluded" : $excluded }""")
        )
    )

  def exclusionFlagIsNotFound(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/exclusion"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json"))
    )

  def exclusion500(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/exclusion"))
        .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json"))
    )

  def exclusion503(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/exclusion"))
        .willReturn(aResponse().withStatus(503).withHeader("Content-Type", "application/json"))
    )

  def dashboardDataIsFound(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/dashboard-data"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(dashboardDataJson)
        )
    )

  def dashboardDataIsNotFound(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/dashboard-data"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json"))
    )

  val dashboardDataJson: String =
    """
  {
    "personalDetails": {
      "forename": "John",
      "surname": "Densmore",
      "nino": "CS700100A",
      "address": {
      "addressLine1": "13 Front Street",
      "addressLine2": "Gosforth",
      "addressLine3": "Newcastle",
      "postCode": "NE43 7AY"
    },
      "rls": false,
      "wtcPaymentFrequency": "WEEKLY",
      "ctcPaymentFrequency": "WEEKLY",
      "dayPhoneNumber": "0191 393 3993",
      "work": {
      "numberOfPaidJobs": 0
    },
      "dateOfBirth": "1978-04-08",
      "title": "Mr"
    },
    "partnerDetails": {
      "forename": "Joan",
      "surname": "Densmore",
      "nino": "CS700200A",
      "address": {
      "addressLine1": "13 Front Street",
      "addressLine2": "Gosforth",
      "addressLine3": "Newcastle",
      "postCode": "NE43 7AY"
    },
      "rls": false,
      "wtcPaymentFrequency": "WEEKLY",
      "ctcPaymentFrequency": "WEEKLY",
      "dayPhoneNumber": "0191 393 3993",
      "work": {
      "numberOfPaidJobs": 0
    },
      "dateOfBirth": "1980-04-08",
      "title": "Mrs"
    },
    "childrenDetails": {
      "child": [
    {
      "firstNames": "Paul",
      "surname": "Cowling",
      "dateOfBirth": "2019-01-01",
      "hasFTNAE": false,
      "hasConnexions": false,
      "isActive": true,
      "isDropOff": true,
      "isEligibleForChildcare": true,
      "hasHadFtnaeOrConnexions": false
    },
    {
      "firstNames": "Sasha",
      "surname": "Cowling",
      "dateOfBirth": "2019-01-01",
      "hasFTNAE": false,
      "hasConnexions": false,
      "isActive": true,
      "isDropOff": true,
      "isEligibleForChildcare": true,
      "hasHadFtnaeOrConnexions": false
    },
    {
      "firstNames": "Eve",
      "surname": "Cowling",
      "dateOfBirth": "2019-01-01",
      "hasFTNAE": false,
      "hasConnexions": false,
      "isActive": true,
      "isDropOff": true,
      "isEligibleForChildcare": true,
      "hasHadFtnaeOrConnexions": false
    },
    {
      "firstNames": "Laura",
      "surname": "Cowling",
      "dateOfBirth": "2019-01-01",
      "hasFTNAE": false,
      "hasConnexions": false,
      "isActive": true,
      "isDropOff": true,
      "dateOfDeath": "2019-08-07",
      "isEligibleForChildcare": false,
      "hasHadFtnaeOrConnexions": false
    },
    {
      "firstNames": "Claire",
      "surname": "Cowling",
      "dateOfBirth": "2002-06-30",
      "hasFTNAE": true,
      "hasConnexions": false,
      "isActive": true,
      "isDropOff": true,
      "isEligibleForChildcare": false,
      "latestFTNAEEndDate": "2021-01-03",
      "childDisabilityPeriods": [
    {
      "childNumber": 10,
      "periodType": "SEVERE DISABILITY",
      "endDate": ""
    }
      ],
      "hasHadFtnaeOrConnexions": true
    },
    {
      "firstNames": "Micheal",
      "surname": "Cowling",
      "dateOfBirth": "2001-06-30",
      "hasFTNAE": true,
      "hasConnexions": false,
      "isActive": true,
      "isDropOff": true,
      "isEligibleForChildcare": false,
      "hasHadFtnaeOrConnexions": true
    },
    {
      "firstNames": "Erika",
      "surname": "Cowling",
      "dateOfBirth": "2003-06-30",
      "hasFTNAE": false,
      "hasConnexions": false,
      "isActive": true,
      "isDropOff": false,
      "dateOfDeath": "2019-08-07",
      "isEligibleForChildcare": false,
      "latestFTNAEEndDate": "2015-09-19",
      "hasHadFtnaeOrConnexions": true
    },
    {
      "firstNames": "Justine",
      "surname": "Cowling",
      "dateOfBirth": "2004-06-30",
      "hasFTNAE": false,
      "hasConnexions": false,
      "isActive": true,
      "isDropOff": true,
      "isEligibleForChildcare": false,
      "hasHadFtnaeOrConnexions": false
    },
    {
      "firstNames": "Mathias",
      "surname": "Cowling",
      "dateOfBirth": "2002-06-30",
      "hasFTNAE": true,
      "hasConnexions": true,
      "isActive": true,
      "isDropOff": true,
      "isEligibleForChildcare": false,
      "latestFTNAEEndDate": "2021-01-03",
      "childDisabilityPeriods": [
    {
      "childNumber": 16,
      "periodType": "DISABILITY",
      "endDate": ""
    }
      ],
      "hasHadFtnaeOrConnexions": true
    },
    {
      "firstNames": "Adam",
      "surname": "Cowling",
      "dateOfBirth": "2019-01-01",
      "hasFTNAE": false,
      "hasConnexions": false,
      "isActive": false,
      "isDropOff": false,
      "dateOfDeath": "2019-08-07",
      "isEligibleForChildcare": false,
      "hasHadFtnaeOrConnexions": false
    },
    {
      "firstNames": "Martin",
      "surname": "Cowling",
      "dateOfBirth": "2019-01-01",
      "hasFTNAE": false,
      "hasConnexions": false,
      "isActive": false,
      "isDropOff": false,
      "isEligibleForChildcare": false,
      "hasHadFtnaeOrConnexions": false
    },
    {
      "firstNames": "Sarah",
      "surname": "Cowling",
      "dateOfBirth": "2019-01-01",
      "hasFTNAE": true,
      "hasConnexions": false,
      "isActive": false,
      "isDropOff": false,
      "isEligibleForChildcare": false,
      "hasHadFtnaeOrConnexions": true
    },
    {
      "firstNames": "Jerry",
      "surname": "Cowling",
      "dateOfBirth": "2019-01-02",
      "hasFTNAE": true,
      "hasConnexions": false,
      "isActive": false,
      "isDropOff": false,
      "dateOfDeath": "2019-08-07",
      "isEligibleForChildcare": false,
      "latestFTNAEEndDate": "2021-01-03",
      "hasHadFtnaeOrConnexions": true
    },
    {
      "firstNames": "Amy",
      "surname": "Cowling",
      "dateOfBirth": "2003-06-30",
      "hasFTNAE": true,
      "hasConnexions": false,
      "isActive": false,
      "isDropOff": false,
      "isEligibleForChildcare": false,
      "latestFTNAEEndDate": "2021-01-03",
      "hasHadFtnaeOrConnexions": true
    },
    {
      "firstNames": "Mark",
      "surname": "Cowling",
      "dateOfBirth": "2002-06-30",
      "hasFTNAE": false,
      "hasConnexions": false,
      "isActive": false,
      "isDropOff": false,
      "isEligibleForChildcare": false,
      "latestFTNAEEndDate": "2015-09-12",
      "hasHadFtnaeOrConnexions": true
    },
    {
      "firstNames": "Tony",
      "surname": "Cowling",
      "dateOfBirth": "2001-06-30",
      "hasFTNAE": false,
      "hasConnexions": false,
      "isActive": false,
      "isDropOff": true,
      "isEligibleForChildcare": false,
      "hasHadFtnaeOrConnexions": false
    },
    {
      "firstNames": "Siri",
      "surname": "Cowling",
      "dateOfBirth": "2002-06-30",
      "hasFTNAE": false,
      "hasConnexions": false,
      "isActive": false,
      "isDropOff": false,
      "isEligibleForChildcare": false,
      "hasHadFtnaeOrConnexions": true
    }
      ]
    },
    "paymentSummary": {
      "workingTaxCredit": {
      "paymentSeq": [
    {
      "amount": 160.45,
      "paymentDate": "2020-01-09T00:00:00.000Z",
      "oneOffPayment": false
    },
    {
      "amount": 160.45,
      "paymentDate": "2020-01-16T00:00:00.000Z",
      "oneOffPayment": false
    },
    {
      "amount": 160.45,
      "paymentDate": "2020-01-23T00:00:00.000Z",
      "oneOffPayment": false
    },
    {
      "amount": 160.45,
      "paymentDate": "2020-01-30T00:00:00.000Z",
      "oneOffPayment": false
    },
    {
      "amount": 160.45,
      "paymentDate": "2020-02-06T00:00:00.000Z",
      "oneOffPayment": false
    },
    {
      "amount": 160.45,
      "paymentDate": "2020-02-13T00:00:00.000Z",
      "oneOffPayment": false
    },
    {
      "amount": 160.45,
      "paymentDate": "2020-02-20T00:00:00.000Z",
      "oneOffPayment": false
    },
    {
      "amount": 160.45,
      "paymentDate": "2020-02-27T00:00:00.000Z",
      "oneOffPayment": false
    }
      ],
      "paymentFrequency": "WEEKLY"
    },
      "childTaxCredit": {
      "paymentSeq": [
    {
      "amount": 140.12,
      "paymentDate": "2020-01-09T00:00:00.000Z",
      "oneOffPayment": false
    },
    {
      "amount": 140.12,
      "paymentDate": "2020-01-16T00:00:00.000Z",
      "oneOffPayment": false
    },
    {
      "amount": 140.12,
      "paymentDate": "2020-01-23T00:00:00.000Z",
      "oneOffPayment": false
    },
    {
      "amount": 140.12,
      "paymentDate": "2020-01-30T00:00:00.000Z",
      "oneOffPayment": false
    },
    {
      "amount": 140.12,
      "paymentDate": "2020-02-06T00:00:00.000Z",
      "oneOffPayment": false
    },
    {
      "amount": 140.12,
      "paymentDate": "2020-02-13T00:00:00.000Z",
      "oneOffPayment": false
    },
    {
      "amount": 140.12,
      "paymentDate": "2020-02-20T00:00:00.000Z",
      "oneOffPayment": false
    },
    {
      "amount": 140.12,
      "paymentDate": "2020-02-27T00:00:00.000Z",
      "oneOffPayment": false
    }
      ],
      "paymentFrequency": "WEEKLY"
    },
      "paymentEnabled": true
    },
    "actualIncomeStatus": {
      "applicant1": "ApplicantAllowed",
      "applicant2": "ApplicantNotApplicable"
    },
    "awardDetails": {
      "applicationId": "198765432134567",
      "isJoint": false,
      "claimantNumber": 1,
      "mainApplicantNino": "CS700100A",
      "availableForCOCAutomation": false,
      "renewalsJourney": false,
      "additionalCocInformation": {
      "contactInformation": {
      "primaryApplicantRegularNumber": "0191 393 3993"
    },
      "complianceCase": false,
      "houseHoldCreationDate": "2008-04-07",
      "householdNotes": [
    {
      "contentType": "XX01",
      "noteCreatedDate": "2008-11-19"
    }
      ],
      "appealCase": false,
      "dateOfBirth": "1978-04-08",
      "title": "Mr",
      "address": {
      "addressLine1": "13 Front Street",
      "addressLine2": "Gosforth",
      "addressLine3": "Newcastle",
      "postCode": "NE43 7AY"
    },
      "numberOfHours": 0
    }
    }
  } """
}
