/*
 * Copyright 2024 HM Revenue & Customs
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

package services.view

import cats.data.NonEmptyList
import cats.implicits.toShow
import models.SchemeId.Srn
import models.backend.responses.{PSRSubmissionResponse, Version}
import models.{Journey, NormalMode}
import services.view.TaskListViewModelService.SectionStatus.{Changed, Declared, Empty}
import services.view.TaskListViewModelService.{SchemeSectionsStatus, TaskListViewModelClosure, ViewMode}
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.{InlineMessage, LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.TaskListSectionViewModel.TaskListItemViewModel
import viewmodels.models.TaskListStatus.{Completed, NotStarted, TaskListStatus, Updated}
import viewmodels.models.{PageViewModel, TaskListSectionViewModel, TaskListViewModel}

import java.time.LocalDate
import javax.inject.Inject

class TaskListViewModelService @Inject()(viewMode: ViewMode) {
  def viewModel(
    srn: Srn,
    schemeName: String,
    startDate: LocalDate,
    endDate: LocalDate,
    overviewURL: String,
    visibleItems: SchemeSectionsStatus,
    fbNumber: String
  ): PageViewModel[TaskListViewModel] =
    new TaskListViewModelClosure(
      viewMode,
      srn: Srn,
      schemeName: String,
      startDate: LocalDate,
      endDate: LocalDate,
      overviewURL: String,
      visibleItems: SchemeSectionsStatus,
      fbNumber: String
    ).pageViewModel
}

object TaskListViewModelService {
  private class TaskListViewModelClosure(
    viewMode: ViewMode,
    srn: Srn,
    schemeName: String,
    startDate: LocalDate,
    endDate: LocalDate,
    overviewURL: String,
    schemeSectionsStatus: SchemeSectionsStatus,
    fbNumber: String
  ) {
    import schemeSectionsStatus._

    private val prefix = s"tasklist.${viewMode.name}"

    private val schemeDetailsSection: TaskListSectionViewModel = {
      val prefix = s"tasklist.${viewMode.name}.schemedetails"

      TaskListSectionViewModel(
        s"$prefix.title",
        getBasicSchemeDetailsTaskListItem(prefix)
      )
    }

    private val memberDetailsSection: TaskListSectionViewModel = {
      val prefix = s"tasklist.${viewMode.name}.memberDetails"

      TaskListSectionViewModel(
        s"$prefix.title",
        getMemberDetailsTaskListItem(prefix, memberDetailsStatus)
      )
    }

    private val landOrPropertySection: TaskListSectionViewModel = {
      TaskListSectionViewModel(
        s"$prefix.landorproperty.title",
        taskListItemViewModel(Journey.InterestInLandOrProperty),
        taskListItemViewModel(Journey.ArmsLengthLandOrProperty)
      )
    }

    private val emptyTaskListItem: TaskListItemViewModel =
      TaskListItemViewModel(
        Message("tasklist.empty.interest.title"),
        Completed
      )

    private val tangibleMoveablePropertySection = singleSection(Journey.TangibleMoveableProperty)
    private val outstandingLoansSection = singleSection(Journey.OutstandingLoans)
    private val unquotedSharesSection = singleSection(Journey.UnquotedShares)
    private val assetFromConnectedPartySection = singleSection(Journey.AssetFromConnectedParty)

    private def singleSection(journey: Journey): TaskListSectionViewModel = {
      val status = schemeSectionsStatus.byJounrey(journey)

      val item = viewMode match {
        case ViewMode.View => if (status == Empty) emptyTaskListItem else taskListItemViewModel(journey)
        case ViewMode.Change => taskListItemViewModel(journey)
      }

      TaskListSectionViewModel(
        s"$prefix.${sectionKey(journey)}.title",
        item
      )
    }

    val pageViewModel: PageViewModel[TaskListViewModel] = {
      val taskListSections: List[TaskListSectionViewModel] =
        Option
          .when(viewMode == ViewMode.Change)(
            memberDetailsSection
          )
          .toList ++
          List(
            landOrPropertySection,
            tangibleMoveablePropertySection,
            outstandingLoansSection,
            unquotedSharesSection,
            assetFromConnectedPartySection
          )

      val viewModelSections: NonEmptyList[TaskListSectionViewModel] = NonEmptyList.of(
        schemeDetailsSection,
        taskListSections: _*
      )

      val viewModel = TaskListViewModel(
        sections = viewModelSections,
        postActionLink = Some(
          LinkMessage(
            s"${viewMode.name}.return",
            overviewURL
          )
        )
      )

      PageViewModel(
        Message(s"$prefix.title", startDate.show, endDate.show),
        Message(s"$prefix.heading", startDate.show, endDate.show),
        viewModel
      ).withDescription(
        ParagraphMessage(Message(s"$prefix.description", startDate.show)) ++
          ParagraphMessage(descriptionMessage) ++
          ParagraphMessage(Message(s"${viewMode.name}.view.hint"))
      )
    }

    private def descriptionMessage: InlineMessage = viewMode match {
      case ViewMode.View =>
        LinkMessage(
          s"$prefix.versions",
          controllers.routes.PsrVersionsController.onPageLoad(srn).url
        )
      case ViewMode.Change => Message(s"$prefix.direction")
    }

    private def getBasicSchemeDetailsTaskListItem(
      prefix: String
    ): TaskListItemViewModel =
      TaskListItemViewModel(
        LinkMessage(
          Message(s"$prefix.details.title", schemeName),
          controllers.routes.ViewBasicDetailsCheckYourAnswersController.onPageLoad(srn).url
        ),
        Completed
      )

    private def getMemberDetailsTaskListItem(
      prefix: String,
      memberStatus: SectionStatus
    ): TaskListItemViewModel =
      TaskListItemViewModel(
        LinkMessage(
          Message(s"$prefix.details.title", schemeName),
          controllers.routes.ViewChangeMembersController.onPageLoad(srn, 1).url
        ),
        memberStatus.toTaskListStatus
      )

    private def taskListItemViewModel(journey: Journey): TaskListItemViewModel = TaskListItemViewModel(
      LinkMessage(
        messageKey(journey),
        messageLink(journey)
      ),
      schemeSectionsStatus.byJounrey(journey).toTaskListStatus
    )

    private def messageKey(journey: Journey): String = {
      val section = sectionKey(journey)
      val title = s"$prefix.$section.details.title"

      viewMode match {
        case ViewMode.Change =>
          schemeSectionsStatus.byJounrey(journey) match {
            case SectionStatus.Empty => title
            case _ => s"$title.change"
          }

        case _ => title
      }
    }

    private def sectionKey(journey: Journey) = journey match {
      case Journey.InterestInLandOrProperty => "landorproperty.interest"
      case Journey.ArmsLengthLandOrProperty => "landorproperty.armslength"
      case Journey.TangibleMoveableProperty => "tangibleproperty"
      case Journey.OutstandingLoans => "loans"
      case Journey.UnquotedShares => "shares"
      case Journey.AssetFromConnectedParty => "assets"
    }

    private def messageLink(journey: Journey): String = viewMode match {
      case ViewMode.View =>
        controllers.routes.DownloadCsvController
          .downloadEtmpFile(
            srn,
            Journey.AssetFromConnectedParty,
            Some(fbNumber),
            None,
            None
          )
          .url

      case ViewMode.Change =>
        schemeSectionsStatus.byJounrey(journey) match {
          case Empty => controllers.routes.JourneyContributionsHeldController.onPageLoad(srn, journey, NormalMode).url
          case _ => controllers.routes.NewFileUploadController.onPageLoad(srn, journey).url
        }
    }
  }

  sealed trait ViewMode {
    def name: String
  }

  object ViewMode {
    case object View extends ViewMode {
      override val name: String = "view"
    }

    case object Change extends ViewMode {
      override val name: String = "change"
    }
  }

  sealed trait SectionStatus

  object SectionStatus {
    case object Declared extends SectionStatus
    case object Changed extends SectionStatus
    case object Empty extends SectionStatus

    implicit class SectionStatusOps(val sectionStatus: SectionStatus) extends AnyVal {
      def isEmpty: Boolean = sectionStatus == Empty
      def toTaskListStatus: TaskListStatus = sectionStatus match {
        case Declared => Completed
        case Changed => Updated
        case Empty => NotStarted
      }
    }
  }

  case class SchemeSectionsStatus(
    memberDetailsStatus: SectionStatus,
    landOrPropertyInterestStatus: SectionStatus,
    landOrPropertyArmsLengthStatus: SectionStatus,
    tangiblePropertyStatus: SectionStatus,
    sharesStatus: SectionStatus,
    assetsStatus: SectionStatus,
    loansStatus: SectionStatus
  )

  object SchemeSectionsStatus {
    implicit class Ops(val schemeSectionsStatus: SchemeSectionsStatus) extends AnyVal {
      def byJounrey(journey: Journey): SectionStatus = journey match {
        case Journey.InterestInLandOrProperty => schemeSectionsStatus.landOrPropertyInterestStatus
        case Journey.ArmsLengthLandOrProperty => schemeSectionsStatus.landOrPropertyArmsLengthStatus
        case Journey.TangibleMoveableProperty => schemeSectionsStatus.tangiblePropertyStatus
        case Journey.OutstandingLoans => schemeSectionsStatus.loansStatus
        case Journey.UnquotedShares => schemeSectionsStatus.sharesStatus
        case Journey.AssetFromConnectedParty => schemeSectionsStatus.assetsStatus
      }
    }

    def fromPSRSubmission(submissionResponse: PSRSubmissionResponse): SchemeSectionsStatus = {
      import submissionResponse._

      val psrVersion = submissionResponse.details.psrVersion.map(Version(_))
      val status = sectionStatus(_, _, psrVersion)

      SchemeSectionsStatus(
        memberDetailsStatus = status(
          false,
          versions.memberDetails
        ),
        landOrPropertyInterestStatus = status(
          landConnectedParty.isEmpty,
          versions.landConnectedParty
        ),
        landOrPropertyArmsLengthStatus = status(
          landArmsLength.isEmpty,
          versions.landArmsLength
        ),
        tangiblePropertyStatus = status(
          tangibleProperty.isEmpty,
          versions.tangibleProperty
        ),
        sharesStatus = status(
          unquotedShares.isEmpty,
          versions.unquotedShares
        ),
        assetsStatus = status(
          otherAssetsConnectedParty.isEmpty,
          versions.otherAssetsConnectedParty
        ),
        loansStatus = status(
          loanOutstanding.isEmpty,
          versions.loanOutstanding
        )
      )
    }

    private def sectionStatus(isEmpty: Boolean, version: Option[Version], psrVersion: Option[Version]): SectionStatus =
      if (isEmpty) {
        Empty
      } else {
        if (version == psrVersion) {
          Declared
        } else {
          Changed
        }
      }
  }
}
