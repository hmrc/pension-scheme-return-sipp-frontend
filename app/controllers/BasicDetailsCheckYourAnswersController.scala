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
import config.RefinedTypes.Max3
import controllers.BasicDetailsCheckYourAnswersController.*
import controllers.actions.*
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{CheckMode, DateRange, Mode, SchemeDetails}
import navigation.Navigator
import pages.{AssetsHeldPage, BasicDetailsCheckYourAnswersPage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SchemeDateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{Heading2, ListMessage, ListType, Message}
import viewmodels.implicits.*
import viewmodels.models.*
import views.html.CheckYourAnswersView
import cats.implicits.*

import javax.inject.{Inject, Named}

class BasicDetailsCheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  checkYourAnswersView: CheckYourAnswersView,
  schemeDateService: SchemeDateService
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withVersionAndTaxYear(srn) { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying
      dataRequest.userAnswers.get(AssetsHeldPage(srn)) match {
        case None =>
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(assetsHeld) =>
          val maybePeriods = schemeDateService
            .returnAccountingPeriods(srn)

          Ok(
            checkYourAnswersView(
              viewModel(
                srn,
                mode,
                loggedInUserNameOrRedirect.getOrElse(""),
                dataRequest.pensionSchemeId.value,
                dataRequest.schemeDetails,
                request.versionTaxYear.taxYearDateRange,
                maybePeriods,
                assetsHeld,
                dataRequest.pensionSchemeId.isPSP
              )
            )
          )
      }
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(navigator.nextPage(BasicDetailsCheckYourAnswersPage(srn), mode, request.userAnswers))
  }

  private def loggedInUserNameOrRedirect(implicit request: DataRequest[?]): Either[Result, String] =
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
    whichTaxYearPage: DateRange,
    accountingPeriods: Option[NonEmptyList[(DateRange, Max3)]],
    assetsToReport: Boolean,
    isPSP: Boolean
  )(implicit
    messages: Messages
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
          assetsToReport,
          isPSP
        )
      ).withMarginBottom(Margin),
      refresh = None,
      buttonText = "site.continue",
      onSubmit = routes.BasicDetailsCheckYourAnswersController.onSubmit(srn, mode)
    )
  }

  private def rows(
    srn: Srn,
    schemeAdminName: String,
    pensionSchemeId: String,
    schemeDetails: SchemeDetails,
    whichTaxYearPage: DateRange,
    accountingPeriods: Option[NonEmptyList[(DateRange, Max3)]],
    assetsToReport: Boolean,
    isPSP: Boolean
  )(implicit
    messages: Messages
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
        ).withOneHalfWidth(),
        CheckYourAnswersRowViewModel(
          "basicDetailsCya.row5",
          if (assetsToReport) "site.yes" else "site.no"
        ).withAction(
          SummaryAction("site.change", routes.AssetsHeldController.onPageLoad(srn).url)
        ),
        CheckYourAnswersRowViewModel(
          "basicDetailsCya.row6",
          ListMessage(
            accountingPeriods.map(_.map(_._1.show).map(Message(_)))
              .getOrElse(NonEmptyList.one(Message(whichTaxYearPage.show))),
            ListType.NewLine
          )
        ).withAction(
          SummaryAction("site.change", routes.CheckReturnDatesController.onPageLoad(srn, CheckMode).url)
        )
      )
    )
  )
}
