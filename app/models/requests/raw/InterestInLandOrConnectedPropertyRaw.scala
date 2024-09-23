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

package models.requests.raw

import cats.data.NonEmptyList
import models.CsvValue
import play.api.libs.json._

object InterestInLandOrConnectedPropertyRaw {
  import LandOrConnectedPropertyRaw._
  case class RawLeased(
    isLeased: CsvValue[String],
    countOfLessees: CsvValue[Option[String]],
    anyLesseeConnectedParty: CsvValue[Option[String]],
    leaseDate: CsvValue[Option[String]],
    annualLeaseAmount: CsvValue[Option[String]]
  )

  case class RawTransactionDetail(
    row: Int,
    firstNameOfSchemeMember: CsvValue[String],
    lastNameOfSchemeMember: CsvValue[String],
    memberDateOfBirth: CsvValue[String],
    memberNino: CsvValue[Option[String]],
    memberReasonNoNino: CsvValue[Option[String]],
    countOfLandOrPropertyTransactions: CsvValue[String],
    acquisitionDate: CsvValue[String],
    rawAddressDetail: RawAddressDetail,
    isThereLandRegistryReference: CsvValue[String],
    landRegistryReferenceOrReason: CsvValue[String],
    acquiredFromName: CsvValue[String],
    totalCostOfLandOrPropertyAcquired: CsvValue[String],
    isSupportedByAnIndependentValuation: CsvValue[String],
    rawJointlyHeld: RawJointlyHeld,
    isPropertyDefinedAsSchedule29a: CsvValue[String],
    rawLeased: RawLeased,
    totalAmountOfIncomeAndReceipts: CsvValue[String],
    rawDisposal: RawDisposal
  )

  object RawTransactionDetail {
    def create(
      row: Int,
      /*  B */ firstNameOfSchemeMember: CsvValue[String],
      /*  C */ lastNameOfSchemeMember: CsvValue[String],
      /*  D */ memberDateOfBirth: CsvValue[String],
      /*  E */ memberNino: CsvValue[Option[String]],
      /*  F */ memberReasonNoNino: CsvValue[Option[String]],
      /*  G */ countOfLandOrPropertyTransactions: CsvValue[String],
      /*  H */ acquisitionDate: CsvValue[String],
      /*  I */ isLandOrPropertyInUK: CsvValue[String],
      /*  J */ landOrPropertyUkAddressLine1: CsvValue[Option[String]],
      /*  K */ landOrPropertyUkAddressLine2: CsvValue[Option[String]],
      /*  L */ landOrPropertyUkAddressLine3: CsvValue[Option[String]],
      /*  M */ landOrPropertyUkTownOrCity: CsvValue[Option[String]],
      /*  N */ landOrPropertyUkPostCode: CsvValue[Option[String]],
      /*  O */ landOrPropertyAddressLine1: CsvValue[Option[String]],
      /*  P */ landOrPropertyAddressLine2: CsvValue[Option[String]],
      /*  Q */ landOrPropertyAddressLine3: CsvValue[Option[String]],
      /*  R */ landOrPropertyAddressLine4: CsvValue[Option[String]],
      /*  S */ landOrPropertyCountry: CsvValue[Option[String]],
      /*  T */ isThereLandRegistryReference: CsvValue[String],
      /*  U */ landRegistryRefOrReason: CsvValue[String],
      /*  V */ acquiredFromName: CsvValue[String],
      /*  W */ totalCostOfLandOrPropertyAcquired: CsvValue[String],
      /*  X */ isSupportedByAnIndependentValuation: CsvValue[String],
      /*  Y */ isPropertyHeldJointly: CsvValue[String],
      /*  Z */ howManyPersonsJointlyOwnProperty: CsvValue[Option[String]],
      /* AA */ isPropertyDefinedAsSchedule29a: CsvValue[String],
      /* AB */ isLeased: CsvValue[String],
      /* AC */ countOfLessees: CsvValue[Option[String]],
      /* AD */ anyOfLesseesConnected: CsvValue[Option[String]],
      /* AE */ leaseDate: CsvValue[Option[String]],
      /* AF */ annualLeaseAmount: CsvValue[Option[String]],
      /* AG */ totalAmountOfIncomeAndReceipts: CsvValue[String],
      /* AH */ wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
      /* AI */ totalSaleProceedIfAnyDisposal: CsvValue[Option[String]],
      /* AJ */ nameOfPurchasers: CsvValue[Option[String]],
      /* AK */ isAnyPurchaserConnected: CsvValue[Option[String]],
      /* AL */ isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
      /* AM */ hasLandOrPropertyFullyDisposedOf: CsvValue[Option[String]]
    ): RawTransactionDetail = RawTransactionDetail(
      row,
      firstNameOfSchemeMember,
      lastNameOfSchemeMember,
      memberDateOfBirth,
      memberNino,
      memberReasonNoNino,
      countOfLandOrPropertyTransactions,
      acquisitionDate,
      RawAddressDetail(
        isLandOrPropertyInUK,
        landOrPropertyUkAddressLine1,
        landOrPropertyUkAddressLine2,
        landOrPropertyUkAddressLine3,
        landOrPropertyUkTownOrCity,
        landOrPropertyUkPostCode,
        landOrPropertyAddressLine1,
        landOrPropertyAddressLine2,
        landOrPropertyAddressLine3,
        landOrPropertyAddressLine4,
        landOrPropertyCountry
      ),
      isThereLandRegistryReference,
      landRegistryRefOrReason,
      acquiredFromName,
      totalCostOfLandOrPropertyAcquired,
      isSupportedByAnIndependentValuation,
      RawJointlyHeld(
        isPropertyHeldJointly,
        howManyPersonsJointlyOwnProperty
      ),
      isPropertyDefinedAsSchedule29a,
      RawLeased(
        isLeased,
        countOfLessees,
        anyOfLesseesConnected,
        leaseDate,
        annualLeaseAmount
      ),
      totalAmountOfIncomeAndReceipts,
      RawDisposal(
        wereAnyDisposalOnThisDuringTheYear,
        totalSaleProceedIfAnyDisposal,
        nameOfPurchasers,
        isAnyPurchaserConnected,
        isTransactionSupportedByIndependentValuation,
        hasLandOrPropertyFullyDisposedOf
      )
    )

    implicit class Ops(val raw: RawTransactionDetail) extends AnyVal {
      // format: off
      def toNonEmptyList: NonEmptyList[String] =
        NonEmptyList.of(
          /*  B */ raw.firstNameOfSchemeMember.value,
          /*  C */ raw.lastNameOfSchemeMember.value,
          /*  D */ raw.memberDateOfBirth.value,
          /*  E */ raw.memberNino.value.getOrElse(""),
          /*  F */ raw.memberReasonNoNino.value.getOrElse(""),
          /*  G */ raw.countOfLandOrPropertyTransactions.value,
          /*  H */ raw.acquisitionDate.value,
          /*  I */ raw.rawAddressDetail.isLandOrPropertyInUK.value,
          /*  J */ raw.rawAddressDetail.landOrPropertyUkAddressLine1.value.getOrElse(""),
          /*  K */ raw.rawAddressDetail.landOrPropertyUkAddressLine2.value.getOrElse(""),
          /*  L */ raw.rawAddressDetail.landOrPropertyUkAddressLine3.value.getOrElse(""),
          /*  M */ raw.rawAddressDetail.landOrPropertyUkTownOrCity.value.getOrElse(""),
          /*  N */ raw.rawAddressDetail.landOrPropertyUkPostCode.value.getOrElse(""),
          /*  O */ raw.rawAddressDetail.landOrPropertyAddressLine1.value.getOrElse(""),
          /*  P */ raw.rawAddressDetail.landOrPropertyAddressLine2.value.getOrElse(""),
          /*  Q */ raw.rawAddressDetail.landOrPropertyAddressLine3.value.getOrElse(""),
          /*  R */ raw.rawAddressDetail.landOrPropertyAddressLine4.value.getOrElse(""),
          /*  S */ raw.rawAddressDetail.landOrPropertyCountry.value.getOrElse(""),
          /*  T */ raw.isThereLandRegistryReference.value,
          /*  U */ raw.landRegistryReferenceOrReason.value,
          /*  V */ raw.acquiredFromName.value,
          /*  W */ raw.totalCostOfLandOrPropertyAcquired.value,
          /*  X */ raw.isSupportedByAnIndependentValuation.value,
          /*  Y */ raw.rawJointlyHeld.isPropertyHeldJointly.value,
          /*  Z */ raw.rawJointlyHeld.howManyPersonsJointlyOwnProperty.value.getOrElse(""),
          /* AA */ raw.isPropertyDefinedAsSchedule29a.value,
          /* AB */ raw.rawLeased.isLeased.value,
          /* AC */ raw.rawLeased.countOfLessees.value.getOrElse(""),
          /* AD */ raw.rawLeased.anyLesseeConnectedParty.value.getOrElse(""),
          /* AE */ raw.rawLeased.leaseDate.value.getOrElse(""),
          /* AF */ raw.rawLeased.annualLeaseAmount.value.getOrElse(""),
          /* AG */ raw.totalAmountOfIncomeAndReceipts.value,
          /* AH */ raw.rawDisposal.wereAnyDisposalOnThisDuringTheYear.value,
          /* AI */ raw.rawDisposal.totalSaleProceedIfAnyDisposal.value.getOrElse(""),
          /* AJ */ raw.rawDisposal.nameOfPurchasers.value.getOrElse(""),
          /* AK */ raw.rawDisposal.isAnyPurchaserConnected.value.getOrElse(""),
          /* AL */ raw.rawDisposal.isTransactionSupportedByIndependentValuation.value.getOrElse(""),
          /* AM */ raw.rawDisposal.hasLandOrPropertyFullyDisposedOf.value.getOrElse("")
        )
      // format: on
    }
  }

  implicit val formatRawLeased: OFormat[RawLeased] = Json.format[RawLeased]
  implicit val formatTransactionRawDetails: OFormat[RawTransactionDetail] = Json.format[RawTransactionDetail]
}
