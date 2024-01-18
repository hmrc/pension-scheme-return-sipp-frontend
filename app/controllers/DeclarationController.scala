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

import controllers.CheckReturnDatesController.{max, min}
import controllers.actions.{AllowAccessActionProvider, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.{DateRange, MinimalSchemeDetails, NormalMode, PensionSchemeId}
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import pages.{CheckReturnDatesPage, DeclarationPage, WhichTaxYearPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{SchemeDetailsService, TaxYearService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{
  CaptionHeading2,
  Heading2,
  HintMessage,
  InsetTextMessage,
  ListMessage,
  ListType,
  Message,
  ParagraphMessage
}
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView
import utils.DateTimeUtils.localDateShow
import cats.implicits.toShow
import viewmodels.{Caption, LabelSize}

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView,
  taxYearService: TaxYearService,
  schemeDetailsService: SchemeDetailsService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      getMinimalSchemeDetails(request.pensionSchemeId, srn) { details =>
        getWhichTaxYear(srn) { taxYear =>
          val viewModel = DeclarationController.viewModel(srn, taxYear.from, taxYear.to, details)
          Future.successful(Ok(view(viewModel)))
        }
      }

    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(DeclarationPage(srn), NormalMode, request.userAnswers))
    }

  private def getWhichTaxYear(
    srn: Srn
  )(f: DateRange => Future[Result])(implicit request: DataRequest[_]): Future[Result] =
    request.userAnswers.get(WhichTaxYearPage(srn)) match {
      case Some(taxYear) => f(taxYear)
      case None => f(DateRange.from(taxYearService.current))
    }

  private def getMinimalSchemeDetails(id: PensionSchemeId, srn: Srn)(
    f: MinimalSchemeDetails => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] =
    schemeDetailsService.getMinimalSchemeDetails(id, srn).flatMap {
      case Some(schemeDetails) => f(schemeDetails)
      case None => Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad))
    }
}

object DeclarationController {

  private def max(d1: LocalDate, d2: LocalDate): LocalDate =
    if (d1.isAfter(d2)) d1 else d2

  private def min(d1: LocalDate, d2: LocalDate): LocalDate =
    if (d1.isAfter(d2)) d2 else d1

  def viewModel(
    srn: Srn,
    fromDate: LocalDate,
    toDate: LocalDate,
    schemeDetails: MinimalSchemeDetails
  ): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("psaDeclaration.title"),
      Message("psaDeclaration.heading"),
      ContentPageViewModel(),
      routes.DeclarationController.onSubmit(srn)
    ).withButtonText(Message("site.agreeAndContinue"))
      .withDescription(
        InsetTextMessage(
          Message(
            "psaDeclaration.taxYear",
            max(schemeDetails.openDate.getOrElse(fromDate), fromDate).show,
            min(schemeDetails.windUpDate.getOrElse(toDate), toDate).show
          )
        ) ++
          ParagraphMessage("psaDeclaration.paragraph") ++
          ListMessage(
            ListType.Bullet,
            "psaDeclaration.listItem1",
            "psaDeclaration.listItem2",
            "psaDeclaration.listItem3",
            "psaDeclaration.listItem4"
          )
      )
      .withAdditionalHeadingText(CaptionHeading2(Message(schemeDetails.name), Caption.Large))

}
