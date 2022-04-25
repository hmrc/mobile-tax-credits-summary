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

package uk.gov.hmrc.mobiletaxcreditssummary.controllers

import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc._
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobiletaxcreditssummary.domain.{ChangeOfCircumstanceLinks, Shuttering}
import uk.gov.hmrc.mobiletaxcreditssummary.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SandboxTaxCreditsSummaryController @Inject() (
  cc:                            ControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends BackendController(cc)
    with TaxCreditsSummaryController
    with FileResource
    with HeaderValidator {

  private final val WebServerIsDown = new Status(521)

  val sandboxChangeOfCircumstanceLinks: Option[ChangeOfCircumstanceLinks] = Some(
    ChangeOfCircumstanceLinks("/", "/", "/", "/")
  )

  private val shuttered =
    Json.toJson(
      Shuttering(shuttered = true,
                 title     = Some("Shuttered"),
                 message   = Some("Tax Credits Summary is currently shuttered"))
    )

  override def parser: BodyParser[AnyContent] = cc.parsers.anyContent

  override final def taxCreditsSummary(
    nino:      Nino,
    journeyId: JourneyId
  ): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      Future successful (request.headers.get("SANDBOX-CONTROL") match {
        case Some("NON-TAX-CREDITS-USER") => Ok(toJson(TaxCreditsSummaryResponse(taxCreditsSummary = None)))
        case Some("EXCLUDED-TAX-CREDITS-USER") =>
          Ok(toJson(TaxCreditsSummaryResponse(excluded = true, taxCreditsSummary = None)))
        case Some("ERROR-401") => Unauthorized
        case Some("ERROR-403") => Forbidden
        case Some("ERROR-500") => InternalServerError
        case Some("WORKING-TAX-CREDIT-ONLY") =>
          Ok(
            toJson(
              TaxCreditsSummaryResponse(excluded = false,
                                        readData("working-tax-credit-only.json"),
                                        changeOfCircumstanceLinks = sandboxChangeOfCircumstanceLinks)
            )
          )
        case Some("CHILD-TAX-CREDIT-ONLY") =>
          Ok(
            toJson(
              TaxCreditsSummaryResponse(excluded = false,
                                        readData("child-tax-credit-only.json"),
                                        changeOfCircumstanceLinks = sandboxChangeOfCircumstanceLinks)
            )
          )
        case Some("PXP5") =>
          Ok(
            toJson(
              TaxCreditsSummaryResponse(excluded = false,
                                        readData("pxp5.json"),
                                        changeOfCircumstanceLinks = sandboxChangeOfCircumstanceLinks)
            )
          )
        case Some("OLD-RATE") =>
          Ok(
            toJson(
              TaxCreditsSummaryResponse(excluded = false,
                                        readData("old-rate.json"),
                                        changeOfCircumstanceLinks = sandboxChangeOfCircumstanceLinks)
            )
          )
        case Some("NEW-RATE") =>
          Ok(
            toJson(
              TaxCreditsSummaryResponse(excluded = false,
                                        readData("new-rate.json"),
                                        changeOfCircumstanceLinks = sandboxChangeOfCircumstanceLinks)
            )
          )
        case Some("PAYMENTS-NOT-ENABLED") =>
          Ok(
            toJson(
              TaxCreditsSummaryResponse(excluded = false,
                                        readData("payments-not-enabled.json"),
                                        changeOfCircumstanceLinks = sandboxChangeOfCircumstanceLinks)
            )
          )
        case Some("RENEWALS-ACTIVE") =>
          val resource: String = findResource(s"/resources/taxcreditssummary/CS700100A-with-renewals.json")
            .getOrElse(throw new IllegalArgumentException("Resource not found!"))
          val response =
            TaxCreditsSummaryResponse(excluded = false,
                                      Some(Json.parse(updateDates(resource)).as[TaxCreditsSummary]),
                                      changeOfCircumstanceLinks = sandboxChangeOfCircumstanceLinks)
          Ok(toJson(response))
        case Some("CLAIMANTS-FAILURE") =>
          val resource: String = findResource(s"/resources/taxcreditssummary/${nino.value}.json")
            .getOrElse(throw new IllegalArgumentException("Resource not found!"))
          val taxCreditsSummary: TaxCreditsSummary =
            TaxCreditsSummary(Json.parse(updateDates(resource)).as[TaxCreditsSummary].paymentSummary, None, None)
          val response = TaxCreditsSummaryResponse(excluded = false,
                                                   Some(taxCreditsSummary),
                                                   changeOfCircumstanceLinks = sandboxChangeOfCircumstanceLinks)
          Ok(toJson(response))
        case Some("SHUTTERED") => WebServerIsDown(shuttered)
        case _ => //TAX-CREDITS-USER
          val resource: String = findResource(s"/resources/taxcreditssummary/${nino.value}.json")
            .getOrElse(throw new IllegalArgumentException("Resource not found!"))
          val response =
            TaxCreditsSummaryResponse(excluded = false,
                                      Some(Json.parse(updateDates(resource)).as[TaxCreditsSummary]),
                                      changeOfCircumstanceLinks = sandboxChangeOfCircumstanceLinks)
          Ok(toJson(response))
      })
    }

  private def readData(resource: String) =
    Some(
      Json
        .parse(
          updateDates(
            findResource(s"/resources/taxcreditssummary/$resource")
              .getOrElse(throw new IllegalArgumentException("Resource not found!"))
          )
        )
        .as[TaxCreditsSummary]
    )

  private def updateDates(resource: String): String = {
    val currentTime = new LocalDate().toDateTimeAtStartOfDay
    resource
      .replaceAll("previousDate1", currentTime.minusWeeks(2).getMillis.toString)
      .replaceAll("previousDate2", currentTime.minusWeeks(1).getMillis.toString)
      .replaceAll("previousDate3", currentTime.getMillis.toString)
      .replaceAll("date1", currentTime.plusWeeks(1).getMillis.toString)
      .replaceAll("date2", currentTime.plusWeeks(2).getMillis.toString)
      .replaceAll("date3", currentTime.plusWeeks(3).getMillis.toString)
      .replaceAll("date4", currentTime.plusWeeks(4).getMillis.toString)
      .replaceAll("date5", currentTime.plusWeeks(5).getMillis.toString)
      .replaceAll("date6", currentTime.plusWeeks(6).getMillis.toString)
      .replaceAll("date7", currentTime.plusWeeks(7).getMillis.toString)
      .replaceAll("date8", currentTime.plusWeeks(8).getMillis.toString)
      .replaceAll("year", currentTime.getYear.toString)
      .replaceAll("packReceivedDateValue", currentTime.minusWeeks(2).toString)
      .replaceAll("renewalsEndDateValue", currentTime.plusWeeks(4).toString)
      .replaceAll("viewRenewalsEndDateValue", currentTime.plusWeeks(8).toString)
  }
}
