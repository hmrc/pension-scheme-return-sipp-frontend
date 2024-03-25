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

package controllers.tangiblemoveableproperty

import akka.stream.Materializer
import akka.stream.alpakka.csv.scaladsl.CsvFormatting
import akka.stream.scaladsl.{FileIO, Keep, Source}
import cats.data.NonEmptyList
import controllers.actions.IdentifyAndRequireData
import models.SchemeId.Srn
import models.{HeaderKeys, Journey, UploadErrorsTangibleMoveableProperty, UploadFormatError, UploadKey}
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.Files.TemporaryFileCreator
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.UploadService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DownloadTangibleMoveableErrorsController @Inject()(
  uploadService: UploadService,
  identifyAndRequireData: IdentifyAndRequireData
)(cc: ControllerComponents)(
  implicit ec: ExecutionContext,
  temporaryFileCreator: TemporaryFileCreator,
  mat: Materializer
) extends AbstractController(cc)
    with I18nSupport {

  def downloadFile(srn: Srn, journey: Journey): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      uploadService
        .getValidatedUpload(UploadKey.fromRequest(srn, journey.uploadRedirectTag))
        .flatMap {
          case Some(UploadErrorsTangibleMoveableProperty(unvalidated, errors)) =>
            val fileName = "output-tangible-moveable-property.csv"

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
                    raw.countOfTangiblePropertyTransactions.value,
                    raw.rawAsset.descriptionOfAsset.value,
                    raw.rawAsset.dateOfAcquisitionAsset.value,
                    raw.rawAsset.totalCostAsset.value,
                    raw.rawAsset.rawAcquiredFromType.whoAcquiredFromName.value,
                    raw.rawAsset.rawAcquiredFromType.acquiredFromType.value,
                    raw.rawAsset.rawAcquiredFromType.acquirerNinoForIndividual.value.getOrElse(""),
                    raw.rawAsset.rawAcquiredFromType.acquirerCrnForCompany.value.getOrElse(""),
                    raw.rawAsset.rawAcquiredFromType.acquirerUtrForPartnership.value.getOrElse(""),
                    raw.rawAsset.rawAcquiredFromType.whoAcquiredFromTypeReasonAsset.value.getOrElse(""),
                    raw.rawAsset.isTxSupportedByIndependentValuation.value,
                    raw.rawAsset.totalAmountIncomeReceiptsTaxYear.value,
                    raw.rawAsset.isTotalCostValueOrMarketValue.value,
                    raw.rawAsset.totalCostValueTaxYearAsset.value,
                    raw.rawAsset.rawDisposal.wereAnyDisposalOnThisDuringTheYear.value,
                    raw.rawAsset.rawDisposal.totalConsiderationAmountSaleIfAnyDisposal.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.wereAnyDisposals.value,
                    raw.rawAsset.rawDisposal.first.name.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.first.connection.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.second.name.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.second.connection.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.third.name.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.third.connection.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.fourth.name.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.fourth.connection.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.fifth.name.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.fifth.connection.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.sixth.name.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.sixth.connection.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.seventh.name.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.seventh.connection.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.eighth.name.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.eighth.connection.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.ninth.name.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.ninth.connection.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.tenth.name.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.tenth.connection.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.isTransactionSupportedByIndependentValuation.value.getOrElse(""),
                    raw.rawAsset.rawDisposal.isAnyPartAssetStillHeld.value.getOrElse(""),
                    groupedErr.get(raw.row).map(_.map(m => Messages(m.message)).toList.mkString(", ")).getOrElse("")
                  )
              )

            val headers = HeaderKeys.headersForTangibleMoveableProperty

            val write = Source(
              List(headers.split(";\n").toList) ++
                List(HeaderKeys.questionHelpersMoveableProperty.split(";\n").toList) ++
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
