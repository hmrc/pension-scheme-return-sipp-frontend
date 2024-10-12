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

package models

import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.*

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.chaining.scalaUtilChainingOps

trait ModelSerializers {

  implicit lazy val writesIndividualDetails: Writes[IndividualDetails] = Json.writes[IndividualDetails]
  implicit lazy val writesMinimalDetails: Writes[MinimalDetails] = Json.writes[MinimalDetails]

  implicit lazy val writesEstablisher: Writes[Establisher] = establisher =>
    (establisher.kind match {
      case EstablisherKind.Company =>
        Json.obj("companyDetails" -> Json.obj("companyName" -> establisher.name))
      case EstablisherKind.Partnership =>
        Json.obj("partnershipDetails" -> Json.obj("name" -> establisher.name))
      case EstablisherKind.Individual =>
        val fullName = establisher.name.split(" ")
        val first = fullName.head
        val last = fullName.tail.last
        val middle = fullName.tail.init.reduceOption((a, b) => s"$a $b")
        Json.obj(
          "establisherDetails" -> Json
            .obj(
              "firstName" -> first,
              "lastName" -> last,
              "middleName" -> middle.mkString
            )
            .pipe(nameJson => middle.fold(nameJson)(m => nameJson + ("middleName" -> JsString(m))))
        )
    }) ++ Json.obj("establisherKind" -> establisher.kind.entryName)

  implicit lazy val writeSchemeDetails: Writes[SchemeDetails] = { details =>
    val authorisingPSAID: JsObject = details.authorisingPSAID.fold(Json.obj())(psaId =>
      Json.obj("pspDetails" -> Json.obj("authorisingPSAID" -> psaId))
    )

    Json.obj(
      "schemeName" -> details.schemeName,
      "pstr" -> details.pstr,
      "schemeStatus" -> details.schemeStatus,
      "schemeType" -> Json.obj("name" -> details.schemeType),
      "establishers" -> details.establishers
    ) ++ authorisingPSAID
  }

  implicit lazy val writeMinimalSchemeDetails: Writes[MinimalSchemeDetails] = { details =>
    def formatDate(date: LocalDate): String =
      date.format(DateTimeFormatter.ofPattern("yyyy-M-d"))

    val fields =
      List[Option[(String, JsValueWrapper)]](
        Some("name" -> details.name),
        Some("referenceNumber" -> details.srn),
        Some("schemeStatus" -> details.schemeStatus),
        details.openDate.map(d => "openDate" -> formatDate(d)),
        details.windUpDate.map(d => "windUpDate" -> formatDate(d))
      ).flatten

    Json.obj(fields*)
  }

  implicit lazy val writeListMinimalSchemeDetails: Writes[ListMinimalSchemeDetails] =
    Json.writes[ListMinimalSchemeDetails]
}
