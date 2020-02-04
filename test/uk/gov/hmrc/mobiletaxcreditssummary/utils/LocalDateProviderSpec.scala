/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatest.{Matchers, Tag, TestData, WordSpecLike}
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting

class LocalDateProviderSpec extends WordSpecLike with Matchers with GuiceOneAppPerTest with Injecting {

  "LocalDateProviderSpec" should {

    "return today's date if no config found" in {
      val localDateProvider = app.injector.instanceOf[LocalDateProvider]
      localDateProvider.now shouldBe LocalDate.now()
    }

    "return today's date with config set to 2019-09-12" taggedAs Tag("2019-09-12") in {
      val localDateProvider = app.injector.instanceOf[LocalDateProvider]
      localDateProvider.now shouldBe LocalDate.of(2019, Month.SEPTEMBER, 12)
    }
  }

  override def newAppForTest(testData: TestData): Application =
    testData.tags.headOption match {
      case Some(tag) =>
        GuiceApplicationBuilder()
          .configure("dateOverride" -> tag)
          .disable[com.kenshoo.play.metrics.PlayModule]
          .configure("metrics.enabled" -> false)
          .build()
      case _ =>
        GuiceApplicationBuilder()
          .configure("dateOverride" -> LocalDate.now().toString)
          .disable[com.kenshoo.play.metrics.PlayModule]
          .configure("metrics.enabled" -> false)
          .build()
    }
}
