/*
 * Copyright 2024 HM Revenue & Customs
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

import models.{Journey, JourneyType, UpscanFileReference, UpscanInitiateResponse}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.UploadService
import views.html.UploadView

import scala.concurrent.Future

class UploadFileControllerSpec extends ControllerBaseSpec {

  private val journey = Journey.InterestInLandOrProperty
  private val journeyType = JourneyType.Standard

  private val fileReference = UpscanFileReference("test-file-reference")
  private val postTarget = "test-post-target"
  private val formFields = Map("test" -> "fields")
  private val initiateResponse = UpscanInitiateResponse(fileReference, postTarget, formFields)

  private val mockUploadService = mock[UploadService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[UploadService].toInstance(mockUploadService)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUploadService)
  }

  "UploadFileController" - {

    def onPageLoad = routes.UploadFileController.onPageLoad(srn, journey, journeyType)

    act.like(
      renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
        val view = app.injector.instanceOf[UploadView]
        val viewModel = UploadFileController.viewModel(
          journey,
          journeyType,
          postTarget,
          formFields,
          None
        )
        view(viewModel)
      }.before {
        when(mockUploadService.initiateUpscan(any, any, any)(any))
          .thenReturn(Future.successful(initiateResponse))

        when(mockUploadService.registerUploadRequest(any, any))
          .thenReturn(Future.successful(()))
      }.updateName("onPageLoad must render the view correctly when called" + _)
    )

    act.like(
      journeyRecoveryPage(onPageLoad).updateName(
        "onPageLoad must redirect to Journey Recovery when no existing data is found" + _
      )
    )

    act.like(
      journeyRecoveryPage(onPageLoad)
        .before {
          when(mockUploadService.initiateUpscan(any, any, any)(any))
            .thenReturn(Future.failed(Exception("Upload initiation failed")))
        }
        .updateName("onPageLoad must handle errors from the upload service" + _)
    )
  }
}
