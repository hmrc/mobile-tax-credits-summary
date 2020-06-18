package uk.gov.hmrc.mobiletaxcreditssummary.stubs

import java.time.{LocalDate, ZoneId}

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlPathEqualTo}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.SpecialCircumstance

object TaxCreditsBrokerStub {

//  val childrenJson =
//    s"""{ "child": [
//        {
//          "firstNames": "Sarah",
//          "surname": "Smith",
//          "dateOfBirth": ${LocalDate
//      .of(LocalDate.now.minusYears(19).getYear, 8, 31)
//      .atStartOfDay(ZoneId.systemDefault())
//      .toEpochSecond() * 1000},
//          "hasFTNAE": false,
//          "hasConnexions": false,
//          "isActive": true
//        },
//        {
//          "firstNames": "Joseph",
//          "surname": "Smith",
//          "dateOfBirth": 884304000000,
//          "hasFTNAE": false,
//          "hasConnexions": false,
//          "isActive": true
//        },
//        {
//          "firstNames": "Mary",
//          "surname": "Smith",
//          "dateOfBirth": 852768000000,
//          "hasFTNAE": false,
//          "hasConnexions": false,
//          "isActive": true
//        } ] }"""
//
//  def childrenAreFound(nino: Nino): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${nino.value}/children"))
//        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(childrenJson))
//    )
//
//  def childrenAreNotFound(nino: Nino): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${nino.value}/children"))
//        .willReturn(
//          aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("""{ "child": [] }""")
//        )
//    )
//
//  def children500(nino: Nino): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${nino.value}/children"))
//        .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json"))
//    )
//
//  def children503(nino: Nino): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${nino.value}/children"))
//        .willReturn(aResponse().withStatus(503).withHeader("Content-Type", "application/json"))
//    )
//
//  def partnerJson(nino: Nino): String =
//    s"""{
//        "forename": "Frederick",
//        "otherForenames": "Tarquin",
//        "surname": "Hunter-Smith",
//        "nino": "${nino.value}",
//        "address": {
//          "addressLine1": "999 Big Street",
//          "addressLine2": "Worthing",
//          "addressLine3": "West Sussex",
//          "postCode": "BN99 8IG"
//        }
//      }""".stripMargin
//
//  def partnerDetailsAreFound(
//    climantsNino: Nino,
//    partnersNino: Nino
//  ): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${climantsNino.value}/partner-details"))
//        .willReturn(
//          aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(partnerJson(partnersNino))
//        )
//    )
//
//  def partnerDetailsAreNotFound(
//    climantsNino: Nino,
//    partnersNino: Nino
//  ): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${climantsNino.value}/partner-details"))
//        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json"))
//    )
//
//  def partnerDetails500(
//    climantsNino: Nino,
//    partnersNino: Nino
//  ): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${climantsNino.value}/partner-details"))
//        .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json"))
//    )
//
//  def partnerDetails503(
//    climantsNino: Nino,
//    partnersNino: Nino
//  ): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${climantsNino.value}/partner-details"))
//        .willReturn(aResponse().withStatus(503).withHeader("Content-Type", "application/json"))
//    )
//
//  val paymentSummaryJson: String =
//    """
//      |{
//      |    "workingTaxCredit": {
//      |      "paymentSeq": [
//      |        {
//      |          "amount": 55,
//      |          "paymentDate": 1509008158781,
//      |          "oneOffPayment": false,
//      |          "earlyPayment": false
//      |        },
//      |        {
//      |          "amount": 55,
//      |          "paymentDate": 1511690158781,
//      |          "oneOffPayment": false,
//      |          "holidayType": "bankHoliday",
//      |          "earlyPayment": true,
//      |          "explanatoryText" : "Your payment is early because of UK bank holidays."
//      |        },
//      |        {
//      |          "amount": 55,
//      |          "paymentDate": 1514282158781,
//      |          "oneOffPayment": true,
//      |          "earlyPayment": false,
//      |          "explanatoryText" : "One-off payment because of a recent change to help you get the right amount of tax credits."
//      |        }
//      |      ],
//      |      "paymentFrequency": "WEEKLY",
//      |      "previousPaymentSeq": [
//      |        {
//      |          "amount": 33,
//      |          "paymentDate": 1503737758781,
//      |          "oneOffPayment": false,
//      |          "earlyPayment": false
//      |        },
//      |        {
//      |          "amount": 43,
//      |          "paymentDate": 1501059358781,
//      |          "oneOffPayment": false,
//      |          "holidayType": "bankHoliday",
//      |          "earlyPayment": true,
//      |          "explanatoryText" : "Your payment was early because of UK bank holidays."
//      |        },
//      |        {
//      |          "amount": 53,
//      |          "paymentDate": 1498467358781,
//      |          "oneOffPayment": true,
//      |          "earlyPayment": false,
//      |          "explanatoryText" : "This was because of a recent change and was to help you get the right amount of tax credits."
//      |        }
//      |      ]
//      |    },
//      |    "childTaxCredit": {
//      |      "paymentSeq": [
//      |        {
//      |          "amount": 55,
//      |          "paymentDate": 1509008158781,
//      |          "oneOffPayment": false,
//      |          "earlyPayment": false
//      |        },
//      |        {
//      |          "amount": 55,
//      |          "paymentDate": 1511690158781,
//      |          "oneOffPayment": false,
//      |          "holidayType": "bankHoliday",
//      |          "earlyPayment": true,
//      |          "explanatoryText" : "Your payment is early because of UK bank holidays."
//      |        },
//      |        {
//      |          "amount": 55,
//      |          "paymentDate": 1514282158781,
//      |          "oneOffPayment": true,
//      |          "earlyPayment": false,
//      |          "explanatoryText" : "One-off payment because of a recent change to help you get the right amount of tax credits."
//      |        }
//      |      ],
//      |      "paymentFrequency": "WEEKLY"
//      |    },
//      |    "paymentEnabled": true,
//      |    "totalsByDate": [
//      |      {
//      |        "amount": 110,
//      |        "paymentDate": 1509008158781
//      |      },
//      |      {
//      |        "amount": 110,
//      |        "paymentDate": 1511690158781
//      |      },
//      |      {
//      |        "amount": 110,
//      |        "paymentDate": 1514282158781
//      |      }
//      |    ],
//      |    "previousTotalsByDate": [
//      |      {
//      |        "amount": 53,
//      |        "paymentDate": 1498467358781
//      |      },
//      |      {
//      |        "amount": 43,
//      |        "paymentDate": 1501059358781
//      |      },
//      |      {
//      |        "amount": 33,
//      |        "paymentDate": 1503737758781
//      |      }
//      |    ]
//      |  }
//    """.stripMargin
//
//  def paymentSummaryWithSpecialCircumstancesJson(specialCircumstance: String): String =
//    s"""
//       |{
//       |    "workingTaxCredit": {
//       |      "paymentSeq": [
//       |        {
//       |          "amount": 55,
//       |          "paymentDate": 1509008158781,
//       |          "oneOffPayment": false,
//       |          "earlyPayment": false
//       |        },
//       |        {
//       |          "amount": 55,
//       |          "paymentDate": 1511690158781,
//       |          "oneOffPayment": false,
//       |          "holidayType": "bankHoliday",
//       |          "earlyPayment": true,
//       |          "explanatoryText" : "Your payment is early because of UK bank holidays."
//       |        },
//       |        {
//       |          "amount": 55,
//       |          "paymentDate": 1514282158781,
//       |          "oneOffPayment": true,
//       |          "earlyPayment": false,
//       |          "explanatoryText" : "One-off payment because of a recent change to help you get the right amount of tax credits."
//       |        }
//       |      ],
//       |      "paymentFrequency": "WEEKLY",
//       |      "previousPaymentSeq": [
//       |        {
//       |          "amount": 33,
//       |          "paymentDate": 1503737758781,
//       |          "oneOffPayment": false,
//       |          "earlyPayment": false
//       |        },
//       |        {
//       |          "amount": 43,
//       |          "paymentDate": 1501059358781,
//       |          "oneOffPayment": false,
//       |          "holidayType": "bankHoliday",
//       |          "earlyPayment": true,
//       |          "explanatoryText" : "Your payment was early because of UK bank holidays."
//       |        },
//       |        {
//       |          "amount": 53,
//       |          "paymentDate": 1498467358781,
//       |          "oneOffPayment": true,
//       |          "earlyPayment": false,
//       |          "explanatoryText" : "This was because of a recent change and was to help you get the right amount of tax credits."
//       |        }
//       |      ]
//       |    },
//       |    "childTaxCredit": {
//       |      "paymentSeq": [
//       |        {
//       |          "amount": 55,
//       |          "paymentDate": 1509008158781,
//       |          "oneOffPayment": false,
//       |          "earlyPayment": false
//       |        },
//       |        {
//       |          "amount": 55,
//       |          "paymentDate": 1511690158781,
//       |          "oneOffPayment": false,
//       |          "holidayType": "bankHoliday",
//       |          "earlyPayment": true,
//       |          "explanatoryText" : "Your payment is early because of UK bank holidays."
//       |        },
//       |        {
//       |          "amount": 55,
//       |          "paymentDate": 1514282158781,
//       |          "oneOffPayment": true,
//       |          "earlyPayment": false,
//       |          "explanatoryText" : "One-off payment because of a recent change to help you get the right amount of tax credits."
//       |        }
//       |      ],
//       |      "paymentFrequency": "WEEKLY"
//       |    },
//       |    "paymentEnabled": true,
//       |    "specialCircumstances": "$specialCircumstance",
//       |    "totalsByDate": [
//       |      {
//       |        "amount": 110,
//       |        "paymentDate": 1509008158781
//       |      },
//       |      {
//       |        "amount": 110,
//       |        "paymentDate": 1511690158781
//       |      },
//       |      {
//       |        "amount": 110,
//       |        "paymentDate": 1514282158781
//       |      }
//       |    ],
//       |    "previousTotalsByDate": [
//       |      {
//       |        "amount": 53,
//       |        "paymentDate": 1498467358781
//       |      },
//       |      {
//       |        "amount": 43,
//       |        "paymentDate": 1501059358781
//       |      },
//       |      {
//       |        "amount": 33,
//       |        "paymentDate": 1503737758781
//       |      }
//       |    ]
//       |  }
//    """.stripMargin
//
//  def paymntSummaryIsFound(nino: Nino): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${nino.value}/payment-summary"))
//        .willReturn(
//          aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(paymentSummaryJson)
//        )
//    )
//
//  def paymntSummaryWithSpecialCircumstanceIsFound(
//    nino:                Nino,
//    specialCircumstance: String
//  ): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${nino.value}/payment-summary"))
//        .willReturn(
//          aResponse()
//            .withStatus(200)
//            .withHeader("Content-Type", "application/json")
//            .withBody(paymentSummaryWithSpecialCircumstancesJson(specialCircumstance))
//        )
//    )
//
//  def paymntSummaryNonTCUser(nino: Nino): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${nino.value}/payment-summary"))
//        .willReturn(
//          aResponse()
//            .withStatus(200)
//            .withHeader("Content-Type", "application/json")
//            .withBody("""{ "excluded": true }""")
//        )
//    )
//
//  def paymntSummary500(nino: Nino): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${nino.value}/payment-summary"))
//        .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json"))
//    )
//
//  def paymntSummary503(nino: Nino): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${nino.value}/payment-summary"))
//        .willReturn(aResponse().withStatus(503).withHeader("Content-Type", "application/json"))
//    )
//
//  def paymentSummary404(nino: Nino): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${nino.value}/payment-summary"))
//        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json"))
//    )
//
//  def personalDetailsJson(nino: Nino) =
//    s"""
//      {
//          "forename": "Nuala",
//          "surname": "O'Shea",
//          "nino": "${nino.value}",
//          "address": {
//            "addressLine1": "999 Big Street",
//            "addressLine2": "Worthing",
//            "addressLine3": "West Sussex",
//            "postCode": "BN99 8IG"
//         }
//        }
//    """
//
//  def personalDetailsAreFound(nino: Nino): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${nino.value}/personal-details"))
//        .willReturn(
//          aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(personalDetailsJson(nino))
//        )
//    )
//
//  def personalDetailsAreNotFound(nino: Nino): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${nino.value}/personal-details"))
//        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json"))
//    )
//
//  def personalDetails500(nino: Nino): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${nino.value}/personal-details"))
//        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json"))
//    )
//
//  def personalDetails503(nino: Nino): Unit =
//    stubFor(
//      get(urlPathEqualTo(s"/tcs/${nino.value}/personal-details"))
//        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json"))
//    )

  def exclusionFlagIsFound(
    nino:     Nino,
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

  def dashboardDataIsFound(nino: Nino, partnerNino: Nino, specialCircumstance: String = ""): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/dashboard-data"))
        .willReturn(
          aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(dashboardDataJson(nino, partnerNino, specialCircumstance))
        )
    )

  def dashboardDataIsNotFound(nino: Nino): Unit =
    stubFor(
      get(urlPathEqualTo(s"/tcs/${nino.value}/dashboard-data"))
        .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json"))
    )

  def dashboardDataJson(nino: Nino, partnerNino: Nino, specialCircumstance: String): String =
    s"""
  {
    "personalDetails": {
          "forename": "Nuala",
          "surname": "O'Shea",
          "nino": "${nino.value}",
          "address": {
            "addressLine1": "999 Big Street",
            "addressLine2": "Worthing",
            "addressLine3": "West Sussex",
            "postCode": "BN99 8IG"
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
      "forename": "Frederick",
      "otherForenames": "Tarquin",
      "surname": "Hunter-Smith",
      "nino": "${partnerNino.value}",
      "address": {
        "addressLine1": "999 Big Street",
        "addressLine2": "Worthing",
        "addressLine3": "West Sussex",
        "postCode": "BN99 8IG"
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
          "firstNames": "Sarah",
          "surname": "Smith",
          "dateOfBirth": ${LocalDate
      .of(LocalDate.now.minusYears(19).getYear, 8, 31)
      .atStartOfDay(ZoneId.systemDefault())
      .toEpochSecond() * 1000},
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
        }
      ]
    },
    "paymentSummary": {
      "workingTaxCredit": {
      "paymentSeq": [
        {
          "amount": 55,
          "paymentDate": 1509008158781,
          "oneOffPayment": false,
          "earlyPayment": false
        },
        {
          "amount": 55,
          "paymentDate": 1511690158781,
          "oneOffPayment": false,
          "holidayType": "bankHoliday",
          "earlyPayment": true,
          "explanatoryText" : "Your payment is early because of UK bank holidays."
        },
        {
          "amount": 55,
          "paymentDate": 1514282158781,
          "oneOffPayment": true,
          "earlyPayment": false,
          "explanatoryText" : "One-off payment because of a recent change to help you get the right amount of tax credits."
        }
      ],
      "paymentFrequency": "WEEKLY",
      "previousPaymentSeq": [
        {
          "amount": 33,
          "paymentDate": 1503737758781,
          "oneOffPayment": false,
          "earlyPayment": false
        },
        {
          "amount": 43,
          "paymentDate": 1501059358781,
          "oneOffPayment": false,
          "holidayType": "bankHoliday",
          "earlyPayment": true,
          "explanatoryText" : "Your payment was early because of UK bank holidays."
        },
        {
          "amount": 53,
          "paymentDate": 1498467358781,
          "oneOffPayment": true,
          "earlyPayment": false,
          "explanatoryText" : "This was because of a recent change and was to help you get the right amount of tax credits."
        }
      ]
      },
      "childTaxCredit": {
        "paymentSeq": [
          {
            "amount": 55,
            "paymentDate": 1509008158781,
            "oneOffPayment": false,
            "earlyPayment": false
          },
          {
            "amount": 55,
            "paymentDate": 1511690158781,
            "oneOffPayment": false,
            "holidayType": "bankHoliday",
            "earlyPayment": true,
            "explanatoryText" : "Your payment is early because of UK bank holidays."
          },
          {
            "amount": 55,
            "paymentDate": 1514282158781,
            "oneOffPayment": true,
            "earlyPayment": false,
            "explanatoryText" : "One-off payment because of a recent change to help you get the right amount of tax credits."
          }
        ],
        "paymentFrequency": "WEEKLY"
      },
      "paymentEnabled": true,
      "specialCircumstances": "$specialCircumstance",
      "totalsByDate": [
        {
          "amount": 110,
          "paymentDate": 1509008158781
        },
        {
          "amount": 110,
          "paymentDate": 1511690158781
        },
        {
          "amount": 110,
          "paymentDate": 1514282158781
        }
      ],
      "previousTotalsByDate": [
        {
          "amount": 53,
          "paymentDate": 1498467358781
        },
        {
          "amount": 43,
          "paymentDate": 1501059358781
        },
        {
          "amount": 33,
          "paymentDate": 1503737758781
        }
      ]
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
