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
import models.Journey.{
  ArmsLengthLandOrProperty,
  AssetFromConnectedParty,
  InterestInLandOrProperty,
  OutstandingLoans,
  TangibleMoveableProperty,
  UnquotedShares
}
import models.{Journey, JourneyType, UploadState}
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

  "FileUploadTooManyErrorsController - InterestInLandOrProperty" - {
    new TestScope {
      override val journey: Journey = InterestInLandOrProperty
    }
  }

  "FileUploadTooManyErrorsController - ArmsLengthLandOrProperty" - {
    new TestScope {
      override val journey: Journey = ArmsLengthLandOrProperty
    }
  }

  "FileUploadTooManyErrorsController - TangibleMoveableProperty" - {
    new TestScope {
      override val journey: Journey = TangibleMoveableProperty
    }
  }

  "FileUploadTooManyErrorsController - OutstandingLoans" - {
    new TestScope {
      override val journey: Journey = OutstandingLoans
    }
  }

  "FileUploadTooManyErrorsController - UnquotedShares" - {
    new TestScope {
      override val journey: Journey = UnquotedShares
    }
  }

  "FileUploadTooManyErrorsController - AssetFromConnectedParty" - {
    new TestScope {
      override val journey: Journey = AssetFromConnectedParty
    }
  }

  private def mockGetUploadStatus(upload: Option[UploadState]): Unit = {
    when(mockUploadService.getUploadValidationState(any())).thenReturn(Future.successful(upload))
    when(mockUploadService.getUploadStatus(any())).thenReturn(Future.successful(None))
  }

  trait TestScope {
    val journey: Journey
    val journeyType: JourneyType = JourneyType.Standard

    private lazy val onPageLoad = routes.FileUploadTooManyErrorsController.onPageLoad(srn, journey, journeyType)
    private lazy val onSubmit = routes.FileUploadTooManyErrorsController.onSubmit(srn, journey, journeyType)

    act.like(
      renderView(onPageLoad) { implicit app => implicit request =>
        injected[ContentPageView].apply(viewModel(srn, journey, journeyType))
      }.before(
        mockGetUploadStatus(Some(uploadResultErrors))
      )
    )

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
