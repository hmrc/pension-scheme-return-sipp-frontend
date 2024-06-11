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

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.Extractors.Int

import java.time.LocalDate

case class SchemeDetails(
  schemeName: String,
  pstr: String,
  schemeStatus: SchemeStatus,
  schemeType: String,
  authorisingPSAID: Option[String],
  establishers: List[Establisher]
)

case class Establisher(
  name: String,
  kind: EstablisherKind
)

sealed trait EstablisherKind extends EnumEntry with Lowercase

object EstablisherKind extends Enum[EstablisherKind] with PlayJsonEnum[EstablisherKind] {
  case object Company extends EstablisherKind
  case object Partnership extends EstablisherKind
  case object Individual extends EstablisherKind

  override def values: IndexedSeq[EstablisherKind] = findValues
}

object Establisher {

  private val companyEstablisherReads: Reads[Establisher] =
    (__ \ "companyDetails" \ "companyName").read[String].map(name => Establisher(name, EstablisherKind.Company))

  private val partnershipEstablisherReads: Reads[Establisher] =
    (__ \ "partnershipDetails" \ "name").read[String].map(name => Establisher(name, EstablisherKind.Partnership))

  private val individualEstablisherReads: Reads[Establisher] = {

    (__ \ "establisherDetails" \ "firstName")
      .read[String]
      .and((__ \ "establisherDetails" \ "middleName").readNullable[String])
      .and((__ \ "establisherDetails" \ "lastName").read[String]) { (first, middle, last) =>
        val name = s"$first ${middle.fold("")(m => s"$m ")}$last"
        Establisher(name, EstablisherKind.Individual)
      }
  }

  implicit val reads: Reads[Establisher] =
    (__ \ "establisherKind").read[EstablisherKind].flatMap {
      case EstablisherKind.Company => companyEstablisherReads
      case EstablisherKind.Partnership => partnershipEstablisherReads
      case EstablisherKind.Individual => individualEstablisherReads
    }
}

object SchemeDetails {

  implicit val reads: Reads[SchemeDetails] =
    (__ \ "schemeName")
      .read[String]
      .and((__ \ "pstr").read[String])
      .and((__ \ "schemeStatus").read[SchemeStatus])
      .and((__ \ "schemeType" \ "name").read[String])
      .and((__ \ "pspDetails" \ "authorisingPSAID").readNullable[String])
      .and(
        (__ \ "establishers")
          .read[JsArray]
          .map[List[Establisher]](
            l =>
              if (l.value.isEmpty) {
                Nil
              } else {
                l.as[List[Establisher]]
              }
          )
      )(SchemeDetails.apply _)
}

sealed abstract class SchemeStatus(override val entryName: String) extends EnumEntry

object SchemeStatus extends Enum[SchemeStatus] with PlayJsonEnum[SchemeStatus] {

  case object Pending extends SchemeStatus("Pending")
  case object PendingInfoRequired extends SchemeStatus("Pending Info Required")
  case object PendingInfoReceived extends SchemeStatus("Pending Info Received")
  case object Rejected extends SchemeStatus("Rejected")
  case object Open extends SchemeStatus("Open")
  case object Deregistered extends SchemeStatus("Deregistered")
  case object WoundUp extends SchemeStatus("Wound-up")
  case object RejectedUnderAppeal extends SchemeStatus("Rejected Under Appeal")

  override def values: IndexedSeq[SchemeStatus] = findValues
}

case class ListMinimalSchemeDetails(schemeDetails: List[MinimalSchemeDetails])

object ListMinimalSchemeDetails {

  implicit val reads: Reads[ListMinimalSchemeDetails] = Json.reads[ListMinimalSchemeDetails]
}

case class MinimalSchemeDetails(
  name: String,
  srn: String,
  schemeStatus: SchemeStatus,
  openDate: Option[LocalDate],
  windUpDate: Option[LocalDate]
)

object MinimalSchemeDetails {

  private val dateRegex = "(\\d{4})-(\\d{1,2})-(\\d{1,2})".r
  private implicit val readLocalDate: Reads[LocalDate] = Reads[LocalDate] {
    case JsString(dateRegex(Int(year), Int(month), Int(day))) =>
      JsSuccess(LocalDate.of(year, month, day))
    case err => JsError(s"Unable to read local date from $err")
  }

  implicit val reads: Reads[MinimalSchemeDetails] =
    (__ \ "name")
      .read[String]
      .and((__ \ "referenceNumber").read[String])
      .and((__ \ "schemeStatus").read[SchemeStatus])
      .and((__ \ "openDate").readNullable[LocalDate])
      .and((__ \ "windUpDate").readNullable[LocalDate])(MinimalSchemeDetails.apply _)
}
