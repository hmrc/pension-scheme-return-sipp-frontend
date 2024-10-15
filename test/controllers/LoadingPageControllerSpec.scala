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

import controllers.LoadingPageController.viewModelForUploading
import models.FileAction.Validating
import models.{Journey, JourneyType}
import play.api.inject
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

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPendingFileActionService, mockTaxYearService)
    when(mockTaxYearService.current).thenReturn(TaxYear(2023))
  }

  "LoadingPageController" - {
    "onPageLoad for Validating action" - {
      lazy val application =
        applicationBuilder(Some(emptyUserAnswers))
          .overrides(
            inject.bind[PendingFileActionService].toInstance(mockPendingFileActionService),
            inject.bind[TaxYearService].toInstance(mockTaxYearService)
          )
          .build()

      val fileAction = Validating
      val onPageLoad = routes.LoadingPageController.onPageLoad(srn, fileAction, journey, journeyType)

      "redirect to the provided URL when state is Complete for Validating action" in {

        val redirectUrl = "/next-page"
        val sessionParams = Map("param1" -> "value1", "param2" -> "value2")

        when(mockPendingFileActionService.getValidationState(any, any, any)(any, any))
          .thenReturn(Future.successful(Complete(redirectUrl, sessionParams)))

        act.like(
          renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
            val view = app.injector.instanceOf[LoadingPageView]
            val viewModel = viewModelForUploading(journey)
            view(viewModel)
          }.before {
            when(mockPendingFileActionService.getUploadState(eqTo(srn), eqTo(journey), eqTo(journeyType))(any))
              .thenReturn(Future.successful(Pending))
          }.updateName("must render the view when state is Pending for Uploading action" + _)
        )
      }

      act.like(
        journeyRecoveryPage(
          routes.LoadingPageController.onPageLoad(srn, Validating, journey, journeyType)
        ).updateName("onPageLoad must redirect to Journey Recovery when no existing data is found" + _)
      )
    }
  }
}
