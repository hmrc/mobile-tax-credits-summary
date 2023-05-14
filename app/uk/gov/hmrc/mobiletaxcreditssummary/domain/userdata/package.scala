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

package uk.gov.hmrc.mobiletaxcreditssummary.domain
import java.time.{LocalDate, ZoneOffset}
import play.api.libs.json.Reads.DefaultLocalDateReads
import play.api.libs.json.{Format, JsNumber, JsResult, JsValue, Writes}

package object userdata {

  /**
    * We want `LocalDate`s returned via the api to be rendered as `Longs`.
    * Play's default is to render as a formatted string, but by supplying these `Writes` and `Format`
    * instances in the package they will be picked up in preference to the defaults.
    */


  implicit val localDateWrites: Writes[LocalDate] = new Writes[LocalDate] {

    override def writes(o: LocalDate): JsValue =
      JsNumber(o.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli)
  }

  implicit val LocalDateFormat: Format[LocalDate] = new Format[LocalDate] {

    override def writes(o: LocalDate): JsValue =
      localDateWrites.writes(o)

    override def reads(json: JsValue): JsResult[LocalDate] =
      DefaultLocalDateReads.reads(json)
  }
}
