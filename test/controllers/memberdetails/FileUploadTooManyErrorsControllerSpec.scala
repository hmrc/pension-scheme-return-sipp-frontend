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

package controllers.memberdetails

import controllers.ControllerBaseSpec
import controllers.memberdetails.FileUploadTooManyErrorsController.viewModel
import models.Upload
import org.mockito.ArgumentMatchers.any
import play.api.inject
import play.api.inject.guice.GuiceableModule
import services.UploadService
import views.html.ContentPageView

import scala.concurrent.Future

class FileUploadTooManyErrorsControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.FileUploadTooManyErrorsController.onPageLoad(srn)
  private lazy val onSubmit = routes.FileUploadTooManyErrorsController.onSubmit(srn)

  private val mockUploadService = mock[UploadService]

  override val additionalBindings: List[GuiceableModule] = List(
    inject.bind[UploadService].toInstance(mockUploadService)
  )

  override def beforeEach(): Unit =
    reset(mockUploadService)

  "FileUploadTooManyErrorsController" - {
    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[ContentPageView].apply(viewModel(srn))
    }.before({
      mockGetUploadStatus(Some(uploadResultErrors))
    }))

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  private def mockGetUploadStatus(upload: Option[Upload]): Unit =
    when(mockUploadService.getUploadResult(any())).thenReturn(Future.successful(upload))
}
