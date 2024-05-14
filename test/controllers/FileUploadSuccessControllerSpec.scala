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

package controllers

import cats.data.NonEmptyList
import controllers.FileUploadSuccessController.viewModel
import models.Journey.InterestInLandOrProperty
import models.UploadStatus.UploadStatus
import models.csv.CsvDocumentInvalid
import models.{
  ErrorDetails,
  NormalMode,
  UploadState,
  UploadStatus,
  UploadValidated,
  ValidationError,
  ValidationErrorType
}
import org.mockito.ArgumentMatchers.any
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.{SaveService, UploadService}
import views.html.ContentPageView

import scala.concurrent.Future

class FileUploadSuccessControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.FileUploadSuccessController.onPageLoad(srn, InterestInLandOrProperty, NormalMode)
  private lazy val onSubmit = routes.FileUploadSuccessController.onSubmit(srn, InterestInLandOrProperty, NormalMode)

  private val mockUploadService = mock[UploadService]
  private val mockSaveService = mock[SaveService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[UploadService].toInstance(mockUploadService),
    bind[SaveService].toInstance(mockSaveService)
  )

  override def beforeEach(): Unit = {
    reset(mockUploadService)
    reset(mockSaveService)
    when(mockSaveService.save(any())(any(), any())).thenReturn(Future.successful(()))
  }

  "FileUploadSuccessController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[ContentPageView].apply(viewModel(srn, uploadFileName, InterestInLandOrProperty, NormalMode))
    }.before(mockGetUploadStatus(Some(uploadSuccessful))))

    act.like(
      journeyRecoveryPage(onPageLoad)
        .before(mockGetUploadStatus(Some(UploadStatus.InProgress)))
        .updateName("onPageLoad when upload status in progress" + _)
    )

    act.like(
      journeyRecoveryPage(onPageLoad)
        .before(mockGetUploadStatus(Some(UploadStatus.Failed(ErrorDetails("reason", "message")))))
        .updateName("onPageLoad when upload status failed" + _)
    )

    act.like(
      journeyRecoveryPage(onPageLoad)
        .before(mockGetUploadStatus(None))
        .updateName("onPageLoad when upload status doesn't exist" + _)
    )

//    act.like(redirectNextPage(onSubmit).before(mockGetUploadResult(Some(uploadResultSuccess))))

    act.like(
      journeyRecoveryPage(onSubmit)
        .before(
          mockGetUploadResult(
            Some(
              UploadValidated(
                CsvDocumentInvalid(
                  1,
                  NonEmptyList.one(
                    ValidationError(
                      0,
                      ValidationErrorType.Formatting,
                      "Invalid file format, please format file as per provided template"
                    )
                  )
                )
              )
            )
          )
        )
        .updateName("onSubmit when upload result has a format error" + _)
    )

    act.like(
      journeyRecoveryPage(onSubmit)
        .before(mockGetUploadResult(Some(uploadResultErrors)))
        .updateName("onSubmit when upload result has errors" + _)
    )

    act.like(
      journeyRecoveryPage(onSubmit)
        .before(mockGetUploadResult(None))
        .updateName("onSubmit when upload result doesn't exist" + _)
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  private def mockGetUploadStatus(uploadStatus: Option[UploadStatus]): Unit =
    when(mockUploadService.getUploadStatus(any())).thenReturn(Future.successful(uploadStatus))

  private def mockGetUploadResult(upload: Option[UploadState]): Unit =
    when(mockUploadService.getUploadValidationState(any())).thenReturn(Future.successful(upload))
}
