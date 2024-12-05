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
import controllers.ViewBasicDetailsCheckYourAnswersController.viewModel
import controllers.actions.*
import models.SchemeId.{Pstr, Srn}
import models.requests.DataRequest
import models.requests.common.YesNo
import models.{DateRange, FormBundleNumber, Mode, SchemeDetails}
import navigation.Navigator
import pages.ViewBasicDetailsCheckYourAnswersPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SchemeDateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Heading2
import viewmodels.implicits.*
import viewmodels.models.*
import views.html.CheckYourAnswersView

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class ViewBasicDetailsCheckYourAnswersController @Inject() (
  @Named("sipp") navigator: Navigator,
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  checkYourAnswersView: CheckYourAnswersView,
  schemeDateService: SchemeDateService,
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withFormBundle(srn).async { request =>
      implicit val dataRequest = request.underlying

      val fbNumber = request.formBundleNumber

      val details =
        schemeDateService
          .returnBasicDetails(Pstr(request.underlying.schemeDetails.pstr), fbNumber)

      details.map { mDetails =>
        Ok(
          checkYourAnswersView(
            viewModel(
              srn,
              fbNumber,
              mode,
              loggedInUserNameOrRedirect.getOrElse(""),
              request.underlying.pensionSchemeId.value,
              request.underlying.schemeDetails,
              mDetails._2,
              mDetails._1,
              mDetails._3,
              request.underlying.pensionSchemeId.isPSP
            )
          )
        )
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
          schemeAdminName,
          pensionSchemeId,
          schemeDetails,
          whichTaxYearPage,
          accountingPeriods,
          isMemberDetailsExist,
          isPSP
        )
      ).withMarginBottom(Margin),
      refresh = None,
      buttonText = "site.returnToTaskList",
      onSubmit = routes.ViewBasicDetailsCheckYourAnswersController.onSubmit(srn)
    )
  }

  private def rows(
    schemeAdminName: String,
    pensionSchemeId: String,
    schemeDetails: SchemeDetails,
    whichTaxYearPage: DateRange,
    accountingPeriods: Option[NonEmptyList[DateRange]],
    isMemberDetailsExist: YesNo,
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
        ).withOneHalfWidth()
      ) ++
        Some(
          CheckYourAnswersRowViewModel(
            "basicDetailsCya.row5",
            if (isMemberDetailsExist == YesNo.Yes) "site.yes" else "site.no"
          )
        )
        ++
        Some(
          CheckYourAnswersRowViewModel(
            "basicDetailsCya.row6",
            whichTaxYearPage.show
          )
        ) ++ accountingPeriods.map(periods =>
          CheckYourAnswersRowViewModel(
            "basicDetailsCya.schemeDetails.accountingPeriod",
            periods.map(_.show).toList.mkString("\n")
          ).withOneHalfWidth()
        )
    )
  )
}
