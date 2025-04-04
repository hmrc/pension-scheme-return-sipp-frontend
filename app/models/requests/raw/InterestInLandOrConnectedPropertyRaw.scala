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
import play.api.libs.json.*

object InterestInLandOrConnectedPropertyRaw {
  import LandOrConnectedPropertyRaw.*
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
      /*  G */ acquisitionDate: CsvValue[String],
      /*  H */ isLandOrPropertyInUK: CsvValue[String],
      /*  I */ landOrPropertyUkAddressLine1: CsvValue[Option[String]],
      /*  J */ landOrPropertyUkAddressLine2: CsvValue[Option[String]],
      /*  K */ landOrPropertyUkAddressLine3: CsvValue[Option[String]],
      /*  L */ landOrPropertyUkTownOrCity: CsvValue[Option[String]],
      /*  M */ landOrPropertyUkPostCode: CsvValue[Option[String]],
      /*  N */ landOrPropertyAddressLine1: CsvValue[Option[String]],
      /*  O */ landOrPropertyAddressLine2: CsvValue[Option[String]],
      /*  P */ landOrPropertyAddressLine3: CsvValue[Option[String]],
      /*  Q */ landOrPropertyAddressLine4: CsvValue[Option[String]],
      /*  R */ landOrPropertyCountry: CsvValue[Option[String]],
      /*  S */ isThereLandRegistryReference: CsvValue[String],
      /*  T */ landRegistryRefOrReason: CsvValue[String],
      /*  U */ acquiredFromName: CsvValue[String],
      /*  V */ totalCostOfLandOrPropertyAcquired: CsvValue[String],
      /*  W */ isSupportedByAnIndependentValuation: CsvValue[String],
      /*  X */ isPropertyHeldJointly: CsvValue[String],
      /*  Y */ whatPercentageOfPropertyOwnedByMember: CsvValue[Option[String]],
      /*  Z */ isPropertyDefinedAsSchedule29a: CsvValue[String],
      /* AA */ isLeased: CsvValue[String],
      /* AB */ countOfLessees: CsvValue[Option[String]],
      /* AC */ anyOfLesseesConnected: CsvValue[Option[String]],
      /* AD */ leaseDate: CsvValue[Option[String]],
      /* AE */ annualLeaseAmount: CsvValue[Option[String]],
      /* AF */ totalAmountOfIncomeAndReceipts: CsvValue[String],
      /* AG */ wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
      /* AH */ totalSaleProceedIfAnyDisposal: CsvValue[Option[String]],
      /* AI */ nameOfPurchasers: CsvValue[Option[String]],
      /* AJ */ isAnyPurchaserConnected: CsvValue[Option[String]],
      /* AK */ isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
      /* AL */ hasLandOrPropertyFullyDisposedOf: CsvValue[Option[String]]
    ): RawTransactionDetail = RawTransactionDetail(
      row,
      firstNameOfSchemeMember,
      lastNameOfSchemeMember,
      memberDateOfBirth,
      memberNino,
      memberReasonNoNino,
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
        whatPercentageOfPropertyOwnedByMember
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
          /*  G */ raw.acquisitionDate.value,
          /*  H */ raw.rawAddressDetail.isLandOrPropertyInUK.value,
          /*  I */ raw.rawAddressDetail.landOrPropertyUkAddressLine1.value.getOrElse(""),
          /*  J */ raw.rawAddressDetail.landOrPropertyUkAddressLine2.value.getOrElse(""),
          /*  K */ raw.rawAddressDetail.landOrPropertyUkAddressLine3.value.getOrElse(""),
          /*  L */ raw.rawAddressDetail.landOrPropertyUkTownOrCity.value.getOrElse(""),
          /*  M */ raw.rawAddressDetail.landOrPropertyUkPostCode.value.getOrElse(""),
          /*  N */ raw.rawAddressDetail.landOrPropertyAddressLine1.value.getOrElse(""),
          /*  O */ raw.rawAddressDetail.landOrPropertyAddressLine2.value.getOrElse(""),
          /*  P */ raw.rawAddressDetail.landOrPropertyAddressLine3.value.getOrElse(""),
          /*  Q */ raw.rawAddressDetail.landOrPropertyAddressLine4.value.getOrElse(""),
          /*  R */ raw.rawAddressDetail.landOrPropertyCountry.value.getOrElse(""),
          /*  S */ raw.isThereLandRegistryReference.value,
          /*  T */ raw.landRegistryReferenceOrReason.value,
          /*  U */ raw.acquiredFromName.value,
          /*  V */ raw.totalCostOfLandOrPropertyAcquired.value,
          /*  W */ raw.isSupportedByAnIndependentValuation.value,
          /*  X */ raw.rawJointlyHeld.isPropertyHeldJointly.value,
          /*  Y */ raw.rawJointlyHeld.whatPercentageOfPropertyOwnedByMember.value.getOrElse(""),
          /*  Z */ raw.isPropertyDefinedAsSchedule29a.value,
          /* AA */ raw.rawLeased.isLeased.value,
          /* AB */ raw.rawLeased.countOfLessees.value.getOrElse(""),
          /* AC */ raw.rawLeased.anyLesseeConnectedParty.value.getOrElse(""),
          /* AD */ raw.rawLeased.leaseDate.value.getOrElse(""),
          /* AE */ raw.rawLeased.annualLeaseAmount.value.getOrElse(""),
          /* AF */ raw.totalAmountOfIncomeAndReceipts.value,
          /* AG */ raw.rawDisposal.wereAnyDisposalOnThisDuringTheYear.value,
          /* AH */ raw.rawDisposal.totalSaleProceedIfAnyDisposal.value.getOrElse(""),
          /* AI */ raw.rawDisposal.nameOfPurchasers.value.getOrElse(""),
          /* AJ */ raw.rawDisposal.isAnyPurchaserConnected.value.getOrElse(""),
          /* AK */ raw.rawDisposal.isTransactionSupportedByIndependentValuation.value.getOrElse(""),
          /* AL */ raw.rawDisposal.hasLandOrPropertyFullyDisposedOf.value.getOrElse("")
        )
      // format: on
    }
  }

  implicit val formatRawLeased: OFormat[RawLeased] = Json.format[RawLeased]
  implicit val formatTransactionRawDetails: OFormat[RawTransactionDetail] = Json.format[RawTransactionDetail]
}
