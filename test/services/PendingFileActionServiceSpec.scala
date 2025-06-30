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

package services

import cats.data.NonEmptyList
import config.Constants
import controllers.routes
import models.*
import models.Journey.InterestInLandOrProperty
import models.JourneyType.Standard
import models.UploadState.*
import models.csv.{CsvDocumentEmpty, CsvDocumentInvalid, CsvDocumentValid, CsvDocumentValidAndSaved}
import models.requests.DataRequest
import navigation.Navigator
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Messages
import play.api.mvc.*
import play.api.test.*
import services.PendingFileActionService.{Complete, Pending}
import services.validation.ValidateUploadService
import utils.BaseSpec

import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}

class PendingFileActionServiceSpec extends BaseSpec with MockitoSugar with ScalaCheckPropertyChecks {

  private val mockNavigator = mock[Navigator]
  private val mockUploadService = mock[UploadService]
  private val mockValidateUploadService = mock[ValidateUploadService]
  private val mockClock = mock[Clock]
  val defaultUserAnswers: UserAnswers = UserAnswers("id")

  private val service = PendingFileActionService(
    mockNavigator,
    mockUploadService,
    mockValidateUploadService,
    mockClock
  )(ExecutionContext.global)

  "getUploadState should return Complete with success URL when upload is successful" in {
    val journey = InterestInLandOrProperty
    val journeyType = Standard
    val srn: SchemeId.Srn = srnGen.sample.value
    val success = UploadStatus.Success("test-file.csv", "test-reference", "test", None)

    when(mockUploadService.getUploadStatus(any)).thenReturn(Future.successful(Some(success)))

    val result = service.getUploadState(srn, journey, journeyType)(
      DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, defaultUserAnswers)
    )

    result.futureValue mustEqual Complete(
      routes.CheckFileNameController.onPageLoad(srn, journey, journeyType, NormalMode).url
    )
  }

  "getValidationState should return Complete with error URL when CSV document is empty" in {
    val journey = InterestInLandOrProperty
    val journeyType = Standard
    val srn: SchemeId.Srn = srnGen.sample.value
    val csvDocumentState = CsvDocumentEmpty

    when(mockUploadService.getUploadValidationState(any))
      .thenReturn(Future.successful(Some(UploadValidated(csvDocumentState))))
    when(mockNavigator.nextPage(any, any, any)(using any)).thenReturn(Call("GET", "/error"))

    val result = service.getValidationState(srn, journey, journeyType)(
      DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, defaultUserAnswers),
      mock[Messages]
    )

    result.futureValue mustEqual Complete("/error")
  }

  "getValidationState should return Pending when CSV document is valid" in {
    val journey = InterestInLandOrProperty
    val journeyType = Standard
    val srn: SchemeId.Srn = srnGen.sample.value
    val csvDocumentState = CsvDocumentValid

    when(mockUploadService.getUploadValidationState(any))
      .thenReturn(Future.successful(Some(UploadValidated(csvDocumentState))))

    val result = service.getValidationState(srn, journey, journeyType)(
      DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, defaultUserAnswers),
      mock[Messages]
    )

    result.futureValue mustEqual Pending
  }

  "getValidationState should return Complete with success URL when CSV document is valid and saved" in {
    val journey = InterestInLandOrProperty
    val journeyType = Standard
    val srn: SchemeId.Srn = srnGen.sample.value
    val csvDocumentState = CsvDocumentValidAndSaved("form-bundle-number")

    when(mockUploadService.getUploadValidationState(any))
      .thenReturn(Future.successful(Some(UploadValidated(csvDocumentState))))

    val result = service.getValidationState(srn, journey, journeyType)(
      DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, defaultUserAnswers),
      mock[Messages]
    )

    result.futureValue mustEqual Complete(
      routes.FileUploadSuccessController.onPageLoad(srn, journey, journeyType, NormalMode).url,
      Map(Constants.formBundleNumber -> "form-bundle-number")
    )
  }

  "getValidationState should return Complete with error URL when CSV document is invalid" in {
    val journey = InterestInLandOrProperty
    val journeyType = Standard
    val srn: SchemeId.Srn = srnGen.sample.value
    val validationError2 = ValidationError(
      row = 1,
      errorType = ValidationErrorType.DuplicateNino,
      message = "Missing field"
    )
    val csvDocumentState = CsvDocumentInvalid(1, NonEmptyList.of(validationError2))

    when(mockUploadService.getUploadValidationState(any))
      .thenReturn(Future.successful(Some(UploadValidated(csvDocumentState))))
    when(mockNavigator.nextPage(any, any, any)(using any)).thenReturn(Call("GET", "/error"))

    val result = service.getValidationState(srn, journey, journeyType)(
      DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, defaultUserAnswers),
      mock[Messages]
    )

    result.futureValue mustEqual Complete("/error")
  }

  "getValidationState should return Complete with recovery URL when validation exception occurs" in {
    val journey = InterestInLandOrProperty
    val journeyType = Standard
    val srn: SchemeId.Srn = srnGen.sample.value

    when(mockUploadService.getUploadValidationState(any)).thenReturn(Future.successful(Some(ValidationException)))

    val result = service.getValidationState(srn, journey, journeyType)(
      DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, defaultUserAnswers),
      mock[Messages]
    )

    result.futureValue mustEqual Complete(controllers.routes.JourneyRecoveryController.onPageLoad().url)
  }

  "getValidationState should return Complete with ETMP error URL when saving to ETMP fails" in {
    val journey = InterestInLandOrProperty
    val journeyType = Standard
    val srn: SchemeId.Srn = srnGen.sample.value
    val etmpErrorUrl = "/etmp-error"

    when(mockUploadService.getUploadValidationState(any))
      .thenReturn(Future.successful(Some(SavingToEtmpException(etmpErrorUrl))))

    val result = service.getValidationState(srn, journey, journeyType)(
      DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, defaultUserAnswers),
      mock[Messages]
    )

    result.futureValue mustEqual Complete(etmpErrorUrl)
  }

  "getValidationState should return Pending when upload is still validating" in {
    val journey = InterestInLandOrProperty
    val journeyType = Standard
    val srn: SchemeId.Srn = srnGen.sample.value

    when(mockUploadService.getUploadValidationState(any))
      .thenReturn(Future.successful(Some(UploadValidating(Instant.now))))

    val result = service.getValidationState(srn, journey, journeyType)(
      DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, defaultUserAnswers),
      mock[Messages]
    )

    result.futureValue mustEqual Pending
  }

  "getValidationState should return Complete with recovery URL when no CSV document upload is found" in {
    val journey = InterestInLandOrProperty
    val journeyType = Standard
    val srn: SchemeId.Srn = srnGen.sample.value

    when(mockUploadService.getUploadValidationState(any)).thenReturn(Future.successful(None))

    val result = service.getValidationState(srn, journey, journeyType)(
      DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, defaultUserAnswers),
      mock[Messages]
    )

    result.futureValue mustEqual Complete(controllers.routes.JourneyRecoveryController.onPageLoad().url)
  }

  "getUploadState should return Complete with failure URL and format error param when file format is incorrect" in {
    val journey = InterestInLandOrProperty
    val journeyType = Standard
    val srn: SchemeId.Srn = srnGen.sample.value
    val success = UploadStatus.Success("test-file.txt", "test-reference", "test", None)

    when(mockUploadService.getUploadStatus(any)).thenReturn(Future.successful(Some(success)))

    val result = service.getUploadState(srn, journey, journeyType)(
      DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, defaultUserAnswers)
    )

    result.futureValue mustEqual Complete(
      routes.UploadFileController
        .onPageLoad(srn, journey, journeyType)
        .url + s"?${UploadStatus.Failed.incorrectFileFormatQueryParam}"
    )
  }

}
