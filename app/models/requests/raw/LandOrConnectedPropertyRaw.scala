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

import models.CsvValue
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
