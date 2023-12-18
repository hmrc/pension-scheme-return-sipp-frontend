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

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext
import pages.{BasicDetailsCheckYourAnswersPage, WhichTaxYearPage}
import controllers.BasicDetailsCheckYourAnswersController._
import controllers.actions._
import models.{DateRange, Mode, SchemeDetails}
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Heading2
import viewmodels.models.{CheckYourAnswersRowViewModel, CheckYourAnswersSection, CheckYourAnswersViewModel, FormPageViewModel}

import scala.concurrent.Future
import views.html.CheckYourAnswersView

class BasicDetailsCheckYourAnswersController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  checkYourAnswersView: CheckYourAnswersView
  )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Ok(
      checkYourAnswersView(
        viewModel(
          srn,
          mode,
          loggedInUserNameOrRedirect.getOrElse(""),
          request.pensionSchemeId.value,
          request.schemeDetails,
          request.userAnswers.get(WhichTaxYearPage(srn))
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
    whichTaxYearPage: Option[DateRange]
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
          whichTaxYearPage
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
    whichTaxYearPage: Option[DateRange]
  ): List[CheckYourAnswersSection] = List(
    CheckYourAnswersSection(
      Some(Heading2.medium("basicDetailsCya.tableHeader")),
      List(
        CheckYourAnswersRowViewModel("basicDetailsCya.row1", schemeDetails.schemeName).withOneHalfWidth(),
        CheckYourAnswersRowViewModel("basicDetailsCya.row2", schemeDetails.pstr).withOneHalfWidth(),
        CheckYourAnswersRowViewModel("basicDetailsCya.row3", schemeAdminName).withOneHalfWidth(),
        CheckYourAnswersRowViewModel("basicDetailsCya.row4", pensionSchemeId).withOneHalfWidth()
      ) ++
//        whichTaxYearPage.map( taxYear => CheckYourAnswersRowViewModel(
//          "basicDetailsCya.row5",
//          taxYear.toString
//        ))
        Some(CheckYourAnswersRowViewModel(
          "basicDetailsCya.row5",
          "6 April 2023 to 5 April 2024" // TODO implement actual taxYear...
        ))
    )
  )
}
