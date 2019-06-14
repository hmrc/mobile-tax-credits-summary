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

package uk.gov.hmrc.mobiletaxcreditssummary.utils

import java.time.{LocalDate, Month}

import javax.inject.Inject
import org.scalatest.{Matchers, Tag, TestData, WordSpecLike}
import org.scalatestplus.play.OneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

class LocalDateProviderSpec @Inject()(localDateProvider: LocalDateProvider) extends WordSpecLike with Matchers with OneAppPerTest {


  "LocalDateProviderSpec" should {

    "return today's date if no config found" in {
      localDateProvider.now shouldBe LocalDate.now()
    }

    "return today's date with config set to 2019-09-12" taggedAs Tag("2019-09-12") in {
      localDateProvider.now shouldBe LocalDate.of(2019, Month.SEPTEMBER, 12)
    }
  }


  override def newAppForTest(testData: TestData): Application = {
    testData.tags.headOption match {
      case Some(tag) =>  GuiceApplicationBuilder().configure("dateOverride" -> tag).build()
      case _ => GuiceApplicationBuilder().configure("dateOverride" -> LocalDate.now().toString).build()
    }
  }
}
