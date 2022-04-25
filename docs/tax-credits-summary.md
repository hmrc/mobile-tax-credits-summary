The Tax Credits Summary response
----
Fetch the Tax Credits Summary object for a given nino.

* **URL**

  `/income/:nino/tax-credits/tax-credits-summary`

* **Method:**

  `GET`

* **URL Params**

  **Required:**

  `nino=[Nino]`

  The nino given must be a valid
  nino. ([http://www.hmrc.gov.uk/manuals/nimmanual/nim39110.htm](http://www.hmrc.gov.uk/manuals/nimmanual/nim39110.htm))

  **Required:**

  `journeyId=[String]`

  a string which is included for journey tracking purposes but has no functional impact

* **Success Responses:**

    * **Code:** 200 <br />
      **Note:** If the user is not excluded <br />
      **Content:**

```json
{
  "excluded": false,
  "paymentSummary": {
    "workingTaxCredit": {
      "paymentSeq": [
        {
          "amount": 55,
          "paymentDate": 1509008158781,
          "oneOffPayment": false,
          "earlyPayment": false
        }
      ],
      "paymentFrequency": "WEEKLY",
      "previousPaymentSeq": [
        {
          "amount": 33,
          "paymentDate": 1503737758781,
          "oneOffPayment": false,
          "earlyPayment": false
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
        }
      ],
      "paymentFrequency": "WEEKLY"
    },
    "paymentEnabled": true,
    "totalsByDate": [
      {
        "amount": 110,
        "paymentDate": 1509008158781
      }
    ],
    "previousTotalsByDate": [
      {
        "amount": 53,
        "paymentDate": 1498467358781
      }
    ]
  },
  "claimants": {
    "ftnaeLink": {
      "link": "/",
      "preFtnaeDeadline": true
    },
    "personalDetails": {
      "forename": "firstname",
      "surname": "surname"
    },
    "partnerDetails": {
      "forename": "Frederick",
      "otherForenames": "Tarquin",
      "surname": "Hunter-Smith"
    },
    "children": [
      {
        "forename": "Sarah",
        "surname": "Smith"
      }
    ]
  },
  "changeOfCircumstanceLinks": {
    "changePersonalDetails": "/tax-credits-service/home/your-details",
    "changeJobsOrIncome": "/tax-credits-service/home/jobs-and-income",
    "addEditChildrenChildcare": "/tax-credits-service/home/children-and-childcare",
    "otherChanges": "/tax-credits-service/home/other-changes"
  }
}
```

* **Code:** 200 <br />
  **Note:** If the user is excluded <br />
  **Content:**

```json
{
  "excluded": true
}
```

* **Code:** 200 <br />
  **Note:** If the user is a non tax credits user <br />
  **Content:**

```json
{
  "excluded": false
}
```

* **Error Responses:**

    * **Code:** 401 UNAUTHORIZED <br/>
      **Content:** `{"code":"UNAUTHORIZED","message":"Bearer token is missing or not authorized for access"}`

    * **Code:** 403 FORBIDDEN <br/>
      **Content:** `{"code":"FORBIDDEN","message":Authenticated user is not authorised for this resource"}`

    * **Code:** 404 NOTFOUND <br/>
      **
      Content:** `{ "code" : "MATCHING_RESOURCE_NOT_FOUND", "message" : "A resource with the name in the request can not be found in the API" }`

  OR when a user does not exist or server failure

    * **Code:** 500 INTERNAL_SERVER_ERROR <br/>



