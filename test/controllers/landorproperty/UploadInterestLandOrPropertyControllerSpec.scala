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

import controllers.ControllerBaseSpec
import models.{UpscanFileReference, UpscanInitiateResponse}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.data.FormError
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Call
import play.api.test.FakeRequest
import services.UploadService
import views.html.UploadView

import scala.concurrent.Future

class UploadInterestLandOrPropertyControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.UploadInterestLandOrPropertyController.onPageLoad(srn)
  private def onPageLoad(errorCode: String, errorMessage: String): Call =
    onPageLoad.copy(url = onPageLoad.url + s"?errorCode=$errorCode&errorMessage=$errorMessage")

  private lazy val onSubmit = routes.UploadInterestLandOrPropertyController.onSubmit(srn)

  private val postTarget = "test-post-target"
  private val formFields = Map("test1" -> "field1", "test2" -> "field2")

  private val upscanInitiateResponse = UpscanInitiateResponse(UpscanFileReference("test-ref"), postTarget, formFields)

  private val mockUploadService = mock[UploadService]

  override val additionalBindings: List[GuiceableModule] = List(bind[UploadService].toInstance(mockUploadService))

  override def beforeEach(): Unit = {
    reset(mockUploadService)
    when(mockUploadService.registerUploadRequest(any(), any()))
      .thenReturn(Future.successful((): Unit))
  }

  "onPageLoad should use the right Upscan config URLs" in {
    running(_ => applicationBuilder(Some(defaultUserAnswers))) { implicit app =>
      when(mockUploadService.initiateUpscan(any(), any(), any())(any()))
        .thenReturn(Future.successful(upscanInitiateResponse))

      val successCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val failureCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val request = FakeRequest(onPageLoad)

      route(app, request).value.futureValue

      verify(mockUploadService).initiateUpscan(any(), successCaptor.capture(), failureCaptor.capture())(any())

      val actualSuccessUrl = successCaptor.getValue
      val actualFailureUrl = failureCaptor.getValue

      actualSuccessUrl must endWith("/submit-upload-interest-land-or-property-file")
      actualFailureUrl must endWith("/upload-interest-land-or-property-file")
    }
  }

  "UploadInterestLandOrPropertyController" - {
    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[UploadView].apply(UploadInterestLandOrPropertyController.viewModel(postTarget, formFields, None, "100MB"))
    }.before(mockInitiateUpscan()))

    act.like(
      renderView(onPageLoad("EntityTooLarge", "file too large")) { implicit app => implicit request =>
        injected[UploadView].apply(
          UploadInterestLandOrPropertyController.viewModel(
            postTarget,
            formFields,
            Some(FormError("file-input", "uploadMemberDetails.error.size", Seq("100MB"))),
            "100MB"
          )
        )
      }.before({
          mockInitiateUpscan()
        })
        .updateName(_ + " with error EntityTooLarge")
    )

    act.like(
      renderView(onPageLoad("InvalidArgument", "'file' field not found")) { implicit app => implicit request =>
        injected[UploadView].apply(
          UploadInterestLandOrPropertyController.viewModel(
            postTarget,
            formFields,
            Some(FormError("file-input", "uploadMemberDetails.error.required")),
            "100MB"
          )
        )
      }.updateName(_ + " with error InvalidArgument")
        .before({
          mockInitiateUpscan()
        })
    )

    act.like(
      renderView(onPageLoad("EntityTooSmall", "Your proposed upload is smaller than the minimum allowed size")) {
        implicit app => implicit request =>
          injected[UploadView].apply(
            UploadInterestLandOrPropertyController.viewModel(
              postTarget,
              formFields,
              Some(FormError("file-input", "uploadMemberDetails.error.required")),
              "100MB"
            )
          )
      }.updateName(_ + " with error InvalidArgument (EntityTooSmall)")
        .before({
          mockInitiateUpscan()
        })
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

//    act.like(continueNoSave(onSubmit)) // TODO ACTIVATE IT AFTER SUBMIT PAGE!

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  private def mockInitiateUpscan(): Unit =
    when(mockUploadService.initiateUpscan(any(), any(), any())(any()))
      .thenReturn(Future.successful(upscanInitiateResponse))
}
