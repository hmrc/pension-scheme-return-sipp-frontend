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
import cats.data.{NonEmptyList, Validated, ValidatedNel}
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

  private def acquiredFromTypeForm(memberFullDetails: String, key: String): Form[String] =
    textFormProvider.acquiredFromType(
      s"$key.upload.error.required",
      s"$key.upload.error.invalid",
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
        "interestInLandOrProperty.isThereARegistryReference",
        memberFullNameDob,
        row
      )
      maybeNoLandRegistryReference = noLandRegistryReference.value.flatMap(
        reason =>
          validateFreeText(
            noLandRegistryReference.as(reason),
            "interestInLandOrProperty.noLandRegistryReference",
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
                  row,
                  errorType = ValidationErrorType.FreeText,
                  "interestInLandOrProperty.noLandRegistryReference.upload.error.required"
                ).invalidNel
              )
          }
        case (e @ Invalid(_), _) => Some(e)
        case _ => None
      }
    } yield referenceDetails

  def validateAcquiredFromType(
    acquiredFromType: CsvValue[String],
    memberFullName: String,
    row: Int,
    key: String
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = acquiredFromTypeForm(memberFullName, key)
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
      validatedAcquiredFromType <- validateAcquiredFromType(
        acquiredFromType,
        memberFullNameDob,
        row,
        "interestInLandOrProperty.acquiredFromType"
      )

      maybeNino = acquirerNinoForIndividual.value.flatMap(
        nino =>
          validateNino(
            acquirerNinoForIndividual.as(nino),
            memberFullNameDob,
            row,
            "interestInLandOrProperty.acquirerNino"
          )
      )
      maybeCrn = acquirerCrnForCompany.value.flatMap(
        crn =>
          validateCrn(acquirerCrnForCompany.as(crn), memberFullNameDob, row, "interestInLandOrProperty.acquirerCrn")
      )
      maybeUtr = acquirerUtrForPartnership.value.flatMap(
        utr =>
          validateUtr(acquirerUtrForPartnership.as(utr), memberFullNameDob, row, "interestInLandOrProperty.acquirerUtr")
      )
      maybeOther = noIdOrAcquiredFromAnotherSource.value.flatMap(
        other =>
          validateFreeText(
            noIdOrAcquiredFromAnotherSource.as(other),
            "interestInLandOrProperty.noIdOrAcquiredFromAnother",
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

            case _ =>
              Some(
                ValidationError(
                  row,
                  ValidationErrorType.FreeText,
                  message = "interestInLandOrProperty.noIdOrAcquiredFromAnother.upload.error.required"
                ).invalidNel
              )
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

            case _ =>
              Some(
                ValidationError(
                  row,
                  ValidationErrorType.FreeText,
                  message = "interestInLandOrProperty.noIdOrAcquiredFromAnother.upload.error.required"
                ).invalidNel
              )
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

            case _ =>
              Some(
                ValidationError(
                  row,
                  ValidationErrorType.FreeText,
                  message = "interestInLandOrProperty.noIdOrAcquiredFromAnother.upload.error.required"
                ).invalidNel
              )
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

            case _ =>
              Some(
                ValidationError(
                  row,
                  ValidationErrorType.FreeText,
                  message = "interestInLandOrProperty.noIdOrAcquiredFromAnother.upload.error.required"
                ).invalidNel
              )
          }

        case (e @ Invalid(_), _, _, _, _) => Some(e)

        case _ => None
      }
    } yield validatedAcquiredFrom

  def validateJointlyHeld(
    count: Int,
    nameJointlyOwning: CsvValue[Option[String]],
    ninoJointlyOwning: CsvValue[Option[String]],
    noNinoJointlyOwning: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int,
    isRequired: Boolean = false
  ): Option[ValidatedNel[ValidationError, Option[JointPropertyDetail]]] =
    if (isRequired && nameJointlyOwning.value.isEmpty) {
      Some(
        ValidationError(
          row,
          ValidationErrorType.FreeText,
          message = "interestInLandOrProperty.jointlyName.upload.error.required"
        ).invalidNel
      )
    } else if (nameJointlyOwning.value.isEmpty) {
      Some(None.validNel)
    } else {
      for {
        name <- validateFreeText(
          nameJointlyOwning.as(nameJointlyOwning.value.get),
          s"interestInLandOrProperty.jointlyName.$count",
          memberFullNameDob,
          row
        )
        maybeNino = ninoJointlyOwning.value.flatMap(
          nino =>
            validateNino(
              ninoJointlyOwning.as(nino),
              memberFullNameDob,
              row,
              s"interestInLandOrProperty.jointlyNino.$count"
            )
        )
        maybeNoNino = noNinoJointlyOwning.value.flatMap(
          other =>
            validateFreeText(
              noNinoJointlyOwning.as(other),
              s"interestInLandOrProperty.jointlyNoNino.$count",
              memberFullNameDob,
              row
            )
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
              case _ =>
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.YesNoQuestion,
                    s"interestInLandOrProperty.jointlyNoNino.$count.upload.error.required"
                  ).invalidNel
                )
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
        "interestInLandOrProperty.isPropertyHeldJointly",
        memberFullNameDob,
        row
      )

      maybeCount = howManyPersonsJointlyOwnProperty.value.flatMap(
        count =>
          validateCount(
            howManyPersonsJointlyOwnProperty.as(count),
            "interestInLandOrProperty.personCount",
            memberFullNameDob,
            row,
            maxCount = 5
          )
      )

      jointlyHeldPeople = jointlyPersonList.zipWithIndex.map {
        case (p, i) =>
          validateJointlyHeld(i + 1, p._1, p._2, p._3, memberFullNameDob, row, isRequired = (i == 0))
      }

      jointlyHeld <- (
        validatedIsPropertyHeldJointly,
        maybeCount,
        jointlyHeldPeople
      ) match {
        case (Valid(isPropertyHeldJointly), mCount, jointlyHeldPeople) if isPropertyHeldJointly.toUpperCase == "YES" =>
          (mCount, jointlyHeldPeople.sequence) match {
            case (Some(count), Some(people)) =>
              (count) match {
                case (e @ Invalid(_)) => Some(e)
                case _ =>
                  people.sequence match {
                    case Invalid(errorList) =>
                      Some(Validated.invalid(errorList))
                    case Valid(details) =>
                      Some((count).map { c =>
                        (Yes, Some(c), Some(details.flatten))
                      })
                  }
              }
            case _ =>
              if (mCount.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.YesNoQuestion,
                    "interestInLandOrProperty.personCount.upload.error.required"
                  ).invalidNel
                )
              } else if (jointlyHeldPeople.isEmpty || jointlyHeldPeople.head.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.FreeText,
                    "interestInLandOrProperty.firstJointlyPerson.upload.error.required"
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
    count: Int,
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
          row,
          ValidationErrorType.FreeText,
          message = s"interestInLandOrProperty.firstLessee.upload.error.required"
        ).invalidNel
      )
    } else if (lesseeName.value.isEmpty) {
      Some(None.validNel)
    } else {
      for {
        name <- validateFreeText(
          lesseeName.as(lesseeName.value.get),
          s"interestInLandOrProperty.lesseeName.$count",
          memberFullNameDob,
          row
        )
        maybeConnectedOrUnconnected = lesseeConnectedOrUnconnected.value.flatMap(
          c =>
            validateConnectedOrUnconnected(
              lesseeConnectedOrUnconnected.as(c),
              s"interestInLandOrProperty.lesseeType.$count",
              memberFullNameDob,
              row
            )
        )
        maybeLesseeGrantedDate = lesseeGrantedDate.value.flatMap(
          date =>
            validateDate(lesseeGrantedDate.as(date), s"interestInLandOrProperty.lesseeGrantedDate.$count", row, None)
        )
        maybeLesseeAnnualAmount = lesseeAnnualAmount.value.flatMap(
          p =>
            validatePrice(
              lesseeAnnualAmount.as(p),
              s"interestInLandOrProperty.lesseeAnnualAmount.$count",
              memberFullNameDob,
              row
            )
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
                  case _ =>
                    Some((con, date, amount).mapN { (_count, _date, _amount) =>
                      val cType = ConnectedOrUnconnectedType.uploadStringToRequestConnectedOrUnconnected(_count)
                      Some(LesseeDetail(name, cType, _date, _amount.value))
                    })
                }
              }
              case _ =>
                val listEmpty = List.empty[Option[ValidationError]]
                val conditionError = if (mCon.isEmpty) {
                  Some(
                    ValidationError(
                      row,
                      errorType = ValidationErrorType.Price,
                      s"interestInLandOrProperty.lesseeType.$count.upload.error.required"
                    )
                  )
                } else {
                  None
                }
                val dateError = if (mDate.isEmpty) {
                  Some(
                    ValidationError(
                      row,
                      errorType = ValidationErrorType.YesNoQuestion,
                      s"interestInLandOrProperty.lesseeGrantedDate.$count.upload.error.required"
                    )
                  )
                } else {
                  None
                }
                val amountError = if (mAmount.isEmpty) {
                  Some(
                    ValidationError(
                      row,
                      errorType = ValidationErrorType.YesNoQuestion,
                      s"interestInLandOrProperty.lesseeAnnualAmount.$count.upload.error.required"
                    )
                  )
                } else {
                  None
                }
                val errors = listEmpty :+ conditionError :+ amountError :+ dateError
                Some(Invalid(NonEmptyList.fromListUnsafe(errors.flatten)))
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
      validatedIsLeased <- validateYesNoQuestion(isLeased, "interestInLandOrProperty.isLeased", memberFullNameDob, row)

      lessees = lesseePeople.zipWithIndex.map {
        case (p, i) =>
          validateLeasePerson(i + 1, p._1, p._2, p._3, p._4, memberFullNameDob, row, isRequired = (i == 0))
      }

      lesseeDetails <- (
        validatedIsLeased,
        lessees
      ) match {
        case (Valid(isLeased), lesseePeople) if isLeased.toUpperCase == "YES" =>
          (lesseePeople.sequence) match {
            case Some(people) => {
              people.sequence match {
                case Invalid(errorList) =>
                  Some(Validated.invalid(errorList))
                case Valid(details) =>
                  Some((Yes, Some(details.flatten)).validNel)
              }
            }
            case _ =>
              None
          }
        case (Valid(isLeased), _) if isLeased.toUpperCase == "NO" =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _) => Some(e)

        case _ => None
      }
    } yield lesseeDetails

  def validatePurchaser(
    count: Int,
    purchaserName: CsvValue[Option[String]],
    purchaserConnectedOrUnconnected: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int,
    isRequired: Boolean = false
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, Option[DispossalDetail.PurchaserDetail]]] =
    if (isRequired && purchaserName.value.isEmpty) {
      Some(
        ValidationError(
          row,
          ValidationErrorType.FreeText,
          message = "interestInLandOrProperty.firstPurchaser.upload.error.required"
        ).invalidNel
      )
    } else if (purchaserName.value.isEmpty) {
      Some(None.validNel)
    } else {
      for {
        name <- validateFreeText(
          purchaserName.as(purchaserName.value.get),
          s"interestInLandOrProperty.purchaserName.${count}",
          memberFullNameDob,
          row
        )
        maybeConnectedOrUnconnected = purchaserConnectedOrUnconnected.value.flatMap(
          c =>
            validateConnectedOrUnconnected(
              purchaserConnectedOrUnconnected.as(c),
              s"interestInLandOrProperty.purchaserType.$count",
              memberFullNameDob,
              row
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
              case _ =>
                Some(
                  ValidationError(
                    row,
                    ValidationErrorType.FreeText,
                    message = s"interestInLandOrProperty.purchaserType.$count.upload.error.required"
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
        "interestInLandOrProperty.isAnyDisposalMade",
        memberFullNameDob,
        row
      )

      maybeDisposalAmount = totalSaleProceedIfAnyDisposal.value.flatMap(
        p =>
          validatePrice(
            totalSaleProceedIfAnyDisposal.as(p),
            s"interestInLandOrProperty.disposedAmount",
            memberFullNameDob,
            row
          )
      )

      purchaserList = purchasers.zipWithIndex.map {
        case (p, i) =>
          validatePurchaser(i + 1, p._1, p._2, memberFullNameDob, row, isRequired = (i == 0))
      }

      maybeIsTransactionSupportedByIndependentValuation = isTransactionSupportedByIndependentValuation.value.flatMap(
        p =>
          validateYesNoQuestion(
            totalSaleProceedIfAnyDisposal.as(p),
            s"interestInLandOrProperty.isTransactionSupported",
            memberFullNameDob,
            row
          )
      )

      maybeHasLandOrPropertyFullyDisposedOf = hasLandOrPropertyFullyDisposedOf.value.flatMap(
        p =>
          validateYesNoQuestion(
            hasLandOrPropertyFullyDisposedOf.as(p),
            s"interestInLandOrProperty.isFullyDisposedOf",
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
          (mAmount, mPurchasers.sequence, mIndependent, mFully) match {
            case (mAmount, mPeople, mDepend, mFully) =>
              (mAmount, mPeople, mDepend, mFully) match {
                case (Some(amount), Some(people), Some(depend), Some(fully)) => {
                  people.sequence match {
                    case Invalid(errorList) =>
                      Some(Validated.invalid(errorList))
                    case Valid(details) =>
                      Some((amount, depend, fully).mapN { (_amount, _depend, _fully) =>
                        (
                          Yes,
                          Some(
                            DispossalDetail(
                              disposedPropertyProceedsAmt = _amount.value,
                              independentValutionDisposal = YesNo.uploadYesNoToRequestYesNo(_depend),
                              propertyFullyDisposed = YesNo.uploadYesNoToRequestYesNo(_fully),
                              purchaserDetails = details.flatten
                            )
                          )
                        )
                      })
                  }
                }
                case _ =>
                  val listEmpty = List.empty[Option[ValidationError]]
                  val purchaserErrors = mPurchasers.sequence match {
                    case Some(people) =>
                      people.sequence match {
                        case Invalid(errorList) =>
                          errorList.toList
                        case _ =>
                          List.empty
                      }
                    case _ =>
                      List.empty
                  }
                  val optAmount = if (mAmount.isEmpty) {
                    Some(
                      ValidationError(
                        row,
                        errorType = ValidationErrorType.Price,
                        "interestInLandOrProperty.disposedAmount.upload.error.required"
                      )
                    )
                  } else {
                    None
                  }

                  val optDepend = if (mDepend.isEmpty) {
                    Some(
                      ValidationError(
                        row,
                        errorType = ValidationErrorType.YesNoQuestion,
                        "interestInLandOrProperty.isTransactionSupported.upload.error.required"
                      )
                    )
                  } else {
                    None
                  }

                  val optFullyDis = if (mFully.isEmpty) {
                    Some(
                      ValidationError(
                        row,
                        errorType = ValidationErrorType.YesNoQuestion,
                        "interestInLandOrProperty.isFullyDisposedOf.upload.error.required"
                      )
                    )
                  } else {
                    None
                  }
                  val errors = listEmpty :+ purchaserErrors :+ optAmount :+ optDepend :+ optFullyDis
                  Some(Invalid(NonEmptyList.fromListUnsafe(errors.flatten)))
              }
            case _ => None
          }

        case (Valid(isLeased), _, _, _, _) if isLeased.toUpperCase == "NO" =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _, _, _, _) => Some(e)

        case _ => None
      }
    } yield disposalDetails

  def validateDuplicatedNinoNumbers(
    ninoNumbers: List[CsvValue[Option[String]]],
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, Option[Any]]] = {
    def hasDuplicates(strings: List[String]): Boolean = strings.distinct.length != strings.length

    val allEnteredNinoNumbers = ninoNumbers.flatMap(_.value).map(_.trim.toUpperCase)
    val hasDupes = hasDuplicates(allEnteredNinoNumbers)

    if (hasDupes) {
      Some(
        ValidationError(
          row,
          errorType = ValidationErrorType.NinoFormat,
          "interestInLandOrProperty.ninoNumbers.upload.error.duplicated"
        ).invalidNel
      )
    } else {
      Some(None.validNel)
    }
  }

}
