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

package services.validation

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNel
import cats.implicits._
import forms._
import models._
import models.requests.YesNo
import models.requests.YesNo.{No, Yes}
import models.requests.common._
import play.api.data.Form
import play.api.i18n.Messages

import javax.inject.Inject

class InterestLandOrPropertyValidationsService @Inject()(
  nameDOBFormProvider: NameDOBFormProvider,
  textFormProvider: TextFormProvider,
  dateFormPageProvider: DatePageFormProvider,
  moneyFormProvider: MoneyFormProvider,
  intFormProvider: IntFormProvider
) extends ValidationsService(
      nameDOBFormProvider,
      textFormProvider,
      dateFormPageProvider,
      moneyFormProvider,
      intFormProvider
    ) {

  private def acquiredFromTypeForm(memberFullDetails: String): Form[String] =
    textFormProvider.acquiredFromType(
      "acquiredFromType.upload.error.required",
      "acquiredFromType.upload.error.invalid",
      memberFullDetails
    )

  private def connectedOrUnconnectedTypeForm(memberFullDetails: String, key: String): Form[String] =
    textFormProvider.connectedOrUnconnectedType(
      s"$key.upload.error.required",
      s"$key.upload.error.invalid",
      memberFullDetails
    )

  def validateIsThereARegistryReference(
    isThereARegistryReference: CsvValue[String],
    noLandRegistryReference: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, RegistryDetails]] =
    for {
      validatedIsThereARegistryReference <- validateYesNoQuestion(
        isThereARegistryReference,
        "landOrProperty.isThereARegistryReference",
        memberFullNameDob,
        row
      )
      maybeNoLandRegistryReference = noLandRegistryReference.value.flatMap(
        reason =>
          validateFreeText(noLandRegistryReference.as(reason), "landOrProperty.noLandRegistryReference", memberFullNameDob, row)
      )
      referenceDetails <- (
        validatedIsThereARegistryReference,
        maybeNoLandRegistryReference
      ) match {
        case (Valid(isThereARegistryReference), _) if isThereARegistryReference.toUpperCase == "YES" =>
          Some(RegistryDetails(Yes, None, None).valid)
        case (Valid(isThereARegistryReference), mNoLandRegistryReference)
            if isThereARegistryReference.toUpperCase == "NO" =>
          mNoLandRegistryReference match {
            case Some(noLandRegistryReference) =>
              Some(noLandRegistryReference.map { noRef =>
                RegistryDetails(No, None, Some(noRef))
              })
            case _ => Some(
              ValidationError(
                s"$row",
                errorType = ValidationErrorType.FreeText,
                "landOrProperty.noLandRegistryReference.upload.error.required"
              ).invalidNel
            )
          }
        case _ => None
      }
    } yield referenceDetails

  def validateAcquiredFromType(
    acquiredFromType: CsvValue[String],
    memberFullName: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = acquiredFromTypeForm(memberFullName)
      .bind(
        Map(
          textFormProvider.formKey -> acquiredFromType.value.toUpperCase
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.AcquiredFromType,
      cellMapping = _ => Some(acquiredFromType.key.cell)
    )
  }

  def validateConnectedOrUnconnected(
    connectedOrUnconnected: CsvValue[String],
    memberFullName: String,
    row: Int,
    key: String
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = connectedOrUnconnectedTypeForm(memberFullName, key)
      .bind(
        Map(
          textFormProvider.formKey -> connectedOrUnconnected.value.toUpperCase
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.ConnectedUnconnectedType,
      cellMapping = _ => Some(connectedOrUnconnected.key.cell)
    )
  }

  def validateAcquiredFrom(
    acquiredFromType: CsvValue[String],
    acquirerNinoForIndividual: CsvValue[Option[String]],
    acquirerCrnForCompany: CsvValue[Option[String]],
    acquirerUtrForPartnership: CsvValue[Option[String]],
    noIdOrAcquiredFromAnotherSource: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, AcquiredFromType]] =
    for {
      validatedAcquiredFromType <- validateAcquiredFromType(acquiredFromType, memberFullNameDob, row)

      maybeNino = acquirerNinoForIndividual.value.flatMap(
        nino => validateNino(acquirerNinoForIndividual.as(nino), memberFullNameDob, row, "landOrProperty.acquirerNino")
      )
      maybeCrn = acquirerCrnForCompany.value.flatMap(
        crn => validateCrn(acquirerCrnForCompany.as(crn), memberFullNameDob, row, "landOrProperty.acquirerCrn")
      )
      maybeUtr = acquirerUtrForPartnership.value.flatMap(
        utr => validateUtr(acquirerUtrForPartnership.as(utr), memberFullNameDob, row, "landOrProperty.acquirerUtr")
      )
      maybeOther = noIdOrAcquiredFromAnotherSource.value.flatMap(
        other =>
          validateFreeText(
            noIdOrAcquiredFromAnotherSource.as(other),
            "landOrProperty.noIdOrAcquiredFromAnother",
            memberFullNameDob,
            row
          )
      )

      validatedAcquiredFrom <- (
        validatedAcquiredFromType,
        maybeNino,
        maybeCrn,
        maybeUtr,
        maybeOther
      ) match {
        case (Valid(acquiredFromType), mNino, _, _, mOther) if acquiredFromType.toUpperCase == "INDIVIDUAL" =>
          (mNino, mOther) match {
            case (Some(nino), None) =>
              Some((nino).map { nino =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = Some(nino.value),
                  reasonNoIdNumber = None,
                  otherDescription = None
                )
              })

            case (None, Some(other)) =>
              Some((other).map { other =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = None,
                  reasonNoIdNumber = Some(other),
                  otherDescription = None
                )
              })

            case _ => None
          }

        case (Valid(acquiredFromType), _, mCrn, _, mOther) if acquiredFromType.toUpperCase == "COMPANY" =>
          (mCrn, mOther) match {
            case (Some(crn), None) =>
              Some((crn).map { crn =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = Some(crn.value),
                  reasonNoIdNumber = None,
                  otherDescription = None
                )
              })
            case (None, Some(other)) =>
              Some((other).map { other =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = None,
                  reasonNoIdNumber = Some(other),
                  otherDescription = None
                )
              })

            case _ => None
          }

        case (Valid(acquiredFromType), _, _, mUtr, mOther) if acquiredFromType.toUpperCase == "PARTNERSHIP" =>
          (mUtr, mOther) match {
            case (Some(utr), None) =>
              Some((utr).map { utr =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = Some(utr.value),
                  reasonNoIdNumber = None,
                  otherDescription = None
                )
              })

            case (None, Some(other)) =>
              Some((other).map { other =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = None,
                  reasonNoIdNumber = Some(other),
                  otherDescription = None
                )
              })

            case _ => None
          }

        case (Valid(acquiredFromType), _, _, _, mOther) if acquiredFromType.toUpperCase == "OTHER" =>
          mOther match {
            case Some(other) =>
              Some((other).map { other =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = None,
                  reasonNoIdNumber = None,
                  otherDescription = Some(other)
                )
              })

            case _ => None
          }

        case (e @ Invalid(_), _, _, _, _) => Some(e)

        case _ => None
      }
    } yield validatedAcquiredFrom

  def validateJointlyHeld(
    nameJointlyOwning: CsvValue[Option[String]],
    ninoJointlyOwning: CsvValue[Option[String]],
    noNinoJointlyOwning: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int,
    isRequired: Boolean = false
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, Option[JointPropertyDetail]]] =
    if (isRequired && nameJointlyOwning.value.isEmpty) {
      Some(
        ValidationError(
          s"$row",
          ValidationErrorType.FreeText,
          message = "landOrProperty.jointlyName.upload.error.required"
        ).invalidNel
      )
    } else if (nameJointlyOwning.value.isEmpty) {
      Some(None.validNel)
    } else {
      for {
        name <- validateFreeText(
          nameJointlyOwning.as(nameJointlyOwning.value.get),
          "landOrProperty.jointlyName",
          memberFullNameDob,
          row
        )
        maybeNino = ninoJointlyOwning.value.flatMap(
          nino => validateNino(ninoJointlyOwning.as(nino), memberFullNameDob, row, "landOrProperty.jointlyNino")
        )
        maybeNoNino = noNinoJointlyOwning.value.flatMap(
          other => validateFreeText(noNinoJointlyOwning.as(other), "landOrProperty.jointlyNoNino", memberFullNameDob, row)
        )

        jointlyHeld <- (
          name,
          maybeNino,
          maybeNoNino
        ) match {
          case (Valid(name), mNino, mNoNino) =>
            (mNino, mNoNino) match {
              case (Some(nino), _) =>
                Some(nino.map { n =>
                  Some(
                    JointPropertyDetail(
                      name,
                      Some(n.value),
                      None
                    )
                  )
                })
              case (None, Some(noNino)) =>
                Some(noNino.map { n =>
                  Some(
                    JointPropertyDetail(
                      name,
                      None,
                      Some(n)
                    )
                  )
                })
              case _ => None
            }
          case (e @ Invalid(_), _, _) => Some(e)
          case _ => None
        }
      } yield jointlyHeld
    }

  def validateJointlyHeldAll(
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
    fifthPersonNoNinoJointlyOwning: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  )(
    implicit messages: Messages
  ): Option[ValidatedNel[ValidationError, (YesNo, Option[Int], Option[List[JointPropertyDetail]])]] =
    for {
      validatedIsPropertyHeldJointly <- validateYesNoQuestion(
        isPropertyHeldJointly,
        "landOrProperty.isPropertyHeldJointly",
        memberFullNameDob,
        row
      )

      maybeCount = howManyPersonsJointlyOwnProperty.value.flatMap(
        count =>
          validateCount(
            howManyPersonsJointlyOwnProperty.as(count),
            "landOrProperty.personCount",
            memberFullNameDob,
            row
          )
      )

      firstPerson = validateJointlyHeld(
        firstPersonNameJointlyOwning,
        firstPersonNinoJointlyOwning,
        firstPersonNoNinoJointlyOwning,
        memberFullNameDob,
        row,
        isRequired = true
      )

      secondPerson = validateJointlyHeld(
        secondPersonNameJointlyOwning,
        secondPersonNinoJointlyOwning,
        secondPersonNoNinoJointlyOwning,
        memberFullNameDob,
        row
      )

      thirdPerson = validateJointlyHeld(
        thirdPersonNameJointlyOwning,
        thirdPersonNinoJointlyOwning,
        thirdPersonNoNinoJointlyOwning,
        memberFullNameDob,
        row
      )

      fourthPerson = validateJointlyHeld(
        fourthPersonNameJointlyOwning,
        fourthPersonNinoJointlyOwning,
        fourthPersonNoNinoJointlyOwning,
        memberFullNameDob,
        row
      )

      fifthPerson = validateJointlyHeld(
        fifthPersonNameJointlyOwning,
        fifthPersonNinoJointlyOwning,
        fifthPersonNoNinoJointlyOwning,
        memberFullNameDob,
        row
      )

      jointlyHeld <- (
        validatedIsPropertyHeldJointly,
        maybeCount,
        firstPerson,
        secondPerson,
        thirdPerson,
        fourthPerson,
        fifthPerson
      ) match {
        case (Valid(isPropertyHeldJointly), mCount, mFirst, mSecond, mThird, mFourth, mFifth)
            if isPropertyHeldJointly.toUpperCase == "YES" =>
          (mCount, mFirst, mSecond, mThird, mFourth, mFifth) match {
            case (Some(count), Some(first), Some(second), Some(third), Some(fourth), Some(fifth)) => {
              (count, first) match {
                case (e @ Invalid(_), _) => Some(e)
                case (_, e @ Invalid(_)) => Some(e)
                case _ => Some((count, first, second, third, fourth, fifth).mapN { (c, f, s, t, fo, fi) =>
                  (Yes, Some(c), Some(List(f.get) ++ s ++ t ++ fo ++ fi))
                })
              }
            }
            case _ =>
              if(mCount.isEmpty) {
                Some(
                  ValidationError(
                    s"$row",
                    errorType = ValidationErrorType.YesNoQuestion,
                    "landOrProperty.personCount.upload.error.required"
                  ).invalidNel
                )
              } else if(mFirst.isEmpty) {
                Some(
                  ValidationError(
                    s"$row",
                    errorType = ValidationErrorType.FreeText,
                    "landOrProperty.firstJointlyPerson.upload.error.required"
                  ).invalidNel
                )
              } else {
                None
              }
          }

        case (Valid(isPropertyHeldJointly), _, _, _, _, _, _) if isPropertyHeldJointly.toUpperCase == "NO" =>
          Some((No, None, None).validNel)

        case (e @ Invalid(_), _, _, _, _, _, _) => Some(e)

        case _ => None
      }
    } yield jointlyHeld

  def validateLeasePerson(
    lesseeName: CsvValue[Option[String]],
    lesseeConnectedOrUnconnected: CsvValue[Option[String]],
    lesseeGrantedDate: CsvValue[Option[String]],
    lesseeAnnualAmount: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int,
    isRequired: Boolean = false
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, Option[LesseeDetail]]] =
    if (isRequired && lesseeName.value.isEmpty) {
      Some(
        ValidationError(
          s"$row",
          ValidationErrorType.FreeText,
          message = s"landOrProperty.lesseeName.upload.error.required"
        ).invalidNel
      )
    } else if (lesseeName.value.isEmpty) {
      Some(None.validNel)
    } else {
      for {
        name <- validateFreeText(
          lesseeName.as(lesseeName.value.get),
          s"landOrProperty.lesseeName",
          memberFullNameDob,
          row
        )
        maybeConnectedOrUnconnected = lesseeConnectedOrUnconnected.value.flatMap(
          c =>
            validateConnectedOrUnconnected(
              lesseeConnectedOrUnconnected.as(c),
              memberFullNameDob,
              row,
              s"landOrProperty.lesseeType"
            )
        )
        maybeLesseeGrantedDate = lesseeGrantedDate.value.flatMap(
          date => validateDate(lesseeGrantedDate.as(date), s"landOrProperty.lesseeGrantedDate", row, None)
        )
        maybeLesseeAnnualAmount = lesseeAnnualAmount.value.flatMap(
          p => validatePrice(lesseeAnnualAmount.as(p), s"landOrProperty.lesseeAnnualAmount", memberFullNameDob, row)
        )

        jointlyHeld <- (
          name,
          maybeConnectedOrUnconnected,
          maybeLesseeGrantedDate,
          maybeLesseeAnnualAmount
        ) match {
          case (Valid(name), mCon, mDate, mAmount) =>
            (mCon, mDate, mAmount) match {
              case (Some(con), Some(date), Some(amount)) => {
                (con, date, amount) match {
                  case (e @ Invalid(_), _, _) => Some(e)
                  case (_, e @ Invalid(_), _) => Some(e)
                  case (_, _, e @ Invalid(_)) => Some(e)
                  case _ => Some((con, date, amount).mapN { (c, d, a) =>
                    Some(
                      LesseeDetail(
                        name,
                        ConnectedOrUnconnectedType.uploadStringToRequestConnectedOrUnconnected(c),
                        d,
                        a.value
                      )
                    )
                  })
                }
              }
              case _ =>
                if (mCon.isEmpty)
                  Some(
                    ValidationError(
                      s"$row",
                      errorType = ValidationErrorType.Price,
                      "landOrProperty.lesseeType.upload.error.required"
                    ).invalidNel
                  )
                else if (mDate.isEmpty)
                  Some(
                    ValidationError(
                      s"$row",
                      errorType = ValidationErrorType.YesNoQuestion,
                      "landOrProperty.lesseeGrantedDate.upload.error.required"
                    ).invalidNel
                  )
                else if(mAmount.isEmpty)
                  Some(
                    ValidationError(
                      s"$row",
                      errorType = ValidationErrorType.YesNoQuestion,
                      "landOrProperty.lesseeAnnualAmount.upload.error.required"
                    ).invalidNel
                  )
                else
                  None
            }
          case (e @ Invalid(_), _, _, _) => Some(e)
          case _ => None
        }
      } yield jointlyHeld
    }
  def validateLeasedAll(
    isLeased: CsvValue[String],
    firstLesseeName: CsvValue[Option[String]],
    firstLesseeConnectedOrUnconnected: CsvValue[Option[String]],
    firstLesseeGrantedDate: CsvValue[Option[String]],
    firstLesseeAnnualAmount: CsvValue[Option[String]],
    secondLesseeName: CsvValue[Option[String]],
    secondLesseeConnectedOrUnconnected: CsvValue[Option[String]],
    secondLesseeGrantedDate: CsvValue[Option[String]],
    secondLesseeAnnualAmount: CsvValue[Option[String]],
    thirdLesseeName: CsvValue[Option[String]],
    thirdLesseeConnectedOrUnconnected: CsvValue[Option[String]],
    thirdLesseeGrantedDate: CsvValue[Option[String]],
    thirdLesseeAnnualAmount: CsvValue[Option[String]],
    fourthLesseeName: CsvValue[Option[String]],
    fourthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
    fourthLesseeGrantedDate: CsvValue[Option[String]],
    fourthLesseeAnnualAmount: CsvValue[Option[String]],
    fifthLesseeName: CsvValue[Option[String]],
    fifthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
    fifthLesseeGrantedDate: CsvValue[Option[String]],
    fifthLesseeAnnualAmount: CsvValue[Option[String]],
    sixthLesseeName: CsvValue[Option[String]],
    sixthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
    sixthLesseeGrantedDate: CsvValue[Option[String]],
    sixthLesseeAnnualAmount: CsvValue[Option[String]],
    seventhLesseeName: CsvValue[Option[String]],
    seventhLesseeConnectedOrUnconnected: CsvValue[Option[String]],
    seventhLesseeGrantedDate: CsvValue[Option[String]],
    seventhLesseeAnnualAmount: CsvValue[Option[String]],
    eighthLesseeName: CsvValue[Option[String]],
    eighthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
    eighthLesseeGrantedDate: CsvValue[Option[String]],
    eighthLesseeAnnualAmount: CsvValue[Option[String]],
    ninthLesseeName: CsvValue[Option[String]],
    ninthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
    ninthLesseeGrantedDate: CsvValue[Option[String]],
    ninthLesseeAnnualAmount: CsvValue[Option[String]],
    tenthLesseeName: CsvValue[Option[String]],
    tenthLesseeConnectedOrUnconnected: CsvValue[Option[String]],
    tenthLesseeGrantedDate: CsvValue[Option[String]],
    tenthLesseeAnnualAmount: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, (YesNo, Option[List[LesseeDetail]])]] =
    for {
      validatedIsLeased <- validateYesNoQuestion(
        isLeased,
        "isLeased",
        memberFullNameDob,
        row
      )

      firstLessee = validateLeasePerson(
        firstLesseeName,
        firstLesseeConnectedOrUnconnected,
        firstLesseeGrantedDate,
        firstLesseeAnnualAmount,
        memberFullNameDob,
        row,
        isRequired = true
      )

      secondLessee = validateLeasePerson(
        secondLesseeName,
        secondLesseeConnectedOrUnconnected,
        secondLesseeGrantedDate,
        secondLesseeAnnualAmount,
        memberFullNameDob,
        row
      )

      thirdLessee = validateLeasePerson(
        thirdLesseeName,
        thirdLesseeConnectedOrUnconnected,
        thirdLesseeGrantedDate,
        thirdLesseeAnnualAmount,
        memberFullNameDob,
        row
      )

      fourthLessee = validateLeasePerson(
        fourthLesseeName,
        fourthLesseeConnectedOrUnconnected,
        fourthLesseeGrantedDate,
        fourthLesseeAnnualAmount,
        memberFullNameDob,
        row
      )

      fifthLessee = validateLeasePerson(
        fifthLesseeName,
        fifthLesseeConnectedOrUnconnected,
        fifthLesseeGrantedDate,
        fifthLesseeAnnualAmount,
        memberFullNameDob,
        row
      )

      sixthLessee = validateLeasePerson(
        sixthLesseeName,
        sixthLesseeConnectedOrUnconnected,
        sixthLesseeGrantedDate,
        sixthLesseeAnnualAmount,
        memberFullNameDob,
        row
      )

      seventhLessee = validateLeasePerson(
        seventhLesseeName,
        seventhLesseeConnectedOrUnconnected,
        seventhLesseeGrantedDate,
        seventhLesseeAnnualAmount,
        memberFullNameDob,
        row
      )

      eighthLessee = validateLeasePerson(
        eighthLesseeName,
        eighthLesseeConnectedOrUnconnected,
        eighthLesseeGrantedDate,
        eighthLesseeAnnualAmount,
        memberFullNameDob,
        row
      )

      ninthLessee = validateLeasePerson(
        ninthLesseeName,
        ninthLesseeConnectedOrUnconnected,
        ninthLesseeGrantedDate,
        ninthLesseeAnnualAmount,
        memberFullNameDob,
        row
      )

      tenthLessee = validateLeasePerson(
        tenthLesseeName,
        tenthLesseeConnectedOrUnconnected,
        tenthLesseeGrantedDate,
        tenthLesseeAnnualAmount,
        memberFullNameDob,
        row
      )

      lesseeDetails <- (
        validatedIsLeased,
        firstLessee,
        secondLessee,
        thirdLessee,
        fourthLessee,
        fifthLessee,
        sixthLessee,
        seventhLessee,
        eighthLessee,
        ninthLessee,
        tenthLessee
      ) match {
        case (Valid(isLeased), mFirst, mSecond, mThird, mFourth, mFifth, mSixth, mSeventh, mEighth, mNinth, mTenth) if isLeased.toUpperCase == "YES" =>
          (mFirst, mSecond, mThird, mFourth, mFifth, mSixth, mSeventh, mEighth, mNinth, mTenth) match {
            case (Some(first), Some(second), Some(third), Some(fourth), Some(fifth), Some(sixth), Some(seventh), Some(eighth), Some(ninth), Some(tenth)) => {
              first match {
                case (e @ Invalid(_)) => Some(e)
                case _ => Some((first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth).mapN {
                  (l1, l2, l3, l4, l5, l6, l7, l8, l9, l10) =>
                    (Yes, Some(List(l1.get) ++ l2 ++ l3 ++ l4 ++ l5 ++ l6 ++ l7 ++ l8 ++ l9 ++ l10))
                })
              }
            }
            case _ => None
          }

        case (Valid(isLeased), _, _, _, _, _, _, _, _, _, _) if isLeased.toUpperCase == "NO" =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _, _, _, _, _, _, _, _, _, _) => Some(e)

        case _ => None
      }
    } yield lesseeDetails

  def validatePurchaser(
    purchaserName: CsvValue[Option[String]],
    purchaserConnectedOrUnconnected: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int,
    isRequired: Boolean = false
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, Option[DispossalDetail.PurchaserDetail]]] =
    if (isRequired && purchaserName.value.isEmpty) {
      Some(
        ValidationError(
          s"$row",
          ValidationErrorType.FreeText,
          message = "landOrProperty.purchaserName.upload.error.required"
        ).invalidNel
      )
    } else if (purchaserName.value.isEmpty) {
      Some(None.validNel)
    } else {
      for {
        name <- validateFreeText(
          purchaserName.as(purchaserName.value.get),
          "landOrProperty.purchaserName",
          memberFullNameDob,
          row
        )
        maybeConnectedOrUnconnected = purchaserConnectedOrUnconnected.value.flatMap(
          c =>
            validateConnectedOrUnconnected(
              purchaserConnectedOrUnconnected.as(c),
              memberFullNameDob,
              row,
              s"landOrProperty.purchaserType"
            )
        )

        purchaser <- (
          name,
          maybeConnectedOrUnconnected
        ) match {
          case (Valid(name), mCon) =>
            mCon match {
              case (Some(con)) =>
                con match {
                  case (e @ Invalid(_)) => Some(e)
                  case _ =>
                    Some(con.map { c =>
                      Some(
                        DispossalDetail.PurchaserDetail(
                          ConnectedOrUnconnectedType.uploadStringToRequestConnectedOrUnconnected(c),
                          name
                        )
                      )
                    })
                }
              case _ => Some(
                ValidationError(
                  s"$row",
                  ValidationErrorType.FreeText,
                  message = "landOrProperty.purchaserType.upload.error.required"
                ).invalidNel
              )
            }
          case (e @ Invalid(_), _) => Some(e)
          case _ => None
        }
      } yield purchaser
    }

  def validateDisposals(
    wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
    totalSaleProceedIfAnyDisposal: CsvValue[Option[String]],
    firstPurchaserName: CsvValue[Option[String]],
    firstPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
    secondPurchaserName: CsvValue[Option[String]],
    secondPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
    thirdPurchaserName: CsvValue[Option[String]],
    thirdPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
    fourthPurchaserName: CsvValue[Option[String]],
    fourthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
    fifthPurchaserName: CsvValue[Option[String]],
    fifthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
    sixthPurchaserName: CsvValue[Option[String]],
    sixthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
    seventhPurchaserName: CsvValue[Option[String]],
    seventhPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
    eighthPurchaserName: CsvValue[Option[String]],
    eighthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
    ninthPurchaserName: CsvValue[Option[String]],
    ninthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
    tenthPurchaserName: CsvValue[Option[String]],
    tenthPurchaserConnectedOrUnconnected: CsvValue[Option[String]],
    isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
    hasLandOrPropertyFullyDisposedOf: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, (YesNo, Option[DispossalDetail])]] =
    for {
      validatedWereAnyDisposalOnThisDuringTheYear <- validateYesNoQuestion(
        wereAnyDisposalOnThisDuringTheYear,
        "landOrProperty.isAnyDisposalMade",
        memberFullNameDob,
        row
      )

      maybeDisposalAmount = totalSaleProceedIfAnyDisposal.value.flatMap(
        p =>
          validatePrice(totalSaleProceedIfAnyDisposal.as(p), s"landOrProperty.disposedAmount", memberFullNameDob, row)
      )

      firstPurchaser = validatePurchaser(
        firstPurchaserName,
        firstPurchaserConnectedOrUnconnected,
        memberFullNameDob,
        row,
        isRequired = true
      )

      secondPurchaser = validatePurchaser(
        secondPurchaserName,
        secondPurchaserConnectedOrUnconnected,
        memberFullNameDob,
        row
      )

      thirdPurchaser = validatePurchaser(
        thirdPurchaserName,
        thirdPurchaserConnectedOrUnconnected,
        memberFullNameDob,
        row
      )

      fourthPurchaser = validatePurchaser(
        fourthPurchaserName,
        fourthPurchaserConnectedOrUnconnected,
        memberFullNameDob,
        row
      )

      fifthPurchaser = validatePurchaser(
        fifthPurchaserName,
        fifthPurchaserConnectedOrUnconnected,
        memberFullNameDob,
        row
      )

      sixthPurchaser = validatePurchaser(
        sixthPurchaserName,
        sixthPurchaserConnectedOrUnconnected,
        memberFullNameDob,
        row
      )

      seventhPurchaser = validatePurchaser(
        seventhPurchaserName,
        seventhPurchaserConnectedOrUnconnected,
        memberFullNameDob,
        row
      )

      eighthPurchaser = validatePurchaser(
        eighthPurchaserName,
        eighthPurchaserConnectedOrUnconnected,
        memberFullNameDob,
        row
      )

      ninthPurchaser = validatePurchaser(
        ninthPurchaserName,
        ninthPurchaserConnectedOrUnconnected,
        memberFullNameDob,
        row
      )

      tenthPurchaser = validatePurchaser(
        tenthPurchaserName,
        tenthPurchaserConnectedOrUnconnected,
        memberFullNameDob,
        row
      )

      maybeIsTransactionSupportedByIndependentValuation = isTransactionSupportedByIndependentValuation.value.flatMap(
        p =>
          validateYesNoQuestion(
            totalSaleProceedIfAnyDisposal.as(p),
            s"landOrProperty.isTransactionSupported",
            memberFullNameDob,
            row
          )
      )

      maybeHasLandOrPropertyFullyDisposedOf = hasLandOrPropertyFullyDisposedOf.value.flatMap(
        p =>
          validateYesNoQuestion(
            hasLandOrPropertyFullyDisposedOf.as(p),
            s"landOrProperty.isFullyDisposedOf",
            memberFullNameDob,
            row
          )
      )

      disposalDetails <- (
        validatedWereAnyDisposalOnThisDuringTheYear,
        maybeDisposalAmount,
        firstPurchaser,
        secondPurchaser,
        thirdPurchaser,
        fourthPurchaser,
        fifthPurchaser,
        sixthPurchaser,
        seventhPurchaser,
        eighthPurchaser,
        ninthPurchaser,
        tenthPurchaser,
        maybeIsTransactionSupportedByIndependentValuation,
        maybeHasLandOrPropertyFullyDisposedOf
      ) match {
        case (Valid(isLeased), mAmount, mFirst, mSecond, mThird, mFourth, mFifth, mSixth, mSeventh, mEighth, mNinth, mTenth, mIndependent, mFully) if isLeased.toUpperCase == "YES" =>
          (mAmount, mFirst, mSecond, mThird, mFourth, mFifth, mSixth, mSeventh, mEighth, mNinth, mTenth, mIndependent, mFully) match {
            case (mAmount, Some(first), Some(second), Some(third), Some(fourth), Some(fifth), Some(sixth), Some(seventh), Some(eighth), Some(ninth), Some(tenth), mDepend, mFully) =>
              (mAmount, mDepend, mFully) match {
                case (Some(amount), Some(depend), Some(fully)) => {
                  (amount, depend, fully) match {
                    case (e@Invalid(_), _, _) => Some(e)
                    case (_, e@Invalid(_), _) => Some(e)
                    case (_, _, e@Invalid(_)) => Some(e)
                    case _ =>
                      Some(
                        (amount, first, second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, depend, fully)
                          .mapN { (a, l1, l2, l3, l4, l5, l6, l7, l8, l9, l10, d, f) =>
                            (
                              Yes,
                              Some(DispossalDetail(disposedPropertyProceedsAmt = a.value, independentValutionDisposal = YesNo.uploadYesNoToRequestYesNo(d), propertyFullyDisposed = YesNo.uploadYesNoToRequestYesNo(f), purchaserDetails = List(l1.get) ++ l2 ++ l3 ++ l4 ++ l5 ++ l6 ++ l7 ++ l8 ++ l9 ++ l10
                            )))
                          }
                      )
                  }
                }
                case _ =>
                  if (mAmount.isEmpty)
                    Some(
                      ValidationError(
                        s"$row",
                        errorType = ValidationErrorType.Price,
                        "landOrProperty.disposedAmount.upload.error.required"
                      ).invalidNel
                    )
                  else if (mDepend.isEmpty)
                    Some(
                      ValidationError(
                        s"$row",
                        errorType = ValidationErrorType.YesNoQuestion,
                        "landOrProperty.isTransactionSupported.upload.error.required"
                      ).invalidNel
                    )
                  else if(mFully.isEmpty)
                    Some(
                      ValidationError(
                        s"$row",
                        errorType = ValidationErrorType.YesNoQuestion,
                        "landOrProperty.isFullyDisposedOf.upload.error.required"
                      ).invalidNel
                    )
                  else None
              }
            case _ => None
          }

        case (Valid(isLeased), _, _, _, _, _, _, _, _, _, _, _, _, _) if isLeased.toUpperCase == "NO" =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _, _, _, _, _, _, _, _, _, _, _, _, _) => Some(e)

        case _ => None
      }
    } yield disposalDetails
}
