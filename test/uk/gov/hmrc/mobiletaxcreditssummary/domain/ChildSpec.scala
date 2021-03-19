/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDate

import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json.Json
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.Child

class ChildSpec extends WordSpecLike with Matchers {

  "Child DOB calculation" should {
    "using today's date for child DOB will result in age 0" in {
      val childA =
        Child("first", "second", LocalDate.now, hasFTNAE = false, hasConnexions = false, isActive = false, None)

      Child.getAge(childA) shouldBe 0
    }

    "using today's date - 16 years for child DOB will result in age 16" in {
      val childB = Child("first",
                         "second",
                         LocalDate.now.minusYears(16),
                         hasFTNAE      = false,
                         hasConnexions = false,
                         isActive      = false,
                         None)

      Child.getAge(childB) shouldBe 16
    }

    "using today's date - 16 years and +1 month for child DOB will result in age 15" in {
      val childC =
        Child("first",
              "second",
              LocalDate.now.minusYears(16).plusMonths(1),
              hasFTNAE      = false,
              hasConnexions = false,
              isActive      = false,
              None)

      Child.getAge(childC) shouldBe 15
    }
  }

  "Converting Child to json" should {
    "render the birth date as a Long" in {
      val expected = Json.parse(
        """{"firstNames":"","surname":"","dateOfBirth":1546300800000,"hasFTNAE":false,"hasConnexions":false,"isActive":false}"""
      )
      val child =
        Child("", "", LocalDate.parse("2019-01-01"), hasFTNAE = false, hasConnexions = false, isActive = false, None)
      jsonDiff(None, Json.toJson(child), expected) shouldBe 'empty
    }
  }
}
