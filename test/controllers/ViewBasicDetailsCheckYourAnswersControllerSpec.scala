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
import controllers.ViewBasicDetailsCheckYourAnswersController._
import models.SchemeId.Srn
import models.{DateRange, Mode, NormalMode, PensionSchemeId, SchemeDetails}
import org.mockito.ArgumentMatchers.any
import pages.WhichTaxYearPage
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import services.SchemeDateService
import uk.gov.hmrc.time.TaxYear
import viewmodels.DisplayMessage.Message
import viewmodels.models.{CheckYourAnswersViewModel, FormPageViewModel}
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class ViewBasicDetailsCheckYourAnswersControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.ViewBasicDetailsCheckYourAnswersController.onPageLoad(srn)
  private lazy val onSubmit = routes.ViewBasicDetailsCheckYourAnswersController.onSubmit(srn)

  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  "BasicDetailsCheckYourAnswersPageController" - {

    val dateRange1 = dateRangeGen.sample.value
    val accountingPeriods = Some(NonEmptyList.of(dateRange1))
    val userAnswersWithTaxYear = defaultUserAnswers
      .unsafeSet(WhichTaxYearPage(srn), dateRange)

    act.like(renderView(onPageLoad, userAnswersWithTaxYear, Seq(("fbNumber", fbNumber))) {
      implicit app => implicit request =>
        injected[CheckYourAnswersView].apply(
          viewModel(
            srn,
            fbNumber,
            NormalMode,
            individualDetails.fullName,
            psaId.value,
            defaultSchemeDetails,
            DateRange.from(TaxYear(dateRange1.from.getYear)),
            accountingPeriods,
            psaId.isPSP
          )
        )
    }.before(when(mockSchemeDateService.returnAccountingPeriodsFromEtmp(any(), any())(any(), any())).thenReturn(Future.successful(accountingPeriods))))

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

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

      vm.page.sections.flatMap(_.rows.map(_.value.asInstanceOf[Message].key)) must
        contain(s"${dateRange1.show}\n${dateRange2.show}\n${dateRange3.show}")
    }
  }

  private def buildViewModel(
    srn: Srn = srn,
    fbNumber: String,
    mode: Mode = NormalMode,
    schemeAdminName: String = individualDetails.fullName,
    pensionSchemeId: PensionSchemeId = pensionSchemeIdGen.sample.value,
    schemeDetails: SchemeDetails = defaultSchemeDetails,
    whichTaxYearPage: Option[DateRange] = Some(dateRange),
    accountingPeriods: Option[NonEmptyList[DateRange]]
  )(implicit messages: Messages): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
    srn,
    fbNumber,
    mode,
    schemeAdminName,
    pensionSchemeId.value,
    schemeDetails,
    dateRange,
    accountingPeriods,
    pensionSchemeId.isPSP
  )
}
