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
import controllers.ViewBasicDetailsCheckYourAnswersController.viewModel
import controllers.actions.*
import models.SchemeId.{Pstr, Srn}
import models.requests.DataRequest
import models.requests.common.YesNo
import models.{CheckMode, DateRange, FormBundleNumber, Mode, SchemeDetails}
import navigation.Navigator
import pages.{ViewBasicDetailsCheckYourAnswersPage, ViewChangeQuestionPage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SchemeDateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{ListMessage, ListType, Message}
import viewmodels.implicits.*
import viewmodels.models.*
import views.html.CheckYourAnswersView
import cats.implicits.*
import models.TypeOfViewChangeQuestion.ChangeReturn
import scala.util.chaining.scalaUtilChainingOps

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class ViewBasicDetailsCheckYourAnswersController @Inject() (
  @Named("sipp") navigator: Navigator,
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  checkYourAnswersView: CheckYourAnswersView,
  schemeDateService: SchemeDateService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withFormBundle(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying

      val fbNumber = request.formBundleNumber

      val details =
        schemeDateService
          .returnBasicDetails(Pstr(request.underlying.schemeDetails.pstr), fbNumber)

      details.map {
        case Some(details) =>
          val isChange = request.underlying.userAnswers.get(ViewChangeQuestionPage(srn)).contains(ChangeReturn)
          Ok(
            checkYourAnswersView(
              viewModel(
                srn,
                fbNumber,
                mode,
                loggedInUserNameOrRedirect.getOrElse(""),
                request.underlying.pensionSchemeId.value,
                request.underlying.schemeDetails,
                details.taxYearDateRange,
                details.accountingPeriods,
                details.memberDetails,
                request.underlying.pensionSchemeId.isPSP,
                isChange
              )
            )
          )
        case None =>
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }

    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(navigator.nextPage(ViewBasicDetailsCheckYourAnswersPage(srn), mode, request.userAnswers))
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

object ViewBasicDetailsCheckYourAnswersController {
  def viewModel(
    srn: Srn,
    fbNumber: FormBundleNumber,
    mode: Mode,
    schemeAdminName: String,
    pensionSchemeId: String,
    schemeDetails: SchemeDetails,
    whichTaxYearPage: DateRange,
    accountingPeriods: Option[NonEmptyList[DateRange]],
    isMemberDetailsExist: YesNo,
    isPSP: Boolean,
    isChange: Boolean
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
          isMemberDetailsExist,
          isPSP,
          isChange
        )
      ).withMarginBottom(Margin),
      refresh = None,
      buttonText = "site.returnToTaskList",
      onSubmit = routes.ViewBasicDetailsCheckYourAnswersController.onSubmit(srn)
    )
  }

  private def rows(
    srn: Srn,
    schemeAdminName: String,
    pensionSchemeId: String,
    schemeDetails: SchemeDetails,
    whichTaxYearPage: DateRange,
    accountingPeriods: Option[NonEmptyList[DateRange]],
    isMemberDetailsExist: YesNo,
    isPSP: Boolean,
    isChange: Boolean
  )(implicit
    messages: Messages
  ): List[CheckYourAnswersSection] = List(
    CheckYourAnswersSection(
      None,
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
          ListMessage(
            accountingPeriods
              .map(_.map(range => Message(range.show)))
              .getOrElse(NonEmptyList.one(Message(whichTaxYearPage.show))),
            ListType.NewLine
          )
        ).pipe(model =>
          if (isChange)
            model.withAction(
              SummaryAction("site.change", routes.CheckReturnDatesController.onPageLoad(srn, CheckMode).url)
                .withVisuallyHiddenContent("basicDetailsCya.hidden.changeTheDates")
            )
          else
            model
        ),
        CheckYourAnswersRowViewModel(
          "basicDetailsCya.row6",
          if (isMemberDetailsExist == YesNo.Yes) "site.yes" else "site.no"
        )
      )
    )
  )
}
