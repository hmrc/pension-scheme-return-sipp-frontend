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
import config.Refined.Max3
import controllers.BasicDetailsCheckYourAnswersController._
import controllers.actions._
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{CheckMode, DateRange, Mode, SchemeDetails}
import navigation.Navigator
import pages.{BasicDetailsCheckYourAnswersPage, WhichTaxYearPage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SchemeDateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Heading2
import viewmodels.implicits._
import viewmodels.models.{CheckYourAnswersRowViewModel, CheckYourAnswersSection, CheckYourAnswersViewModel, FormPageViewModel, SummaryAction}
import views.html.CheckYourAnswersView

import javax.inject.{Inject, Named}

class BasicDetailsCheckYourAnswersController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  checkYourAnswersView: CheckYourAnswersView,
  schemeDateService: SchemeDateService
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val maybePeriods = schemeDateService.returnAccountingPeriods(srn)

    Ok(
      checkYourAnswersView(
        viewModel(
          srn,
          mode,
          loggedInUserNameOrRedirect.getOrElse(""),
          request.pensionSchemeId.value,
          request.schemeDetails,
          request.userAnswers.get(WhichTaxYearPage(srn)),
          maybePeriods,
          request.pensionSchemeId.isPSP
        )
      )
    )

  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(navigator.nextPage(BasicDetailsCheckYourAnswersPage(srn), mode, request.userAnswers))
  }

  private def loggedInUserNameOrRedirect(implicit request: DataRequest[_]): Either[Result, String] =
    request.minimalDetails.individualDetails match {
      case Some(individual) => Right(individual.fullName)
      case None =>
        request.minimalDetails.organisationName match {
          case Some(orgName) => Right(orgName)
          case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }
}

object BasicDetailsCheckYourAnswersController {
  def viewModel(
    srn: Srn,
    mode: Mode,
    schemeAdminName: String,
    pensionSchemeId: String,
    schemeDetails: SchemeDetails,
    whichTaxYearPage: Option[DateRange],
    accountingPeriods: Option[NonEmptyList[(DateRange, Max3)]],
    isPSP: Boolean
  )(
    implicit messages: Messages
  ): FormPageViewModel[CheckYourAnswersViewModel] = {
    val Margin = 9
    FormPageViewModel[CheckYourAnswersViewModel](
      title = "basicDetailsCya.title",
      heading = "basicDetailsCya.heading",
      description = None,
      page = CheckYourAnswersViewModel(
        rows(
          srn,
          schemeAdminName,
          pensionSchemeId,
          schemeDetails,
          whichTaxYearPage,
          accountingPeriods,
          isPSP
        )
      ).withMarginBottom(Margin),
      refresh = None,
      buttonText = "site.saveAndContinue",
      onSubmit = routes.BasicDetailsCheckYourAnswersController.onSubmit(srn, mode)
    )
  }

  private def rows(
    srn: Srn,
    schemeAdminName: String,
    pensionSchemeId: String,
    schemeDetails: SchemeDetails,
    whichTaxYearPage: Option[DateRange],
    accountingPeriods: Option[NonEmptyList[(DateRange, Max3)]],
    isPSP: Boolean
  )(
    implicit messages: Messages
  ): List[CheckYourAnswersSection] = List(
    CheckYourAnswersSection(
      Some(Heading2.medium("basicDetailsCya.tableHeader")),
      List(
        CheckYourAnswersRowViewModel("basicDetailsCya.row1", schemeDetails.schemeName).withOneHalfWidth(),
        CheckYourAnswersRowViewModel("basicDetailsCya.row2", schemeDetails.pstr).withOneHalfWidth(),
        CheckYourAnswersRowViewModel(
          if (isPSP) {
            "basicDetailsCya.row3.asPractitioner"
          } else {
            "basicDetailsCya.row3.asAdmin"
          },
          schemeAdminName
        ).withOneHalfWidth(),
        CheckYourAnswersRowViewModel(
          if (isPSP) {
            "basicDetailsCya.row4.asPractitioner"
          } else {
            "basicDetailsCya.row4.asAdmin"
          },
          pensionSchemeId
        ).withOneHalfWidth()
      ) ++
//        whichTaxYearPage.map( taxYear => CheckYourAnswersRowViewModel(
//          "basicDetailsCya.row5",
//          taxYear.toString
//        ))
        Some(
          CheckYourAnswersRowViewModel(
            "basicDetailsCya.row5",
            "6 April 2023 to 5 April 2024" // TODO implement actual taxYear...
          ).withAction(
            SummaryAction("site.change", routes.CheckReturnDatesController.onPageLoad(srn, CheckMode).url)
          )
        ) ++ accountingPeriods.map(
        periods =>
          CheckYourAnswersRowViewModel(
            "basicDetailsCya.schemeDetails.accountingPeriod",
            periods.map(_._1.show).toList.mkString("\n")
          ).withChangeAction(
              changeUrl = controllers.accountingperiod.routes.AccountingPeriodListController
                .onPageLoad(srn, CheckMode)
                .url,
              hidden = "basicDetailsCya.schemeDetails.accountingPeriod.hidden"
            )
            .withOneHalfWidth()
      )
    )
  )
}
