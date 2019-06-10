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

package uk.gov.hmrc.mobiletaxcreditssummary.domain.userdata

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, Period}

import play.api.libs.json.{Json, OFormat}

case class Child(
  firstNames:     String,
  surname:        String,
  dateOfBirth:    LocalDate,
  hasFTNAE:       Boolean,
  hasConnections: Boolean,
  isActive:       Boolean,
  dateOfDeath:    Option[LocalDate])

object Child {
  implicit val formats: OFormat[Child] = Json.format[Child]

  def getAge(child: Child): Long =
    Period.between(child.dateOfBirth, LocalDate.now).get(ChronoUnit.YEARS)

  def getEligibleChildren(children: Seq[Child]): Seq[Child] =
    children
      .filter { child =>
        getAge(child) < 20 &&
        child.isActive &&
        child.dateOfDeath.isEmpty
      }

  def hasFTNAEChildren(children: Seq[Child]): Boolean =
    children.exists {
      child => child.hasFTNAE
    }

  def countFtnaeChildren(children: Seq[Child]): Int = {
    children.count(child => child.hasFTNAE)
  }
}
