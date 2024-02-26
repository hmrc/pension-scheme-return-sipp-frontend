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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import forms.YesNoPageFormProvider
import models.Journey.MemberDetails
import models.UploadStatus.UploadStatus
import models._
import org.mockito.ArgumentMatchers.any
import pages.CheckFileNamePage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.{AuditService, SchemeDateService, UploadService}
import views.html.YesNoPageView
import CheckFileNameController._

import scala.concurrent.Future

class CheckFileNameControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.CheckFileNameController.onPageLoad(srn, MemberDetails, NormalMode)
  private lazy val onSubmit = routes.CheckFileNameController.onSubmit(srn, MemberDetails, NormalMode)

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
    mockSeUploadedStatus()
  }

  "CheckFileNameController" - {

    act.like(
      renderView(onPageLoad) { implicit app => implicit request =>
        injected[YesNoPageView].apply(
          form(injected[YesNoPageFormProvider], MemberDetails),
          viewModel(srn, MemberDetails, Some(fileName), NormalMode)
        )
      }.before({
        mockGetUploadStatus(Some(uploadedSuccessfully))
      })
    )

    act.like(renderPrePopView(onPageLoad, CheckFileNamePage(srn, MemberDetails), true) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider], MemberDetails).fill(true),
            viewModel(srn, MemberDetails, Some(fileName), NormalMode)
          )
    }.before({
      mockGetUploadStatus(Some(uploadedSuccessfully))
    }))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      saveAndContinue(onSubmit, "value" -> "true")
        .before({
          mockGetUploadStatus(Some(uploadedSuccessfully))
        })
    )

    act.like(invalidForm(onSubmit, "invalid" -> "form").before(mockGetUploadStatus(Some(uploadedSuccessfully))))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  private def mockGetUploadStatus(uploadStatus: Option[UploadStatus]): Unit =
    when(mockUploadService.getUploadStatus(any())).thenReturn(Future.successful(uploadStatus))

  private def mockStream(): Unit =
    when(mockUploadService.stream(any())(any())).thenReturn(Future.successful((200, Source.single(byteString))))

  private def mockSaveValidatedUpload(): Unit =
    when(mockUploadService.saveValidatedUpload(any(), any())).thenReturn(Future.successful(()))

  private def mockSeUploadedStatus(): Unit =
    when(mockUploadService.setUploadedStatus(any())).thenReturn(Future.successful(()))

}
