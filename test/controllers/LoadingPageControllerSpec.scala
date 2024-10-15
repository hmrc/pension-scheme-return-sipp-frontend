package controllers

import models.FileAction.Validating
import models.{Journey, JourneyType}
import services.{PendingFileActionService, TaxYearService}
import uk.gov.hmrc.time.TaxYear

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
//TODO: Fix tests
//    "onPageLoad for Validating action" - {
//      lazy val application =
//        applicationBuilder(Some(emptyUserAnswers))
//          .overrides(bind[PendingFileActionService].toInstance(mockPendingFileActionService),
//            bind[TaxYearService].toInstance(mockTaxYearService))
//          .build()
//
//      val fileAction = Validating
//      val onPageLoad = routes.LoadingPageController.onPageLoad(srn, fileAction, journey, journeyType)
//
//      "redirect to the provided URL when state is Complete for Validating action" in {
//
//        val redirectUrl = "/next-page"
//        val sessionParams = Map("param1" -> "value1", "param2" -> "value2")
//
//        when(mockPendingFileActionService.getValidationState(any, any, any)(any, any))
//          .thenReturn(Future.successful(Complete(redirectUrl, sessionParams)))
//
//        running(application) {
//          val request = FakeRequest(GET, onPageLoad.url)
//          val result = route(application, request).value
//
//          status(result) mustEqual SEE_OTHER
//          redirectLocation(result).value mustEqual redirectUrl
//        }
//      }
//
//      act.like(
//        renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
//          val view = app.injector.instanceOf[LoadingPageView]
//          val viewModel = viewModelForValidating(journey)
//          view(viewModel)
//        }.before {
//          when(mockPendingFileActionService.getValidationState(any, any, any)(any, any))
//            .thenReturn(Future.successful(Pending))
//        }.updateName("state is Pending for Validating action" + _)
//      )
//    }

//    "onPageLoad for Uploading action" - {
//      lazy val application =
//        applicationBuilder(Some(emptyUserAnswers))
//          .overrides(bind[PendingFileActionService].toInstance(mockPendingFileActionService),
//            bind[TaxYearService].toInstance(mockTaxYearService))
//          .build()
//
//      val fileAction = Uploading
//      val onPageLoad = routes.LoadingPageController.onPageLoad(srn, fileAction, journey, journeyType)
//
//      "redirect to the provided URL when state is Complete for Uploading action" in {
//
//        val redirectUrl = "/upload-complete"
//        val sessionParams = Map("sessionKey" -> "sessionValue")
//
//        when(mockPendingFileActionService.getUploadState(eqTo(srn), eqTo(journey), eqTo(journeyType))(any))
//          .thenReturn(Future.successful(Complete(redirectUrl, sessionParams)))
//
//        running(application) {
//          val request = FakeRequest(GET, onPageLoad.url)
//          val result = route(application, request).value
//
//          status(result) mustEqual SEE_OTHER
//          redirectLocation(result).value mustEqual redirectUrl
//        }
//      }

//      act.like(
//        renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
//          val view = app.injector.instanceOf[LoadingPageView]
//          val viewModel = viewModelForUploading(journey)
//          view(viewModel)
//        }.before {
//          when(mockPendingFileActionService.getUploadState(eqTo(srn), eqTo(journey), eqTo(journeyType))(any))
//            .thenReturn(Future.successful(Pending))
//        }.updateName("must render the view when state is Pending for Uploading action" + _)
//      )
//    }

    act.like(
      journeyRecoveryPage(
        routes.LoadingPageController.onPageLoad(srn, Validating, journey, journeyType)
      ).updateName("onPageLoad must redirect to Journey Recovery when no existing data is found" + _)
    )
  }
}
