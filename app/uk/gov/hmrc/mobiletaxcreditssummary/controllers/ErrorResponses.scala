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

package uk.gov.hmrc.mobiletaxcreditssummary.controllers

import uk.gov.hmrc.api.controllers.ErrorResponse

case object ErrorUnauthorizedNoNino extends ErrorResponse(401, "UNAUTHORIZED", "NINO does not exist on account")

case object ClientRetryRequest extends ErrorResponse(429, "NTC_RETRY", "Client must retry the request.")

case object ErrorBadRequest extends ErrorResponse(400, "BAD_REQUEST", "Invalid request sent")

case object ErrorUnauthorizedMicroService extends ErrorResponse(401, "UNAUTHORIZED", "Unauthorized to access resource")

case object ErrorForbidden extends ErrorResponse(403, "FORBIDDEN", "Access Denied")

class FailToMatchTaxIdOnAuth extends Exception

class NinoNotFoundOnAccount extends Exception

class AccountWithLowCL extends Exception
