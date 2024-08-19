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
import config.RefinedTypes.{Max3, OneToThree}
import controllers.BasicDetailsCheckYourAnswersController._
import eu.timepit.refined.refineMV
import models.SchemeId.Srn
import models.{DateRange, Mode, NormalMode, PensionSchemeId, SchemeDetails}
import org.mockito.ArgumentMatchers.any
import pages.{AssetsHeldPage, WhichTaxYearPage}
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

class BasicDetailsCheckYourAnswersControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.BasicDetailsCheckYourAnswersController.onSubmit(srn, NormalMode)

  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  "BasicDetailsCheckYourAnswersPageController" - {

    val assetsHeld = true
    val dateRange1 = dateRangeGen.sample.value
    val accountingPeriods = Some(NonEmptyList.of(dateRange1 -> refineMV[OneToThree](1)))
    val userAnswersWithTaxYear = defaultUserAnswers
      .unsafeSet(WhichTaxYearPage(srn), dateRange)

    val userAnswersWithTaxYearWithAssetsHeld = userAnswersWithTaxYear
      .unsafeSet(AssetsHeldPage(srn), assetsHeld)

    act.like(renderView(onPageLoad, userAnswersWithTaxYearWithAssetsHeld) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(
        viewModel(
          srn,
          NormalMode,
          individualDetails.fullName,
          psaId.value,
          defaultSchemeDetails,
          DateRange.from(TaxYear(dateRange1.from.getYear)),
          accountingPeriods,
          assetsHeld,
          psaId.isPSP
        )
      )
    }.before(when(mockSchemeDateService.returnAccountingPeriods(any())(any())).thenReturn(accountingPeriods)))

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
        accountingPeriods = Some(
          NonEmptyList.of(
            dateRange1 -> refineMV[OneToThree](1),
            dateRange2 -> refineMV[OneToThree](2),
            dateRange3 -> refineMV[OneToThree](3)
          )
        )
      )

      vm.page.sections.flatMap(_.rows.map(_.value.asInstanceOf[Message].key)) must
        contain(s"${dateRange1.show}\n${dateRange2.show}\n${dateRange3.show}")
    }
  }

  private def buildViewModel(
    srn: Srn = srn,
    mode: Mode = NormalMode,
    schemeAdminName: String = individualDetails.fullName,
    pensionSchemeId: PensionSchemeId = pensionSchemeIdGen.sample.value,
    schemeDetails: SchemeDetails = defaultSchemeDetails,
    accountingPeriods: Option[NonEmptyList[(DateRange, Max3)]]
  )(implicit messages: Messages): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
    srn,
    mode,
    schemeAdminName,
    pensionSchemeId.value,
    schemeDetails,
    dateRange,
    accountingPeriods,
    assetsToReport = true,
    pensionSchemeId.isPSP
  )
}
