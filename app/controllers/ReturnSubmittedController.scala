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
import config.FrontendAppConfig
import controllers.ReturnSubmittedController.*
import controllers.actions.*
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{DateRange, Mode}
import pages.ReturnSubmittedPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Writes.*
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ReportDetailsService, SaveService, SchemeDateService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.{localDateShow, localDateTimeShow}
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{
  LinkMessage,
  ListMessage,
  ListType,
  Message,
  ParagraphMessage
}
import viewmodels.implicits.*
import viewmodels.models.SubmissionViewModel
import views.html.SubmissionView

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReturnSubmittedController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  view: SubmissionView,
  dateService: SchemeDateService,
  reportDetailsService: ReportDetailsService,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val reportDetails = reportDetailsService.getReportDetails()

    getOrSaveSubmissionDate(srn).map { submissionDate =>
      Ok(
        view(
          viewModel(
            request.schemeDetails.schemeName,
            request.minimalDetails.email,
            NonEmptyList.one(reportDetails.taxYearDateRange),
            submissionDate,
            config.urls.pensionSchemeEnquiry,
            config.urls.managePensionsSchemes.schemeSummaryDashboard(srn, request.pensionSchemeId)
          )
        )
      )
    }
  }

  private def getOrSaveSubmissionDate(srn: Srn)(implicit request: DataRequest[?]): Future[LocalDateTime] =
    request.userAnswers.get(ReturnSubmittedPage(srn)) match {
      case Some(submissionDate) => Future.successful(submissionDate)
      case None =>
        val submissionDate = dateService.now()
        for {
          updatedUserAnswers <- Future.fromTry(request.userAnswers.set(ReturnSubmittedPage(srn), submissionDate))
          _ <- saveService.save(updatedUserAnswers)
        } yield submissionDate
    }
}

object ReturnSubmittedController {

  def viewModel(
    schemeName: String,
    email: String,
    returnPeriods: NonEmptyList[DateRange],
    submissionDate: LocalDateTime,
    pensionSchemeEnquiriesUrl: String,
    managePensionSchemeDashboardUrl: String
  ): SubmissionViewModel =
    SubmissionViewModel(
      "returnSubmitted.title",
      "returnSubmitted.panel.heading",
      "returnSubmitted.panel.content",
      email = Some(ParagraphMessage(Message("returnSubmitted.paragraph", email))),
      scheme = Message(schemeName),
      periodOfReturn = returnPeriodsToMessage(returnPeriods),
      dateSubmitted = Message(
        "site.at",
        submissionDate.show,
        submissionDate.format(DateRange.readableTimeFormat).toLowerCase()
      ),
      whatHappensNextContent = ParagraphMessage("returnSubmitted.whatHappensNext.paragraph1") ++
        ParagraphMessage(
          "returnSubmitted.whatHappensNext.paragraph2",
          LinkMessage("returnSubmitted.whatHappensNext.paragraph2.link", pensionSchemeEnquiriesUrl)
        ) ++
        ParagraphMessage("returnSubmitted.whatHappensNext.paragraph3") ++
        ListMessage.Bullet(
          Message("returnSubmitted.whatHappensNext.list1") ++ LinkMessage(
            Message("returnSubmitted.whatHappensNext.list1.link", schemeName),
            managePensionSchemeDashboardUrl
          ),
          LinkMessage("returnSubmitted.whatHappensNext.list2", "#print")
        )
    )

  def returnPeriodsToMessage(returnPeriods: NonEmptyList[DateRange]): DisplayMessage = {
    def toMessage(returnPeriod: DateRange): Message = Message(
      "site.to",
      returnPeriod.from.show,
      returnPeriod.to.show
    )

    returnPeriods match {
      case NonEmptyList(returnPeriod, Nil) => toMessage(returnPeriod)
      case _ => ListMessage(returnPeriods.map(toMessage), ListType.NewLine)
    }
  }
}
