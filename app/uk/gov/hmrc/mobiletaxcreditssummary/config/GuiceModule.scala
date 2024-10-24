/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.mobiletaxcreditssummary.config

import com.google.inject.AbstractModule
import com.google.inject.name.Names.named
import com.typesafe.config.Config
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.mobiletaxcreditssummary.controllers.api.ApiAccess
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig


class GuiceModule(
  environment:   Environment,
  configuration: Configuration)
    extends AbstractModule {

  val servicesConfig = new ServicesConfig(configuration)

  override def configure(): Unit = {
    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector])

    bindConfigInt("controllers.confidenceLevel")
    bindConfigString("appUrl", "appUrl")

    bind(classOf[String])
      .annotatedWith(named("tax-credits-broker"))
      .toInstance(servicesConfig.baseUrl("tax-credits-broker"))
    bind(classOf[String])
      .annotatedWith(named("mobile-tax-credits-renewal"))
      .toInstance(servicesConfig.baseUrl("mobile-tax-credits-renewal"))
    bind(classOf[String])
      .annotatedWith(named("mobile-shuttering"))
      .toInstance(servicesConfig.baseUrl("mobile-shuttering"))
    bindConfigString("reportActualProfitPeriod.startDate", "microservice.reportActualProfitPeriod.startDate")
    bindConfigString("reportActualProfitPeriod.endDate", "microservice.reportActualProfitPeriod.endDate")
    bindConfigString("renewalsStartDate", "microservice.renewals.startDate")
    bindConfigString("renewalsPackReceivedDate", "microservice.renewals.packReceivedDate")
    bindConfigString("renewalsEndDate", "microservice.renewals.endDate")
    bindConfigString("renewalsGracePeriodEndDate", "microservice.renewals.gracePeriodEndDate")
    bindConfigString("renewalsEndViewDate", "microservice.renewals.endViewRenewalsDate")

    bind(classOf[ApiAccess]).toInstance(ApiAccess("PRIVATE"))
  }

  class DefaultConfig(val config: Config)

  /**
    * Binds a configuration value using the `path` as the name for the binding.
    * Throws an exception if the configuration value does not exist or cannot be read as an Int.
    */
  private def bindConfigInt(path: String): Unit =
    bindConstant()
      .annotatedWith(named(path))
      .to(configuration.underlying.getInt(path))

  private def bindConfigString(
    name: String,
    path: String
  ): Unit =
    bindConstant()
      .annotatedWith(named(name))
      .to(configuration.underlying.getString(path))
}
