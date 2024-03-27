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
import models.requests.raw.TangibleMoveablePropertyRaw.RawTransactionDetail
import play.api.libs.json._

object LandOrConnectedPropertyRaw {

  case class RawAddressDetail(
    isLandOrPropertyInUK: CsvValue[String],
    landOrPropertyUkAddressLine1: CsvValue[Option[String]],
    landOrPropertyUkAddressLine2: CsvValue[Option[String]],
    landOrPropertyUkAddressLine3: CsvValue[Option[String]],
    landOrPropertyUkTownOrCity: CsvValue[Option[String]],
    landOrPropertyUkPostCode: CsvValue[Option[String]],
    landOrPropertyAddressLine1: CsvValue[Option[String]],
    landOrPropertyAddressLine2: CsvValue[Option[String]],
    landOrPropertyAddressLine3: CsvValue[Option[String]],
    landOrPropertyAddressLine4: CsvValue[Option[String]],
    landOrPropertyCountry: CsvValue[Option[String]]
  )

  case class RawAcquiredFromType(
    acquiredFromType: CsvValue[String],
    acquirerNinoForIndividual: CsvValue[Option[String]],
    acquirerCrnForCompany: CsvValue[Option[String]],
    acquirerUtrForPartnership: CsvValue[Option[String]],
    noIdOrAcquiredFromAnotherSource: CsvValue[Option[String]]
  )

  case class RawJointlyHeld(
    isPropertyHeldJointly: CsvValue[String],
    howManyPersonsJointlyOwnProperty: CsvValue[Option[String]],
    firstPersonNameJointlyOwning: CsvValue[Option[String]],
    firstPersonNinoJointlyOwning: CsvValue[Option[String]],
    firstPersonNoNinoJointlyOwning: CsvValue[Option[String]],
    secondPersonNameJointlyOwning: CsvValue[Option[String]],
    secondPersonNinoJointlyOwning: CsvValue[Option[String]],
    secondPersonNoNinoJointlyOwning: CsvValue[Option[String]],
    thirdPersonNameJointlyOwning: CsvValue[Option[String]],
    thirdPersonNinoJointlyOwning: CsvValue[Option[String]],
    thirdPersonNoNinoJointlyOwning: CsvValue[Option[String]],
    fourthPersonNameJointlyOwning: CsvValue[Option[String]],
    fourthPersonNinoJointlyOwning: CsvValue[Option[String]],
    fourthPersonNoNinoJointlyOwning: CsvValue[Option[String]],
    fifthPersonNameJointlyOwning: CsvValue[Option[String]],
    fifthPersonNinoJointlyOwning: CsvValue[Option[String]],
    fifthPersonNoNinoJointlyOwning: CsvValue[Option[String]]
  )

  case class RawLessee(
    name: CsvValue[Option[String]],
    connection: CsvValue[Option[String]],
    grantedDate: CsvValue[Option[String]],
    annualAmount: CsvValue[Option[String]]
  )

  case class RawLeased(
    isLeased: CsvValue[String],
    first: RawLessee,
    second: RawLessee,
    third: RawLessee,
    fourth: RawLessee,
    fifth: RawLessee,
    sixth: RawLessee,
    seventh: RawLessee,
    eighth: RawLessee,
    ninth: RawLessee,
    tenth: RawLessee
  )

  case class RawPurchaser(
    name: CsvValue[Option[String]],
    connection: CsvValue[Option[String]]
  )

  case class RawDisposal(
    wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
    totalSaleProceedIfAnyDisposal: CsvValue[Option[String]],
    first: RawPurchaser,
    second: RawPurchaser,
    third: RawPurchaser,
    fourth: RawPurchaser,
    fifth: RawPurchaser,
    sixth: RawPurchaser,
    seventh: RawPurchaser,
    eighth: RawPurchaser,
    ninth: RawPurchaser,
    tenth: RawPurchaser,
    isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
    hasLandOrPropertyFullyDisposedOf: CsvValue[Option[String]]
  )

  case class RawTransactionDetail(
    row: Int,
    firstNameOfSchemeMember: CsvValue[String],
    lastNameOfSchemeMember: CsvValue[String],
    memberDateOfBirth: CsvValue[String],
    countOfLandOrPropertyTransactions: CsvValue[String],
    acquisitionDate: CsvValue[String],
    rawAddressDetail: RawAddressDetail,
    isThereLandRegistryReference: CsvValue[String],
    noLandRegistryReference: CsvValue[Option[String]],
    acquiredFromName: CsvValue[String],
    rawAcquiredFromType: RawAcquiredFromType,
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
      /*  E */ countOfLandOrPropertyTransactions: CsvValue[String],
      /*  F */ acquisitionDate: CsvValue[String],
      /*  G */ isLandOrPropertyInUK: CsvValue[String],
      /*  H */ landOrPropertyUkAddressLine1: CsvValue[Option[String]],
      /*  I */ landOrPropertyUkAddressLine2: CsvValue[Option[String]],
      /*  J */ landOrPropertyUkAddressLine3: CsvValue[Option[String]],
      /*  K */ landOrPropertyUkTownOrCity: CsvValue[Option[String]],
      /*  L */ landOrPropertyUkPostCode: CsvValue[Option[String]],
      /*  M */ landOrPropertyAddressLine1: CsvValue[Option[String]],
      /*  N */ landOrPropertyAddressLine2: CsvValue[Option[String]],
      /*  O */ landOrPropertyAddressLine3: CsvValue[Option[String]],
      /*  P */ landOrPropertyAddressLine4: CsvValue[Option[String]],
      /*  Q */ landOrPropertyCountry: CsvValue[Option[String]],
      /*  R */ isThereLandRegistryReference: CsvValue[String],
      /*  S */ noLandRegistryReference: CsvValue[Option[String]],
      /*  T */ acquiredFromName: CsvValue[String],
      /*  U */ acquiredFromType: CsvValue[String],
      /*  V */ acquirerNinoForIndividual: CsvValue[Option[String]],
      /*  W */ acquirerCrnForCompany: CsvValue[Option[String]],
      /*  X */ acquirerUtrForPartnership: CsvValue[Option[String]],
      /*  Y */ noIdOrAcquiredFromAnotherSource: CsvValue[Option[String]],
      /*  Z */ totalCostOfLandOrPropertyAcquired: CsvValue[String],
      /* AA */ isSupportedByAnIndependentValuation: CsvValue[String],
      /* AB */ isPropertyHeldJointly: CsvValue[String],
      /* AC */ howManyPersonsJointlyOwnProperty: CsvValue[Option[String]],
      /* AD */ firstPersonNameJointlyOwning: CsvValue[Option[String]],
      /* AE */ firstPersonNinoJointlyOwning: CsvValue[Option[String]],
      /* AF */ firstPersonNoNinoJointlyOwning: CsvValue[Option[String]],
      /* AG */ secondPersonNameJointlyOwning: CsvValue[Option[String]],
      /* AH */ secondPersonNinoJointlyOwning: CsvValue[Option[String]],
      /* AI */ secondPersonNoNinoJointlyOwning: CsvValue[Option[String]],
      /* AJ */ thirdPersonNameJointlyOwning: CsvValue[Option[String]],
      /* AK */ thirdPersonNinoJointlyOwning: CsvValue[Option[String]],
      /* AL */ thirdPersonNoNinoJointlyOwning: CsvValue[Option[String]],
      /* AM */ fourthPersonNameJointlyOwning: CsvValue[Option[String]],
      /* AN */ fourthPersonNinoJointlyOwning: CsvValue[Option[String]],
      /* AO */ fourthPersonNoNinoJointlyOwning: CsvValue[Option[String]],
      /* AP */ fifthPersonNameJointlyOwning: CsvValue[Option[String]],
      /* AQ */ fifthPersonNinoJointlyOwning: CsvValue[Option[String]],
      /* AR */ fifthPersonNoNinoJointlyOwning: CsvValue[Option[String]],
      /* AS */ isPropertyDefinedAsSchedule29a: CsvValue[String],
      /* AT */ isLeased: CsvValue[String],
      /* AU */ firstLesseeName: CsvValue[Option[String]],
      /* AV */ firstLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* AW */ firstLesseeGrantedDate: CsvValue[Option[String]],
      /* AX */ firstLesseeAnnualAmount: CsvValue[Option[String]],
      /* AY */ secondLesseeName: CsvValue[Option[String]],
      /* AZ */ secondLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* BA */ secondLesseeGrantedDate: CsvValue[Option[String]],
      /* BB */ secondLesseeAnnualAmount: CsvValue[Option[String]],
      /* BC */ thirdLesseeName: CsvValue[Option[String]],
      /* BD */ thirdLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* BE */ thirdLesseeGrantedDate: CsvValue[Option[String]],
      /* BF */ thirdLesseeAnnualAmount: CsvValue[Option[String]],
      /* BG */ fourthLesseeName: CsvValue[Option[String]],
      /* BH */ fourthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* BI */ fourthLesseeGrantedDate: CsvValue[Option[String]],
      /* BJ */ fourthLesseeAnnualAmount: CsvValue[Option[String]],
      /* BK */ fifthLesseeName: CsvValue[Option[String]],
      /* BL */ fifthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* BM */ fifthLesseeGrantedDate: CsvValue[Option[String]],
      /* BN */ fifthLesseeAnnualAmount: CsvValue[Option[String]],
      /* BO */ sixthLesseeName: CsvValue[Option[String]],
      /* BP */ sixthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* BQ */ sixthLesseeGrantedDate: CsvValue[Option[String]],
      /* BR */ sixthLesseeAnnualAmount: CsvValue[Option[String]],
      /* BS */ seventhLesseeName: CsvValue[Option[String]],
      /* BT */ seventhLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* BU */ seventhLesseeGrantedDate: CsvValue[Option[String]],
      /* BV */ seventhLesseeAnnualAmount: CsvValue[Option[String]],
      /* BW */ eighthLesseeName: CsvValue[Option[String]],
      /* BX */ eighthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* BY */ eighthLesseeGrantedDate: CsvValue[Option[String]],
      /* BZ */ eighthLesseeAnnualAmount: CsvValue[Option[String]],
      /* CA */ ninthLesseeName: CsvValue[Option[String]],
      /* CB */ ninthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* CC */ ninthLesseeGrantedDate: CsvValue[Option[String]],
      /* CD */ ninthLesseeAnnualAmount: CsvValue[Option[String]],
      /* CE */ tenthLesseeName: CsvValue[Option[String]],
      /* CF */ tenthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* CG */ tenthLesseeGrantedDate: CsvValue[Option[String]],
      /* CH */ tenthLesseeAnnualAmount: CsvValue[Option[String]],
      /* CI */ totalAmountOfIncomeAndReceipts: CsvValue[String],
      /* CJ */ wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
      /* CK */ totalSaleProceedIfAnyDisposal: CsvValue[Option[String]],
      /* CL */ firstPurchaserName: CsvValue[Option[String]],
      /* CM */ firstPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* CN */ secondPurchaserName: CsvValue[Option[String]],
      /* CO */ secondPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* CP */ thirdPurchaserName: CsvValue[Option[String]],
      /* CQ */ thirdPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* CR */ fourthPurchaserName: CsvValue[Option[String]],
      /* CS */ fourthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* CT */ fifthPurchaserName: CsvValue[Option[String]],
      /* CU */ fifthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* CV */ sixthPurchaserName: CsvValue[Option[String]],
      /* CW */ sixthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* CX */ seventhPurchaserName: CsvValue[Option[String]],
      /* CY */ seventhPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* CZ */ eighthPurchaserName: CsvValue[Option[String]],
      /* DA */ eighthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* DB */ ninthPurchaserName: CsvValue[Option[String]],
      /* DC */ ninthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* DD */ tenthPurchaserName: CsvValue[Option[String]],
      /* DE */ tenthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* DF */ isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
      /* DG */ hasLandOrPropertyFullyDisposedOf: CsvValue[Option[String]]
    ): RawTransactionDetail = RawTransactionDetail(
      row,
      firstNameOfSchemeMember,
      lastNameOfSchemeMember,
      memberDateOfBirth,
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
      noLandRegistryReference,
      acquiredFromName,
      RawAcquiredFromType(
        acquiredFromType,
        acquirerNinoForIndividual,
        acquirerCrnForCompany,
        acquirerUtrForPartnership,
        noIdOrAcquiredFromAnotherSource
      ),
      totalCostOfLandOrPropertyAcquired,
      isSupportedByAnIndependentValuation,
      RawJointlyHeld(
        isPropertyHeldJointly,
        howManyPersonsJointlyOwnProperty,
        firstPersonNameJointlyOwning,
        firstPersonNinoJointlyOwning,
        firstPersonNoNinoJointlyOwning,
        secondPersonNameJointlyOwning,
        secondPersonNinoJointlyOwning,
        secondPersonNoNinoJointlyOwning,
        thirdPersonNameJointlyOwning,
        thirdPersonNinoJointlyOwning,
        thirdPersonNoNinoJointlyOwning,
        fourthPersonNameJointlyOwning,
        fourthPersonNinoJointlyOwning,
        fourthPersonNoNinoJointlyOwning,
        fifthPersonNameJointlyOwning,
        fifthPersonNinoJointlyOwning,
        fifthPersonNoNinoJointlyOwning
      ),
      isPropertyDefinedAsSchedule29a,
      RawLeased(
        isLeased,
        RawLessee(firstLesseeName, firstLesseeConnectedOrUnconnected, firstLesseeGrantedDate, firstLesseeAnnualAmount),
        RawLessee(
          secondLesseeName,
          secondLesseeConnectedOrUnconnected,
          secondLesseeGrantedDate,
          secondLesseeAnnualAmount
        ),
        RawLessee(thirdLesseeName, thirdLesseeConnectedOrUnconnected, thirdLesseeGrantedDate, thirdLesseeAnnualAmount),
        RawLessee(
          fourthLesseeName,
          fourthLesseeConnectedOrUnconnected,
          fourthLesseeGrantedDate,
          fourthLesseeAnnualAmount
        ),
        RawLessee(fifthLesseeName, fifthLesseeConnectedOrUnconnected, fifthLesseeGrantedDate, fifthLesseeAnnualAmount),
        RawLessee(sixthLesseeName, sixthLesseeConnectedOrUnconnected, sixthLesseeGrantedDate, sixthLesseeAnnualAmount),
        RawLessee(
          seventhLesseeName,
          seventhLesseeConnectedOrUnconnected,
          seventhLesseeGrantedDate,
          seventhLesseeAnnualAmount
        ),
        RawLessee(
          eighthLesseeName,
          eighthLesseeConnectedOrUnconnected,
          eighthLesseeGrantedDate,
          eighthLesseeAnnualAmount
        ),
        RawLessee(ninthLesseeName, ninthLesseeConnectedOrUnconnected, ninthLesseeGrantedDate, ninthLesseeAnnualAmount),
        RawLessee(tenthLesseeName, tenthLesseeConnectedOrUnconnected, tenthLesseeGrantedDate, tenthLesseeAnnualAmount)
      ),
      totalAmountOfIncomeAndReceipts,
      rawDisposal = RawDisposal(
        wereAnyDisposalOnThisDuringTheYear,
        totalSaleProceedIfAnyDisposal,
        RawPurchaser(firstPurchaserName, firstPurchaserConnectedOrUnconnected),
        RawPurchaser(secondPurchaserName, secondPurchaserConnectedOrUnconnected),
        RawPurchaser(thirdPurchaserName, thirdPurchaserConnectedOrUnconnected),
        RawPurchaser(fourthPurchaserName, fourthPurchaserConnectedOrUnconnected),
        RawPurchaser(fifthPurchaserName, fifthPurchaserConnectedOrUnconnected),
        RawPurchaser(sixthPurchaserName, sixthPurchaserConnectedOrUnconnected),
        RawPurchaser(seventhPurchaserName, seventhPurchaserConnectedOrUnconnected),
        RawPurchaser(eighthPurchaserName, eighthPurchaserConnectedOrUnconnected),
        RawPurchaser(ninthPurchaserName, ninthPurchaserConnectedOrUnconnected),
        RawPurchaser(tenthPurchaserName, tenthPurchaserConnectedOrUnconnected),
        isTransactionSupportedByIndependentValuation,
        hasLandOrPropertyFullyDisposedOf
      )
    )

    implicit class Ops(val raw: RawTransactionDetail) extends AnyVal {
      def toNonEmptyList: NonEmptyList[String] =
        NonEmptyList.of(
          "",
          raw.firstNameOfSchemeMember.value,
          raw.lastNameOfSchemeMember.value,
          raw.memberDateOfBirth.value,
          raw.countOfLandOrPropertyTransactions.value,
          raw.acquisitionDate.value,
          raw.rawAddressDetail.isLandOrPropertyInUK.value,
          raw.rawAddressDetail.landOrPropertyUkAddressLine1.value.getOrElse(""),
          raw.rawAddressDetail.landOrPropertyUkAddressLine2.value.getOrElse(""),
          raw.rawAddressDetail.landOrPropertyUkAddressLine3.value.getOrElse(""),
          raw.rawAddressDetail.landOrPropertyUkTownOrCity.value.getOrElse(""),
          raw.rawAddressDetail.landOrPropertyUkPostCode.value.getOrElse(""),
          raw.rawAddressDetail.landOrPropertyAddressLine1.value.getOrElse(""),
          raw.rawAddressDetail.landOrPropertyAddressLine2.value.getOrElse(""),
          raw.rawAddressDetail.landOrPropertyAddressLine3.value.getOrElse(""),
          raw.rawAddressDetail.landOrPropertyAddressLine4.value.getOrElse(""),
          raw.rawAddressDetail.landOrPropertyCountry.value.getOrElse(""),
          raw.isThereLandRegistryReference.value,
          raw.noLandRegistryReference.value.getOrElse(""),
          raw.acquiredFromName.value,
          raw.rawAcquiredFromType.acquiredFromType.value,
          raw.rawAcquiredFromType.acquirerNinoForIndividual.value.getOrElse(""),
          raw.rawAcquiredFromType.acquirerCrnForCompany.value.getOrElse(""),
          raw.rawAcquiredFromType.acquirerUtrForPartnership.value.getOrElse(""),
          raw.rawAcquiredFromType.noIdOrAcquiredFromAnotherSource.value.getOrElse(""),
          raw.totalCostOfLandOrPropertyAcquired.value,
          raw.isSupportedByAnIndependentValuation.value,
          raw.rawJointlyHeld.isPropertyHeldJointly.value,
          raw.rawJointlyHeld.howManyPersonsJointlyOwnProperty.value.getOrElse(""),
          raw.rawJointlyHeld.firstPersonNameJointlyOwning.value.getOrElse(""),
          raw.rawJointlyHeld.firstPersonNinoJointlyOwning.value.getOrElse(""),
          raw.rawJointlyHeld.firstPersonNoNinoJointlyOwning.value.getOrElse(""),
          raw.rawJointlyHeld.secondPersonNameJointlyOwning.value.getOrElse(""),
          raw.rawJointlyHeld.secondPersonNinoJointlyOwning.value.getOrElse(""),
          raw.rawJointlyHeld.secondPersonNoNinoJointlyOwning.value.getOrElse(""),
          raw.rawJointlyHeld.thirdPersonNameJointlyOwning.value.getOrElse(""),
          raw.rawJointlyHeld.thirdPersonNinoJointlyOwning.value.getOrElse(""),
          raw.rawJointlyHeld.thirdPersonNoNinoJointlyOwning.value.getOrElse(""),
          raw.rawJointlyHeld.fourthPersonNameJointlyOwning.value.getOrElse(""),
          raw.rawJointlyHeld.fourthPersonNinoJointlyOwning.value.getOrElse(""),
          raw.rawJointlyHeld.fourthPersonNoNinoJointlyOwning.value.getOrElse(""),
          raw.rawJointlyHeld.fifthPersonNameJointlyOwning.value.getOrElse(""),
          raw.rawJointlyHeld.fifthPersonNinoJointlyOwning.value.getOrElse(""),
          raw.rawJointlyHeld.fifthPersonNoNinoJointlyOwning.value.getOrElse(""),
          raw.isPropertyDefinedAsSchedule29a.value,
          raw.rawLeased.isLeased.value,
          raw.rawLeased.first.name.value.getOrElse(""),
          raw.rawLeased.first.connection.value.getOrElse(""),
          raw.rawLeased.first.grantedDate.value.getOrElse(""),
          raw.rawLeased.first.annualAmount.value.getOrElse(""),
          raw.rawLeased.second.name.value.getOrElse(""),
          raw.rawLeased.second.connection.value.getOrElse(""),
          raw.rawLeased.second.grantedDate.value.getOrElse(""),
          raw.rawLeased.second.annualAmount.value.getOrElse(""),
          raw.rawLeased.third.name.value.getOrElse(""),
          raw.rawLeased.third.connection.value.getOrElse(""),
          raw.rawLeased.third.grantedDate.value.getOrElse(""),
          raw.rawLeased.third.annualAmount.value.getOrElse(""),
          raw.rawLeased.fourth.name.value.getOrElse(""),
          raw.rawLeased.fourth.connection.value.getOrElse(""),
          raw.rawLeased.fourth.grantedDate.value.getOrElse(""),
          raw.rawLeased.fourth.annualAmount.value.getOrElse(""),
          raw.rawLeased.fifth.name.value.getOrElse(""),
          raw.rawLeased.fifth.connection.value.getOrElse(""),
          raw.rawLeased.fifth.grantedDate.value.getOrElse(""),
          raw.rawLeased.fifth.annualAmount.value.getOrElse(""),
          raw.rawLeased.sixth.name.value.getOrElse(""),
          raw.rawLeased.sixth.connection.value.getOrElse(""),
          raw.rawLeased.sixth.grantedDate.value.getOrElse(""),
          raw.rawLeased.sixth.annualAmount.value.getOrElse(""),
          raw.rawLeased.seventh.name.value.getOrElse(""),
          raw.rawLeased.seventh.connection.value.getOrElse(""),
          raw.rawLeased.seventh.grantedDate.value.getOrElse(""),
          raw.rawLeased.seventh.annualAmount.value.getOrElse(""),
          raw.rawLeased.eighth.name.value.getOrElse(""),
          raw.rawLeased.eighth.connection.value.getOrElse(""),
          raw.rawLeased.eighth.grantedDate.value.getOrElse(""),
          raw.rawLeased.eighth.annualAmount.value.getOrElse(""),
          raw.rawLeased.ninth.name.value.getOrElse(""),
          raw.rawLeased.ninth.connection.value.getOrElse(""),
          raw.rawLeased.ninth.grantedDate.value.getOrElse(""),
          raw.rawLeased.ninth.annualAmount.value.getOrElse(""),
          raw.rawLeased.tenth.name.value.getOrElse(""),
          raw.rawLeased.tenth.connection.value.getOrElse(""),
          raw.rawLeased.tenth.grantedDate.value.getOrElse(""),
          raw.rawLeased.tenth.annualAmount.value.getOrElse(""),
          raw.totalAmountOfIncomeAndReceipts.value,
          raw.rawDisposal.wereAnyDisposalOnThisDuringTheYear.value,
          raw.rawDisposal.totalSaleProceedIfAnyDisposal.value.getOrElse(""),
          raw.rawDisposal.first.name.value.getOrElse(""),
          raw.rawDisposal.first.connection.value.getOrElse(""),
          raw.rawDisposal.second.name.value.getOrElse(""),
          raw.rawDisposal.second.connection.value.getOrElse(""),
          raw.rawDisposal.third.name.value.getOrElse(""),
          raw.rawDisposal.third.connection.value.getOrElse(""),
          raw.rawDisposal.fourth.name.value.getOrElse(""),
          raw.rawDisposal.fourth.connection.value.getOrElse(""),
          raw.rawDisposal.fifth.name.value.getOrElse(""),
          raw.rawDisposal.fifth.connection.value.getOrElse(""),
          raw.rawDisposal.sixth.name.value.getOrElse(""),
          raw.rawDisposal.sixth.connection.value.getOrElse(""),
          raw.rawDisposal.seventh.name.value.getOrElse(""),
          raw.rawDisposal.seventh.connection.value.getOrElse(""),
          raw.rawDisposal.eighth.name.value.getOrElse(""),
          raw.rawDisposal.eighth.connection.value.getOrElse(""),
          raw.rawDisposal.ninth.name.value.getOrElse(""),
          raw.rawDisposal.ninth.connection.value.getOrElse(""),
          raw.rawDisposal.tenth.name.value.getOrElse(""),
          raw.rawDisposal.tenth.connection.value.getOrElse(""),
          raw.rawDisposal.isTransactionSupportedByIndependentValuation.value.getOrElse(""),
          raw.rawDisposal.hasLandOrPropertyFullyDisposedOf.value.getOrElse("")
        )
    }
  }

  implicit val formatRawAddressDetails: OFormat[RawAddressDetail] = Json.format[RawAddressDetail]
  implicit val formatRawAcquiredFrom: OFormat[RawAcquiredFromType] = Json.format[RawAcquiredFromType]
  implicit val formatRawJointlyHeld: OFormat[RawJointlyHeld] = Json.format[RawJointlyHeld]
  implicit val formatRawLessee: OFormat[RawLessee] = Json.format[RawLessee]
  implicit val formatRawLeased: OFormat[RawLeased] = Json.format[RawLeased]
  implicit val formatRawPurchaser: OFormat[RawPurchaser] = Json.format[RawPurchaser]
  implicit val formatRawDisposal: OFormat[RawDisposal] = Json.format[RawDisposal]
  implicit val formatTransactionRawDetails: OFormat[RawTransactionDetail] = Json.format[RawTransactionDetail]
}
