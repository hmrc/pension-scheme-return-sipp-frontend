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

package controllers.landorproperty

import akka.stream.Materializer
import akka.stream.alpakka.csv.scaladsl.CsvFormatting
import akka.stream.scaladsl.{FileIO, Keep, Source}
import cats.data.NonEmptyList
import controllers.actions.IdentifyAndRequireData
import models.Journey.InterestInLandOrProperty
import models.SchemeId.Srn
import models.{HeaderKeys, Journey, UploadErrorsLandConnectedProperty, UploadFormatError, UploadKey}
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.Files.TemporaryFileCreator
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.UploadService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DownloadLandOrPropertyErrorsController @Inject()(
  uploadService: UploadService,
  identifyAndRequireData: IdentifyAndRequireData
)(cc: ControllerComponents)(
  implicit ec: ExecutionContext,
  temporaryFileCreator: TemporaryFileCreator,
  mat: Materializer
) extends AbstractController(cc)
    with I18nSupport {

  def downloadFile(srn: Srn, journey: Journey): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    uploadService
      .getValidatedUpload(UploadKey.fromRequest(srn, journey.uploadRedirectTag))
      .flatMap {
        case Some(UploadErrorsLandConnectedProperty(unvalidated, errors)) =>
          val fileName = if(journey == InterestInLandOrProperty) {
            "output-interest-land-or-property.csv"
          } else {
            "output-arms-length-land-or-property.csv"
          }
          val tempFile = temporaryFileCreator.create(suffix = fileName)
          val fileOutput = FileIO.toPath(tempFile.path)
          val groupedErr = errors.groupBy(_.row)

          val csvLines: NonEmptyList[List[String]] = unvalidated
            .map(
              raw =>
                List(
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
                  raw.rawDisposal.hasLandOrPropertyFullyDisposedOf.value.getOrElse(""),
                  groupedErr.get(raw.row).map(_.map(m => Messages(m.message)).toList.mkString(", ")).getOrElse("")
                )
            )

            val headers = if (journey == InterestInLandOrProperty)
                HeaderKeys.headersForInterestLandOrProperty
              else
                HeaderKeys.headersForArmsLength
            val questionHelpers = HeaderKeys.questionHelpers

            val write = Source(
              List(headers.split(";\n").toList) ++
                List(questionHelpers.split(";\n").toList) ++
                csvLines.toList
            ).via(CsvFormatting.format())
              .toMat(fileOutput)(Keep.right)

            write.run().map { _ =>
              Ok.sendFile(
                content = tempFile.toFile,
                fileName = _ => Option(fileName)
              )
            }

          case Some(UploadFormatError(_)) =>
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          case _ =>
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
  }
}
