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

import controllers.LoadingPageController.viewModelForValidating
import models.FileAction.{Uploading, Validating}
import models.{FileAction, Journey, JourneyType}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Call
import services.PendingFileActionService.{Complete, Pending}
import services.{PendingFileActionService, TaxYearService}
import uk.gov.hmrc.time.TaxYear
import views.html.LoadingPageView

import scala.concurrent.Future

class LoadingPageControllerSpec extends ControllerBaseSpec {

  private val journey = Journey.InterestInLandOrProperty
  private val journeyType = JourneyType.Standard

  private val mockPendingFileActionService = mock[PendingFileActionService]
  private val mockTaxYearService = mock[TaxYearService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[PendingFileActionService].toInstance(mockPendingFileActionService),
    bind[TaxYearService].toInstance(mockTaxYearService)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPendingFileActionService, mockTaxYearService)
    when(mockTaxYearService.current).thenReturn(TaxYear(2023))
  }

  def onPageLoad(action: FileAction): Call = routes.LoadingPageController.onPageLoad(srn, action, journey, journeyType)
  def redirectPage: Call = routes.WhatYouWillNeedController.onPageLoad(srn)

  "LoadingPageController" - {
    act.like(
      renderView(onPageLoad(Validating), defaultUserAnswers) { implicit app => implicit request =>
        val view = app.injector.instanceOf[LoadingPageView]
        val viewModel = viewModelForValidating(journey)
        view(viewModel)
      }.before {
        when(mockPendingFileActionService.getValidationState(any, any, any)(using any, any))
          .thenReturn(Future.successful(Pending))
      }.updateName("render the view when state is Pending for Validating action" + _)
    )

    act.like(
      renderView(onPageLoad(Uploading), defaultUserAnswers) { implicit app => implicit request =>
        val view = app.injector.instanceOf[LoadingPageView]
        val viewModel = LoadingPageController.viewModelForUploading(journey)
        view(viewModel)
      }.before {
        when(mockPendingFileActionService.getUploadState(any, any, any)(using any))
          .thenReturn(Future.successful(Pending))
      }.updateName("render the view when state is Pending for Uploading action" + _)
    )

    act.like(
      redirectToPage(onPageLoad(Uploading), redirectPage, defaultUserAnswers, Nil)
        .before {
          when(mockPendingFileActionService.getUploadState(any, any, any)(using any))
            .thenReturn(Future.successful(Complete(redirectPage.url, Map("key" -> "value"))))
        }
        .updateName("redirect to the URL when state is Complete for Uploading action" + _)
    )

    act.like(
      redirectToPage(onPageLoad(Validating), redirectPage, defaultUserAnswers, Nil)
        .before {
          when(mockPendingFileActionService.getValidationState(any, any, any)(using any, any))
            .thenReturn(Future.successful(Complete(redirectPage.url, Map("key" -> "value"))))
        }
        .updateName("redirect to the URL when state is Complete for Validating action" + _)
    )

    act.like(
      journeyRecoveryPage(
        onPageLoad(Validating)
      ).updateName("redirect to Journey Recovery when no existing data is found" + _)
    )
  }

}
