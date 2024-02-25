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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import controllers.ControllerBaseSpec
import controllers.landorproperty.CheckInterestLandOrPropertyFileController.{form, viewModel}
import forms.YesNoPageFormProvider
import models.UploadStatus.UploadStatus
import models._
import org.mockito.ArgumentMatchers.any
import pages.landorproperty.CheckInterestLandOrPropertyFilePage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.{AuditService, SchemeDateService, UploadService}
import views.html.YesNoPageView

import scala.concurrent.Future

class CheckInterestLandOrPropertyFileControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.CheckInterestLandOrPropertyFileController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.CheckInterestLandOrPropertyFileController.onSubmit(srn, NormalMode)

  private val fileName = "test-file-name"
  private val byteString = ByteString("test-content")

  private val uploadedSuccessfully = UploadStatus.Success(
    fileName,
    "text/csv",
    "/test-download-url",
    Some(123L)
  )

  private val mockUploadService = mock[UploadService]
  private val mockSchemeDateService = mock[SchemeDateService]
  private val mockAuditService = mock[AuditService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[UploadService].toInstance(mockUploadService),
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[AuditService].toInstance(mockAuditService)
  )

  override def beforeEach(): Unit = {
    reset(mockUploadService)
    reset(mockSchemeDateService)
    reset(mockAuditService)
    mockStream()
    mockSaveValidatedUpload()
  }

  "CheckInterestLandOrPropertyFileController" - {

    act.like(
      renderView(onPageLoad) { implicit app => implicit request =>
        injected[YesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel(srn, Some(fileName), NormalMode))
      }.before({
        mockGetUploadStatus(Some(uploadedSuccessfully))
      })
    )

    act.like(renderPrePopView(onPageLoad, CheckInterestLandOrPropertyFilePage(srn), true) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(form(injected[YesNoPageFormProvider]).fill(true), viewModel(srn, Some(fileName), NormalMode))
    }.before({
      mockGetUploadStatus(Some(uploadedSuccessfully))
    }))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit, "invalid" -> "form").before(mockGetUploadStatus(Some(uploadedSuccessfully))))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  private def mockGetUploadStatus(uploadStatus: Option[UploadStatus]): Unit =
    when(mockUploadService.getUploadStatus(any())).thenReturn(Future.successful(uploadStatus))

  private def mockStream(): Unit =
    when(mockUploadService.stream(any())(any())).thenReturn(Future.successful((200, Source.single(byteString))))

  private def mockSaveValidatedUpload(): Unit =
    when(mockUploadService.saveValidatedUpload(any(), any())).thenReturn(Future.successful(()))

}
