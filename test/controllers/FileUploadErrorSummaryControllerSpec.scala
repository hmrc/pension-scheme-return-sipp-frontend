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

import cats.data.NonEmptyList
import controllers.FileUploadErrorSummaryController.{viewModelErrors, viewModelFormatting}
import models.UploadState.UploadValidated
import models.csv.CsvDocumentInvalid
import models.{Journey, JourneyType, UploadKey, UploadStatus, ValidationError, ValidationErrorType}
import navigation.Navigator
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Call
import services.{AuditService, TaxYearService, UploadService}
import uk.gov.hmrc.time.TaxYear
import views.html.ContentPageView

import scala.concurrent.Future

class FileUploadErrorSummaryControllerSpec extends ControllerBaseSpec with MockitoSugar {

  private val mockNavigator = mock[Navigator]
  private val mockUploadService = mock[UploadService]
  private val mockAuditService = mock[AuditService]
  private val mockTaxYearService = mock[TaxYearService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[UploadService].toInstance(mockUploadService),
    bind[AuditService].toInstance(mockAuditService),
    bind[TaxYearService].toInstance(mockTaxYearService)
  )

  private val journey = Journey.InterestInLandOrProperty
  private val journeyType = JourneyType.Amend

  private val sampleTaxYear = TaxYear(2023)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockNavigator, mockUploadService, mockAuditService, mockTaxYearService)
  }

  lazy val onPageLoad: Call = controllers.routes.FileUploadErrorSummaryController.onPageLoad(srn, journey, journeyType)

  "FileUploadErrorSummaryController" - {

    "onPageLoad" - {
      val validationError = ValidationError(
        row = 1,
        errorType = ValidationErrorType.InvalidRowFormat,
        message = "Invalid row format"
      )

      val validationError2 = ValidationError(
        row = 1,
        errorType = ValidationErrorType.DuplicateNino,
        message = "Missing field"
      )

      val validationError3 = ValidationError(
        row = 2,
        errorType = ValidationErrorType.MarketOrCostType,
        message = "Invalid value"
      )

      act.like(
        renderView(
          onPageLoad,
          defaultUserAnswers
        ) { implicit app => implicit request =>
          val view = app.injector.instanceOf[ContentPageView]
          val resultViewModel = viewModelFormatting(srn, journey, journeyType, validationError)
          view(resultViewModel)
        }.before {
          val uploadKey = UploadKey(userAnswersId, srn, journey.uploadRedirectTag)

          when(mockUploadService.getUploadValidationState(eqTo(uploadKey)))
            .thenReturn(
              Future.successful(Some(UploadValidated(CsvDocumentInvalid(1, NonEmptyList.of(validationError)))))
            )

          when(mockUploadService.getUploadStatus(eqTo(uploadKey)))
            .thenReturn(
              Future.successful(
                Some(
                  UploadStatus.Success(
                    name = "fileName.csv",
                    mimeType = "text/csv",
                    downloadUrl = "downloadUrl",
                    size = Some(1000L)
                  )
                )
              )
            )

          when(mockAuditService.sendEvent(any)(any, any))
            .thenReturn(Future.successful(()))

          when(mockTaxYearService.current).thenReturn(sampleTaxYear)
        }.updateName("when there is an InvalidRowFormat error" + _)
      )

      act.like(
        renderView(
          onPageLoad,
          defaultUserAnswers
        ) { implicit app => implicit request =>
          val errors = NonEmptyList.of(validationError2, validationError3)

          val view = app.injector.instanceOf[ContentPageView]
          val resultViewModel = viewModelErrors(srn, journey, journeyType, errors)
          view(resultViewModel)
        }.before {
          val uploadKey = UploadKey(userAnswersId, srn, journey.uploadRedirectTag)

          when(mockUploadService.getUploadValidationState(eqTo(uploadKey)))
            .thenReturn(
              Future.successful(
                Some(UploadValidated(CsvDocumentInvalid(1, NonEmptyList.of(validationError2, validationError3))))
              )
            )

          when(mockUploadService.getUploadStatus(eqTo(uploadKey)))
            .thenReturn(
              Future.successful(
                Some(
                  UploadStatus.Success(
                    name = "fileName.csv",
                    mimeType = "text/csv",
                    downloadUrl = "downloadUrl",
                    size = Some(1000L)
                  )
                )
              )
            )

          when(mockAuditService.sendEvent(any)(any, any))
            .thenReturn(Future.successful(()))

          when(mockTaxYearService.current).thenReturn(sampleTaxYear)
        }.updateName(
          "when there are errors but no InvalidRowFormat error" + _
        )
      )

      // TODO: Fix this test
      //      act.like(
//        journeyRecoveryPage(
//          onPageLoad
//        ).before {
//          when(mockUploadService.getUploadValidationState(any))
//            .thenReturn(Future.successful(None))
//        }.updateName("when validation state is not CsvDocumentInvalid" + _)
//      )

    }

    // TODO: Fix this test
//    "onSubmit" - {
//      act.like(
//        redirectNextPage(
//          controllers.routes.FileUploadErrorSummaryController.onSubmit(srn, journey, journeyType),
//          defaultUserAnswers
//        ).before {
////          when(mockNavigator.nextPage(eqTo(UploadErrorSummaryPage(srn, journey, journeyType)), eqTo(mode), any)(any))
////            .thenReturn(dummyCall)
//        }
//      )
//    }

  }
}
