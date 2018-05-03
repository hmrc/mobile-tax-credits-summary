/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.mobiletaxcreditssummary.stubs

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.Configuration
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobiletaxcreditssummary.connectors.TaxCreditsBrokerConnector
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata.TaxCreditSummary
import uk.gov.hmrc.mobiletaxcreditssummary.services.{LiveTaxCreditsSummaryService, TaxCreditsSummaryService}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

trait TaxCreditsSummaryServiceStub extends UnitSpec {

  def stubTaxCreditSummary(response: TaxCreditSummary)(implicit personalIncomeService: TaxCreditsSummaryService): OngoingStubbing[Future[TaxCreditSummary]] = {
    when(personalIncomeService.getTaxCreditSummary(any[Nino]())(any[HeaderCarrier](), any[ExecutionContext]())).thenReturn(response)
  }

  class TestTaxCreditsSummaryService(val taxCreditBrokerConnector: TaxCreditsBrokerConnector,
                                     override val auditConnector: AuditConnector,
                                     configuration: Configuration)
    extends LiveTaxCreditsSummaryService(taxCreditBrokerConnector, auditConnector, configuration)

}