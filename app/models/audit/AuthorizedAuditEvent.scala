/*
 * Copyright 2024 HM Revenue & Customs
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

package models.audit

import models.{MinimalDetails, PensionSchemeId, SchemeDetails}

trait AuthorizedAuditEvent extends AuditEvent {
  def pensionSchemeId: PensionSchemeId

  def minimalDetails: MinimalDetails

  def schemeDetails: SchemeDetails

  private def schemeAdministratorNameKey: String = pensionSchemeId match {
    case PensionSchemeId.PspId(_) => "schemePractitionerName"
    case PensionSchemeId.PsaId(_) => "schemeAdministratorName"
  }

  private def schemeAdministratorIdKey: String = pensionSchemeId match {
    case PensionSchemeId.PspId(_) => "pensionSchemePractitionerId"
    case PensionSchemeId.PsaId(_) => "pensionSchemeAdministratorId"
  }

  private def credentialRole: String = if (pensionSchemeId.isPSP) "PSP" else "PSA"

  private def affinityGroup: String = if (minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual"

  def additionalDetails: Map[String, String]

  final override def details: Map[String, String] =
    Map(
      "schemeName" -> schemeDetails.schemeName,
      schemeAdministratorNameKey -> schemeDetails.establishers.headOption.map(_.name).getOrElse("empty establisher"),
      schemeAdministratorIdKey -> pensionSchemeId.value,
      "pensionSchemeTaxReference" -> schemeDetails.pstr,
      "affinityGroup" -> affinityGroup,
      "credentialRole(PSA/PSP)" -> credentialRole
    ) ++ additionalDetails
}
