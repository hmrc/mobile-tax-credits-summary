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

package uk.gov.hmrc.mobiletaxcreditssummary
import play.api.libs.json.{JsArray, JsNull, JsObject, JsValue}

package object domain {

  /**
    * Handy function to calculate the diffs between two json objects and return a list of messages.
    */
  def jsonDiff(
    name: Option[String],
    j1:   JsValue,
    j2:   JsValue
  ): List[String] = {
    def nam(
      n:  String,
      io: Option[Int]
    ): Some[String] =
      (name, io) match {
        case (None, None)        => Some(n)
        case (None, Some(i))     => Some(s"$n[$i]")
        case (Some(n1), None)    => Some(s"$n1.$n")
        case (Some(n1), Some(i)) => Some(s"$n1.$n[$i]")
      }

    j1 match {
      case _ if j1.getClass != j2.getClass => List(s"Types of ${name.getOrElse("")} differ - $j1 vs. $j2")
      case o: JsObject =>
        o.fields.flatMap {
          case (n, v) => jsonDiff(nam(n, None), v, (j2 \ n).getOrElse(JsNull))
        }.toList

      case JsArray(vs) =>
        val vs2 = j2.as[JsArray].value
        val lengthCheck =
          if (vs.length != vs2.length)
            List(s"${name.getOrElse("")}: Array 1 has length ${vs.length} but array 2 has length ${vs2.length}")
          else List()
        lengthCheck ++
        vs.zipWithIndex.flatMap {
          case (v, i) =>
            vs2.drop(i).headOption match {
              case Some(v2) => jsonDiff(nam(name.getOrElse(""), Some(i)), v, v2)
              case None     => List()
            }
        }

      case _ if j1 == j2 => List()
      case _             => List(s"Values of ${name.getOrElse("")} differ - '$j1' vs. '$j2'")
    }
  }
}
