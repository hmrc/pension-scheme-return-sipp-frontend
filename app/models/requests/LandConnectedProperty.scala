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

package models.requests

import cats.data.NonEmptyList
import models.{CsvValue, _}
import models.requests.common._
import play.api.libs.json.{Format, Json, OFormat, Reads, Writes}

import java.time.LocalDate

case class LandConnectedProperty(
  noOfTransactions: Int,
  transactionDetails: Option[List[LandConnectedProperty.TransactionDetail]]
)

object LandConnectedProperty {

  case class RawAddressDetail(
    /*      G */ isLandOrPropertyInUK: CsvValue[String],
    /*      H */ landOrPropertyUkAddressLine1: CsvValue[Option[String]],
    /*      I */ landOrPropertyUkAddressLine2: CsvValue[Option[String]],
    /*      J */ landOrPropertyUkAddressLine3: CsvValue[Option[String]],
    /*      K */ landOrPropertyUkTownOrCity: CsvValue[Option[String]],
    /*      L */ landOrPropertyUkPostCode: CsvValue[Option[String]],
    /*      M */ landOrPropertyAddressLine1: CsvValue[Option[String]],
    /*      N */ landOrPropertyAddressLine2: CsvValue[Option[String]],
    /*      O */ landOrPropertyAddressLine3: CsvValue[Option[String]],
    /*      P */ landOrPropertyAddressLine4: CsvValue[Option[String]],
    /*      Q */ landOrPropertyCountry: CsvValue[Option[String]]
  )

  case class RawAcquiredFrom(
    /*      T */ acquiredFromType: CsvValue[String],
    /*      U */ acquirerNinoForIndividual: CsvValue[Option[String]],
    /*      V */ acquirerCrnForCompany: CsvValue[Option[String]],
    /*      W */ acquirerUtrForPartnership: CsvValue[Option[String]],
    /*      X */ noIdOrAcquiredFromAnotherSource: CsvValue[Option[String]]
  )

  case class RawJointlyHeld(
    /*     AA */ isPropertyHeldJointly: CsvValue[String],
    /*     AB */ howManyPersonsJointlyOwnProperty: CsvValue[Option[String]],
    /*     AC */ firstPersonNameJointlyOwning: CsvValue[Option[String]],
    /*     AD */ firstPersonNinoJointlyOwning: CsvValue[Option[String]],
    /*     AE */ firstPersonNoNinoJointlyOwning: CsvValue[Option[String]],
    /*     AF */ secondPersonNameJointlyOwning: CsvValue[Option[String]],
    /*     AG */ secondPersonNinoJointlyOwning: CsvValue[Option[String]],
    /*     AH */ secondPersonNoNinoJointlyOwning: CsvValue[Option[String]],
    /*     AI */ thirdPersonNameJointlyOwning: CsvValue[Option[String]],
    /*     AJ */ thirdPersonNinoJointlyOwning: CsvValue[Option[String]],
    /*     AK */ thirdPersonNoNinoJointlyOwning: CsvValue[Option[String]],
    /*     AL */ fourthPersonNameJointlyOwning: CsvValue[Option[String]],
    /*     AM */ fourthPersonNinoJointlyOwning: CsvValue[Option[String]],
    /*     AN */ fourthPersonNoNinoJointlyOwning: CsvValue[Option[String]],
    /*     AO */ fifthPersonNameJointlyOwning: CsvValue[Option[String]],
    /*     AP */ fifthPersonNinoJointlyOwning: CsvValue[Option[String]],
    /*     AQ */ fifthPersonNoNinoJointlyOwning: CsvValue[Option[String]]
  )

  case class RawLessee(
    name: CsvValue[Option[String]],
    connection: CsvValue[Option[String]],
    grantedDate: CsvValue[Option[String]],
    annualAmount: CsvValue[Option[String]]
  )

  case class RawLeased(
    /*     AS */ isLeased: CsvValue[String],
    /*AT - AW */ first: RawLessee,
    /*AX - BA */ second: RawLessee,
    /*BB - BE */ third: RawLessee,
    /*BF - BI */ fourth: RawLessee,
    /*BJ - BM */ fifth: RawLessee,
    /*BN - BQ */ sixth: RawLessee,
    /*BR - BU */ seventh: RawLessee,
    /*BV - BY */ eighth: RawLessee,
    /*CA - CC */ ninth: RawLessee,
    /*CD - CG */ tenth: RawLessee
  )

  case class RawPurchaser(
    name: CsvValue[Option[String]],
    connection: CsvValue[Option[String]]
  )

  case class RawDisposal(
    /*     CI */ wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
    /*     CJ */ totalSaleProceedIfAnyDisposal: CsvValue[Option[String]],
    /*CK - CL */ first: RawPurchaser,
    /*CM - CN */ second: RawPurchaser,
    /*CO - CP */ third: RawPurchaser,
    /*CQ - CR */ fourth: RawPurchaser,
    /*CS - CT */ fifth: RawPurchaser,
    /*CU - CW */ sixth: RawPurchaser,
    /*CX - CV */ seventh: RawPurchaser,
    /*CY - DZ */ eighth: RawPurchaser,
    /*DA - DB */ ninth: RawPurchaser,
    /*DC - DD */ tenth: RawPurchaser,
    /*     DE */ isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
    /*     DF */ hasLandOrPropertyFullyDisposedOf: CsvValue[Option[String]]
  )

  case class RawTransactionDetail(
    row: Int,
    /*      B */ firstNameOfSchemeMember: CsvValue[String],
    /*      C */ lastNameOfSchemeMember: CsvValue[String],
    /*      D */ memberDateOfBirth: CsvValue[String],
    /*      E */ countOfLandOrPropertyTransactions: CsvValue[String],
    /*      F */ acquisitionDate: CsvValue[String],
    /*  G - Q */ rawAddressDetail: RawAddressDetail,
    /*      R */ isThereLandRegistryReference: CsvValue[String],
    /*      S */ noLandRegistryReference: CsvValue[Option[String]],
    /*  T - X */ rawAcquiredFrom: RawAcquiredFrom,
    /*      Y */ totalCostOfLandOrPropertyAcquired: CsvValue[String],
    /*      Z */ isSupportedByAnIndependentValuation: CsvValue[String],
    /*AA - AQ */ rawJointlyHeld: RawJointlyHeld,
    /*     AR */ isPropertyDefinedAsSchedule29a: CsvValue[String],
    /*AS - CG */ rawLeased: RawLeased,
    /*     CH */ totalAmountOfIncomeAndReceipts: CsvValue[String],
    /*CI - DF */ rawDisposal: RawDisposal
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
      /*  T */ acquiredFromType: CsvValue[String],
      /*  U */ acquirerNinoForIndividual: CsvValue[Option[String]],
      /*  V */ acquirerCrnForCompany: CsvValue[Option[String]],
      /*  W */ acquirerUtrForPartnership: CsvValue[Option[String]],
      /*  X */ noIdOrAcquiredFromAnotherSource: CsvValue[Option[String]],
      /*  Y */ totalCostOfLandOrPropertyAcquired: CsvValue[String],
      /*  Z */ isSupportedByAnIndependentValuation: CsvValue[String],
      /* AA */ isPropertyHeldJointly: CsvValue[String],
      /* AB */ howManyPersonsJointlyOwnProperty: CsvValue[Option[String]],
      /* AC */ firstPersonNameJointlyOwning: CsvValue[Option[String]],
      /* AD */ firstPersonNinoJointlyOwning: CsvValue[Option[String]],
      /* AE */ firstPersonNoNinoJointlyOwning: CsvValue[Option[String]],
      /* AF */ secondPersonNameJointlyOwning: CsvValue[Option[String]],
      /* AG */ secondPersonNinoJointlyOwning: CsvValue[Option[String]],
      /* AH */ secondPersonNoNinoJointlyOwning: CsvValue[Option[String]],
      /* AI */ thirdPersonNameJointlyOwning: CsvValue[Option[String]],
      /* AJ */ thirdPersonNinoJointlyOwning: CsvValue[Option[String]],
      /* AK */ thirdPersonNoNinoJointlyOwning: CsvValue[Option[String]],
      /* AL */ fourthPersonNameJointlyOwning: CsvValue[Option[String]],
      /* AM */ fourthPersonNinoJointlyOwning: CsvValue[Option[String]],
      /* AN */ fourthPersonNoNinoJointlyOwning: CsvValue[Option[String]],
      /* AO */ fifthPersonNameJointlyOwning: CsvValue[Option[String]],
      /* AP */ fifthPersonNinoJointlyOwning: CsvValue[Option[String]],
      /* AQ */ fifthPersonNoNinoJointlyOwning: CsvValue[Option[String]],
      /* AR */ isPropertyDefinedAsSchedule29a: CsvValue[String],
      /* AS */ isLeased: CsvValue[String],
      /* AT */ firstLesseeName: CsvValue[Option[String]],
      /* AU */ firstLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* AV */ firstLesseeGrantedDate: CsvValue[Option[String]],
      /* AW */ firstLesseeAnnualAmount: CsvValue[Option[String]],
      /* AX */ secondLesseeName: CsvValue[Option[String]],
      /* AY */ secondLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* AZ */ secondLesseeGrantedDate: CsvValue[Option[String]],
      /* BA */ secondLesseeAnnualAmount: CsvValue[Option[String]],
      /* BB */ thirdLesseeName: CsvValue[Option[String]],
      /* BC */ thirdLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* BD */ thirdLesseeGrantedDate: CsvValue[Option[String]],
      /* BE */ thirdLesseeAnnualAmount: CsvValue[Option[String]],
      /* BF */ fourthLesseeName: CsvValue[Option[String]],
      /* BG */ fourthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* BH */ fourthLesseeGrantedDate: CsvValue[Option[String]],
      /* BI */ fourthLesseeAnnualAmount: CsvValue[Option[String]],
      /* BJ */ fifthLesseeName: CsvValue[Option[String]],
      /* BK */ fifthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* BL */ fifthLesseeGrantedDate: CsvValue[Option[String]],
      /* BM */ fifthLesseeAnnualAmount: CsvValue[Option[String]],
      /* BN */ sixthLesseeName: CsvValue[Option[String]],
      /* BO */ sixthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* BP */ sixthLesseeGrantedDate: CsvValue[Option[String]],
      /* BQ */ sixthLesseeAnnualAmount: CsvValue[Option[String]],
      /* BR */ seventhLesseeName: CsvValue[Option[String]],
      /* BS */ seventhLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* BT */ seventhLesseeGrantedDate: CsvValue[Option[String]],
      /* BU */ seventhLesseeAnnualAmount: CsvValue[Option[String]],
      /* BV */ eighthLesseeName: CsvValue[Option[String]],
      /* BW */ eighthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* BX */ eighthLesseeGrantedDate: CsvValue[Option[String]],
      /* BY */ eighthLesseeAnnualAmount: CsvValue[Option[String]],
      /* BZ */ ninthLesseeName: CsvValue[Option[String]],
      /* CA */ ninthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* CB */ ninthLesseeGrantedDate: CsvValue[Option[String]],
      /* CC */ ninthLesseeAnnualAmount: CsvValue[Option[String]],
      /* CD */ tenthLesseeName: CsvValue[Option[String]],
      /* CE */ tenthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
      /* CF */ tenthLesseeGrantedDate: CsvValue[Option[String]],
      /* CG */ tenthLesseeAnnualAmount: CsvValue[Option[String]],
      /* CH */ totalAmountOfIncomeAndReceipts: CsvValue[String],
      /* CI */ wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
      /* CJ */ totalSaleProceedIfAnyDisposal: CsvValue[Option[String]],
      /* CK */ firstPurchaserName: CsvValue[Option[String]],
      /* CL */ firstPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* CM */ secondPurchaserName: CsvValue[Option[String]],
      /* CN */ secondPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* CO */ thirdPurchaserName: CsvValue[Option[String]],
      /* CP */ thirdPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* CQ */ fourthPurchaserName: CsvValue[Option[String]],
      /* CR */ fourthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* CS */ fifthPurchaserName: CsvValue[Option[String]],
      /* CT */ fifthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* CU */ sixthPurchaserName: CsvValue[Option[String]],
      /* CV */ sixthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* CW */ seventhPurchaserName: CsvValue[Option[String]],
      /* CX */ seventhPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* CY */ eighthPurchaserName: CsvValue[Option[String]],
      /* CZ */ eighthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* DA */ ninthPurchaserName: CsvValue[Option[String]],
      /* DB */ ninthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* DC */ tenthPurchaserName: CsvValue[Option[String]],
      /* DD */ tenthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
      /* DE */ isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
      /* DF */ hasLandOrPropertyFullyDisposedOf: CsvValue[Option[String]]
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
      RawAcquiredFrom(
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

  case class TransactionDetail(
    row: Int,
    nameDOB: NameDOB,
    acquisitionDate: LocalDate,
    landOrPropertyinUK: YesNo,
    addressDetails: AddressDetails,
    registryDetails: RegistryDetails,
    acquiredFromName: String,
    acquiredFromType: AcquiredFromType,
    totalCost: Double,
    independentValution: YesNo,
    jointlyHeld: YesNo,
    noOfPersons: Option[Int],
    jointPropertyPersonDetails: Option[List[JointPropertyDetail]],
    residentialSchedule29A: YesNo,
    isLeased: YesNo,
    lesseeDetails: Option[List[LesseeDetail]],
    totalIncomeOrReceipts: Double,
    isPropertyDisposed: YesNo,
    disposalDetails: Option[DispossalDetail]
  )

  implicit def nonEmptyListFormat[T: Format]: Format[NonEmptyList[T]] = Format(
    Reads.list[T].flatMap { xs =>
      NonEmptyList.fromList(xs).fold[Reads[NonEmptyList[T]]](Reads.failed("The list is empty"))(Reads.pure(_))
    },
    Writes.list[T].contramap(_.toList)
  )

  implicit val formatRawAddressDetails: OFormat[RawAddressDetail] = Json.format[RawAddressDetail]
  implicit val formatRawAcquiredFrom: OFormat[RawAcquiredFrom] = Json.format[RawAcquiredFrom]
  implicit val formatRawJointlyHeld: OFormat[RawJointlyHeld] = Json.format[RawJointlyHeld]
  implicit val formatRawLessee: OFormat[RawLessee] = Json.format[RawLessee]
  implicit val formatRawLeased: OFormat[RawLeased] = Json.format[RawLeased]
  implicit val formatRawPurchaser: OFormat[RawPurchaser] = Json.format[RawPurchaser]
  implicit val formatRawDisposal: OFormat[RawDisposal] = Json.format[RawDisposal]
  implicit val formatTransactionRawDetails: OFormat[RawTransactionDetail] = Json.format[RawTransactionDetail]
  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
  implicit val formatLandConnectedParty: OFormat[LandConnectedProperty] = Json.format[LandConnectedProperty]
}
