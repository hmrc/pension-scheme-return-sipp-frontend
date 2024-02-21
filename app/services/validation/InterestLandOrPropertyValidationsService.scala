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
import cats.data.{Validated, ValidatedNel}
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
          validateFreeText(
            noLandRegistryReference.as(reason),
            "landOrProperty.noLandRegistryReference",
            memberFullNameDob,
            row
          )
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
            case _ =>
              Some(
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
    key: String,
    memberFullName: String,
    row: Int
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
          other =>
            validateFreeText(noNinoJointlyOwning.as(other), "landOrProperty.jointlyNoNino", memberFullNameDob, row)
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
    jointlyPersonList: List[(CsvValue[Option[String]], CsvValue[Option[String]], CsvValue[Option[String]])],
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

      jointlyHeldPeople = jointlyPersonList.zipWithIndex.map {
        case (p, i) =>
          validateJointlyHeld(p._1, p._2, p._3, memberFullNameDob, row, isRequired = (i == 0))
      }

      jointlyHeld <- (
        validatedIsPropertyHeldJointly,
        maybeCount,
        jointlyHeldPeople
      ) match {
        case (Valid(isPropertyHeldJointly), mCount, jointlyHeldPeople) if isPropertyHeldJointly.toUpperCase == "YES" =>
          (mCount, jointlyHeldPeople.head, jointlyHeldPeople.tail.sequence) match {
            case (Some(count), Some(first), Some(people)) => {
              (count, first) match {
                case (e @ Invalid(_), _) => Some(e)
                case (_, e @ Invalid(_)) => Some(e)
                case _ => {
                  val pTraverse = people.traverse(_.toEither)
                  pTraverse match {
                    case Right(people) =>
                      Some((count, first).mapN { (c, f) =>
                        (Yes, Some(c), Some(List(f.get) ++ people.flatten))
                      })
                    case Left(error) => Some(Validated.invalid(error))
                  }
                }
              }
            }
            case _ =>
              if (mCount.isEmpty) {
                Some(
                  ValidationError(
                    s"$row",
                    errorType = ValidationErrorType.YesNoQuestion,
                    "landOrProperty.personCount.upload.error.required"
                  ).invalidNel
                )
              } else if (jointlyHeldPeople.head.isEmpty) {
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

        case (Valid(isPropertyHeldJointly), _, _) if isPropertyHeldJointly.toUpperCase == "NO" =>
          Some((No, None, None).validNel)

        case (e @ Invalid(_), _, _) => Some(e)

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
          message = s"landOrProperty.firstLessee.upload.error.required"
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
              s"landOrProperty.lesseeType",
              memberFullNameDob,
              row
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
                  case _ =>
                    Some((con, date, amount).mapN { (_count, _date, _amount) =>
                      val cType = ConnectedOrUnconnectedType.uploadStringToRequestConnectedOrUnconnected(_count)
                      Some(LesseeDetail(name, cType, _date, _amount.value))
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
                else if (mAmount.isEmpty)
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
    lesseePeople: List[
      (CsvValue[Option[String]], CsvValue[Option[String]], CsvValue[Option[String]], CsvValue[Option[String]])
    ],
    memberFullNameDob: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, (YesNo, Option[List[LesseeDetail]])]] =
    for {
      validatedIsLeased <- validateYesNoQuestion(isLeased, "isLeased", memberFullNameDob, row)

      lessees = lesseePeople.zipWithIndex.map {
        case (p, i) =>
          validateLeasePerson(p._1, p._2, p._3, p._4, memberFullNameDob, row, isRequired = (i == 0))
      }

      lesseeDetails <- (
        validatedIsLeased,
        lessees
      ) match {
        case (Valid(isLeased), lessees) if isLeased.toUpperCase == "YES" =>
          (lessees.head, lessees.tail.sequence) match {
            case (Some(first), Some(others)) => {
              first match {
                case (e @ Invalid(_)) => Some(e)
                case _ => {
                  val oTraverse = others.traverse(_.toEither)
                  oTraverse match {
                    case Right(people) =>
                      Some(first.map { f =>
                        (Yes, Some(List(f.get) ++ people.flatten))
                      })
                    case Left(error) => Some(Validated.invalid(error))
                  }
                }
              }
            }
            case _ => None
          }

        case (Valid(isLeased), _) if isLeased.toUpperCase == "NO" =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _) => Some(e)

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
          message = "landOrProperty.firstPurchaser.upload.error.required"
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
              s"landOrProperty.purchaserType",
              memberFullNameDob,
              row,
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
    purchasers: List[(CsvValue[Option[String]], CsvValue[Option[String]])],
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

      purchaserList = purchasers.zipWithIndex.map {
        case (p, i) =>
          validatePurchaser(p._1, p._2, memberFullNameDob, row, isRequired = (i == 0))
      }

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
        purchaserList,
        maybeIsTransactionSupportedByIndependentValuation,
        maybeHasLandOrPropertyFullyDisposedOf
      ) match {
        case (Valid(isLeased), mAmount, mPurchasers, mIndependent, mFully) if isLeased.toUpperCase == "YES" =>
          (mAmount, mPurchasers.head, mPurchasers.tail.sequence, mIndependent, mFully) match {
            case (mAmount, mFirst, mOthers, mDepend, mFully) =>
              (mAmount, mFirst, mOthers, mDepend, mFully) match {
                case (Some(amount), Some(first), Some(others), Some(depend), Some(fully)) => {
                  (amount, first, others, depend, fully) match {
                    case (e @ Invalid(_), _, _, _, _) => Some(e)
                    case (_, e @ Invalid(_), _, _, _) => Some(e)
                    case (_, _, _, e @ Invalid(_), _) => Some(e)
                    case (_, _, _, _, e @ Invalid(_)) => Some(e)
                    case _ => {
                      val oTraverse = others.traverse(_.toEither)
                      oTraverse match {
                        case Left(error) => Some(Validated.invalid(error))
                        case Right(people) =>
                          Some(
                            (amount, first, depend, fully).mapN { (_amount, _first, _depend, _fully) =>
                              (
                                Yes,
                                Some(
                                  DispossalDetail(
                                    disposedPropertyProceedsAmt = _amount.value,
                                    independentValutionDisposal = YesNo.uploadYesNoToRequestYesNo(_depend),
                                    propertyFullyDisposed = YesNo.uploadYesNoToRequestYesNo(_fully),
                                    purchaserDetails = List(_first.get) ++ people.flatten
                                  )
                                )
                              )
                            }
                          )
                      }
                    }
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
                  else if (mFully.isEmpty)
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

        case (Valid(isLeased), _, _, _, _) if isLeased.toUpperCase == "NO" =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _, _, _, _) => Some(e)

        case _ => None
      }
    } yield disposalDetails
}
