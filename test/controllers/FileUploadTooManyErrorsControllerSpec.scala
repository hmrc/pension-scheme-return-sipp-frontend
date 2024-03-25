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

import controllers.FileUploadTooManyErrorsController.viewModel
import models.Journey.{InterestInLandOrProperty, MemberDetails}
import models.{Journey, Upload}
import org.mockito.ArgumentMatchers.any
import play.api.inject
import play.api.inject.guice.GuiceableModule
import services.{AuditService, UploadService}
import views.html.ContentPageView

import scala.concurrent.Future

class FileUploadTooManyErrorsControllerSpec extends ControllerBaseSpec {

  private val mockUploadService = mock[UploadService]
  private val mockAuditService = mock[AuditService]

  override val additionalBindings: List[GuiceableModule] = List(
    inject.bind[UploadService].toInstance(mockUploadService),
    inject.bind[AuditService].toInstance(mockAuditService)
  )

  override def beforeEach(): Unit =
    reset(mockUploadService)

  "FileUploadTooManyErrorsController - MemberDetails" - {
    new TestScope {
      override val journey: Journey = MemberDetails
    }
  }

  "FileUploadTooManyErrorsController - InterestInLandOrProperty" - {
    new TestScope {
      override val journey: Journey = InterestInLandOrProperty
    }
  }

  private def mockGetUploadStatus(upload: Option[Upload]): Unit = {
    when(mockUploadService.getValidatedUpload(any())).thenReturn(Future.successful(upload))
    when(mockUploadService.getUploadStatus(any())).thenReturn(Future.successful(None))
  }

  trait TestScope {
    val journey: Journey

    private lazy val onPageLoad = routes.FileUploadTooManyErrorsController.onPageLoad(srn, journey)
    private lazy val onSubmit = routes.FileUploadTooManyErrorsController.onSubmit(srn, journey)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[ContentPageView].apply(viewModel(srn, journey))
    }.before({
      mockGetUploadStatus(Some(uploadResultErrors))
    }))

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }

}
