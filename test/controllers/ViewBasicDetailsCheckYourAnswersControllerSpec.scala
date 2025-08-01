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

import cats.data.NonEmptyList
import cats.implicits.toShow
import controllers.ViewBasicDetailsCheckYourAnswersController.*
import models.SchemeId.{Pstr, Srn}
import models.TypeOfViewChangeQuestion.ChangeReturn
import models.requests.common.YesNo
import models.requests.psr.EtmpPsrStatus.Submitted
import models.{BasicDetails, DateRange, FormBundleNumber, Mode, NormalMode, PensionSchemeId, SchemeDetails}
import pages.{ViewChangeQuestionPage, WhichTaxYearPage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import services.SchemeDateService
import viewmodels.models.{CheckYourAnswersViewModel, FormPageViewModel}
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class ViewBasicDetailsCheckYourAnswersControllerSpec extends ControllerBaseSpec {
  private val session: Seq[(String, String)] = Seq(("fbNumber", fbNumber))
  private lazy val onPageLoad = routes.ViewBasicDetailsCheckYourAnswersController.onPageLoad(srn)
  private lazy val onSubmit = routes.ViewBasicDetailsCheckYourAnswersController.onSubmit(srn)

  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  "ViewBasicDetailsCheckYourAnswersControllerSpec" - {

    val dateRange1 = dateRangeGen.sample.value
    val accountingPeriods = Some(NonEmptyList.of(dateRange1))
    val userAnswersWithTaxYear = defaultUserAnswers.unsafeSet(WhichTaxYearPage(srn), dateRange)
    val userAnswersInChangeMode = userAnswersWithTaxYear.unsafeSet(ViewChangeQuestionPage(srn), ChangeReturn)

    List(
      (defaultMinimalDetails, individualDetails.fullName, psaId),
      (defaultMinimalDetails, individualDetails.fullName, pspId),
      (organizationMinimalDetails, organisationName, psaId),
      (organizationMinimalDetails, organisationName, pspId),
      (defaultMinimalDetails.copy(organisationName = None, individualDetails = None), "", psaId)
    ).foreach { (minimalDetails, expectedName, psaOrPspId) =>
      act.like(
        renderView(onPageLoad, userAnswersWithTaxYear, session, minimalDetails, isPsa = !psaOrPspId.isPSP) {
          implicit app => implicit request =>
            injected[CheckYourAnswersView].apply(
              viewModel(
                srn,
                FormBundleNumber(fbNumber),
                NormalMode,
                expectedName,
                psaOrPspId.value,
                defaultSchemeDetails,
                dateRange,
                accountingPeriods,
                YesNo.Yes,
                psaOrPspId.isPSP,
                false
              )
            )
        }.before(
          when(mockSchemeDateService.returnBasicDetails(any, any[FormBundleNumber])(any, any, any))
            .thenReturn(
              Future.successful(Some(BasicDetails(accountingPeriods, dateRange, YesNo.Yes, Submitted, YesNo.Yes)))
            )
        ).withName(s"render view with ${expectedName} and isPSA ${!psaOrPspId.isPSP}")
      )
    }

    act.like(
      renderView(onPageLoad, userAnswersInChangeMode, session, defaultMinimalDetails) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              srn,
              FormBundleNumber(fbNumber),
              NormalMode,
              individualDetails.fullName,
              psaId.value,
              defaultSchemeDetails,
              dateRange,
              accountingPeriods,
              YesNo.Yes,
              false,
              true
            )
          )
      }.before(
        when(mockSchemeDateService.returnBasicDetails(any, any[FormBundleNumber])(any, any, any))
          .thenReturn(
            Future.successful(Some(BasicDetails(accountingPeriods, dateRange, YesNo.Yes, Submitted, YesNo.Yes)))
          )
      ).withName("render view with in change mode")
    )

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      journeyRecoveryPage(onPageLoad, Some(userAnswersWithTaxYear), defaultMinimalDetails, session)
        .before(
          when(mockSchemeDateService.returnBasicDetails(any[Pstr], any[FormBundleNumber])(any, any, any))
            .thenReturn(Future.successful(None))
        )
        .withName("onPageLoad journeyRecovery with no scheme dates")
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "View Model" - {

    implicit val stubMessages: Messages = stubMessagesApi(
      messages = Map(
        "en" ->
          Map("site.to" -> "{0} to {1}")
      )
    ).preferred(FakeRequest())

    "should display the correct accounting periods" in {

      val dateRange1 = dateRangeGen.sample.value
      val dateRange2 = dateRangeGen.sample.value
      val dateRange3 = dateRangeGen.sample.value

      val vm = buildViewModel(
        fbNumber = fbNumber,
        accountingPeriods = Some(
          NonEmptyList.of(
            dateRange1,
            dateRange2,
            dateRange3
          )
        )
      )

      val rows = vm.page.sections.head.rows
      val dateRanges = rows(rows.length - 2).value.toString

      dateRanges must include(dateRange1.show)
      dateRanges must include(dateRange2.show)
      dateRanges must include(dateRange3.show)
    }
  }

  private def buildViewModel(
    srn: Srn = srn,
    fbNumber: String,
    mode: Mode = NormalMode,
    schemeAdminName: String = individualDetails.fullName,
    pensionSchemeId: PensionSchemeId = pensionSchemeIdGen.sample.value,
    schemeDetails: SchemeDetails = defaultSchemeDetails,
    accountingPeriods: Option[NonEmptyList[DateRange]]
  )(implicit messages: Messages): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
    srn,
    FormBundleNumber(fbNumber),
    mode,
    schemeAdminName,
    pensionSchemeId.value,
    schemeDetails,
    dateRange,
    accountingPeriods,
    YesNo.Yes,
    pensionSchemeId.isPSP,
    false
  )
}
