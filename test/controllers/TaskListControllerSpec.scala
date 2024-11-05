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

package controllers

import cats.syntax.option._
import connectors.PSRConnector
import config.RefinedTypes.Max3
import models.Journey.{
  ArmsLengthLandOrProperty,
  AssetFromConnectedParty,
  InterestInLandOrProperty,
  OutstandingLoans,
  TangibleMoveableProperty,
  UnquotedShares
}
import models.ReportStatus.SubmittedAndSuccessfullyProcessed
import models.backend.responses.PsrAssetCountsResponse
import models.requests.psr.EtmpPsrStatus.Compiled
import models.requests.psr.ReportDetails
import models.{DateRange, JourneyType, NormalMode, PsrVersionsResponse, ReportSubmitterDetails, UserAnswers}
import models.SchemeId.Srn
import pages.accountingperiod.AccountingPeriodPage
import pages.{CheckReturnDatesPage, TaskListStatusPage}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.ReportDetailsService
import viewmodels.DisplayMessage.{LinkMessage, Message}
import viewmodels.models.TaskListStatus
import viewmodels.models.TaskListStatus.TaskListStatus
import views.html.TaskListView

import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.Future

class TaskListControllerSpec extends ControllerBaseSpec {

  private val taxYearDates: DateRange =
    DateRange(LocalDate.of(2020, 4, 6), LocalDate.of(2021, 4, 5))
  private val psrVersion = "001"
  private val session: Seq[(String, String)] = Seq(("version", psrVersion), ("taxYear", taxYearDates.to.toString))
  private val psrAssetCountsResponse = PsrAssetCountsResponse(
    interestInLandOrPropertyCount = 1,
    landArmsLengthCount = 1,
    assetsFromConnectedPartyCount = 1,
    tangibleMoveablePropertyCount = 1,
    outstandingLoansCount = 1,
    unquotedSharesCount = 1
  )
  private val reportDetails =
    ReportDetails("test", Compiled, taxYearDates.from, taxYearDates.to, schemeName.some, psrVersion.some)

  private val mockReportDetailsService = mock[ReportDetailsService]
  private val mockPsrConnector = mock[PSRConnector]
  private def dashboardUrl(srn: Srn) = s"http://localhost:10701/pension-scheme-return/${srn.value}/overview"
  
  when(mockPsrConnector.getPsrAssetCounts(any, any, any, any)(any))
    .thenReturn(Future.successful(None))

  val psrVersionResponse: PsrVersionsResponse = PsrVersionsResponse(
    reportFormBundleNumber = "123456",
    reportVersion = 1,
    reportStatus = SubmittedAndSuccessfullyProcessed,
    compilationOrSubmissionDate = ZonedDateTime.now,
    reportSubmitterDetails = Some(ReportSubmitterDetails("John", None, None)),
    psaDetails = None
  )

  when(mockPsrConnector.getPsrVersions(any, any)(any))
    .thenReturn(Future.successful(Seq(psrVersionResponse)))

  when(mockReportDetailsService.getReportDetails()(any)).thenReturn(reportDetails)

  override val additionalBindings: List[GuiceableModule] = List(
    bind[PSRConnector].toInstance(mockPsrConnector),
    bind[ReportDetailsService].toInstance(mockReportDetailsService)
  )

  "TaskListController" - {

    lazy val viewModel = TaskListController.viewModel(
      srn,
      schemeName,
      taxYearDates.from,
      taxYearDates.to,
      defaultUserAnswers,
      None,
      dashboardUrl(srn)
    )
    lazy val onPageLoad = routes.TaskListController.onPageLoad(srn)

    act.like(renderView(onPageLoad, defaultUserAnswers, session) { implicit app => implicit request =>
      val view = injected[TaskListView]
      view(viewModel)
    }.withName("task list renders OK"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    "schemeDetailsSection - basic details" - {
      "NotStarted" - {
        "check dates page absent" in {
          testViewModel(
            defaultUserAnswers,
            0,
            0,
            expectedStatus = TaskListStatus.NotStarted,
            expectedTitleKey = "tasklist.schemedetails.title",
            expectedLinkContentKey = "tasklist.schemedetails.details.title",
            expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
          )
        }
      }

      "inProgress" - {
        "stopped after dates page" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), false)

          testViewModel(
            userAnswersPopulated,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "tasklist.schemedetails.title",
            expectedLinkContentKey = "tasklist.schemedetails.details.title",
            expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
          )
        }
      }

      "completed" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

        testViewModel(
          userAnswers,
          0,
          0,
          expectedStatus = TaskListStatus.Completed,
          expectedTitleKey = "tasklist.schemedetails.title",
          expectedLinkContentKey = "tasklist.schemedetails.details.title",
          expectedLinkUrl = controllers.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode).url
        )
      }
    }
  }

  "schemeDetailsSection - land or property interest" - {

    "Incomplete" - {
      "basic details section not complete" in {
        testViewModel(
          defaultUserAnswers,
          1,
          0,
          expectedStatus = TaskListStatus.UnableToStart,
          expectedTitleKey = "tasklist.landorproperty.title",
          expectedLinkContentKey = "tasklist.landorproperty.interest.title",
          expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
        )
      }
    }

    "NotStarted" - {
      "yes / no is not selected" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

        testViewModel(
          userAnswers,
          1,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "tasklist.landorproperty.title",
          expectedLinkContentKey = "tasklist.landorproperty.interest.title",
          expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
            .onPageLoad(srn, InterestInLandOrProperty, NormalMode)
            .url
        )
      }
    }

    "completed with true" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
          .unsafeSet(
            TaskListStatusPage(srn, InterestInLandOrProperty),
            TaskListStatusPage.Status(completedWithNo = false)
          )

      testViewModel(
        userAnswers,
        1,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.landorproperty.title",
        expectedLinkContentKey = "tasklist.landorproperty.interest.title.change",
        expectedLinkUrl =
          controllers.routes.NewFileUploadController.onPageLoad(srn, InterestInLandOrProperty, JourneyType.Standard).url
      )
    }

    "completed with false" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
          .unsafeSet(
            TaskListStatusPage(srn, InterestInLandOrProperty),
            TaskListStatusPage.Status(completedWithNo = true)
          )

      testViewModel(
        userAnswers,
        1,
        0,
        expectedStatus = TaskListStatus.CompletedWithoutUpload,
        expectedTitleKey = "tasklist.landorproperty.title",
        expectedLinkContentKey = "tasklist.landorproperty.interest.title",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, InterestInLandOrProperty, NormalMode)
          .url
      )
    }

    "completed when not set and asset count > 0" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

      testViewModel(
        userAnswers,
        1,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.landorproperty.title",
        expectedLinkContentKey = "tasklist.landorproperty.interest.title.change",
        expectedLinkUrl = controllers.routes.NewFileUploadController
          .onPageLoad(srn, InterestInLandOrProperty, JourneyType.Standard)
          .url,
        Some(psrAssetCountsResponse)
      )
    }

    "not started when not set and asset count == 0" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

      testViewModel(
        userAnswers,
        1,
        0,
        expectedStatus = TaskListStatus.NotStarted,
        expectedTitleKey = "tasklist.landorproperty.title",
        expectedLinkContentKey = "tasklist.landorproperty.interest.title",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, InterestInLandOrProperty, NormalMode)
          .url,
        Some(psrAssetCountsResponse.copy(interestInLandOrPropertyCount = 0))
      )
    }

    "completed without upload when asset count == 0 - fallback to frontend flag" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
          .unsafeSet(
            TaskListStatusPage(srn, InterestInLandOrProperty),
            TaskListStatusPage.Status(completedWithNo = true)
          )
      testViewModel(
        userAnswers,
        1,
        0,
        expectedStatus = TaskListStatus.CompletedWithoutUpload,
        expectedTitleKey = "tasklist.landorproperty.title",
        expectedLinkContentKey = "tasklist.landorproperty.interest.title",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, InterestInLandOrProperty, NormalMode)
          .url,
        Some(psrAssetCountsResponse.copy(interestInLandOrPropertyCount = 0))
      )
    }
  }

  "schemeDetailsSection - arms length" - {

    "Incomplete" - {
      "basic details section not complete" in {
        testViewModel(
          defaultUserAnswers,
          1,
          1,
          expectedStatus = TaskListStatus.UnableToStart,
          expectedTitleKey = "tasklist.landorproperty.title",
          expectedLinkContentKey = "tasklist.landorproperty.armslength.title",
          expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
        )
      }
    }

    "NotStarted" - {
      "yes / no is not selected" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

        testViewModel(
          userAnswers,
          1,
          1,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "tasklist.landorproperty.title",
          expectedLinkContentKey = "tasklist.landorproperty.armslength.title",
          expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
            .onPageLoad(srn, ArmsLengthLandOrProperty, NormalMode)
            .url
        )
      }
    }

    "completed with true" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
          .unsafeSet(
            TaskListStatusPage(srn, ArmsLengthLandOrProperty),
            TaskListStatusPage.Status(completedWithNo = false)
          )

      testViewModel(
        userAnswers,
        1,
        1,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.landorproperty.title",
        expectedLinkContentKey = "tasklist.landorproperty.armslength.title.change",
        expectedLinkUrl =
          controllers.routes.NewFileUploadController.onPageLoad(srn, ArmsLengthLandOrProperty, JourneyType.Standard).url
      )
    }

    "completed with false" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
          .unsafeSet(
            TaskListStatusPage(srn, ArmsLengthLandOrProperty),
            TaskListStatusPage.Status(completedWithNo = true)
          )

      testViewModel(
        userAnswers,
        1,
        1,
        expectedStatus = TaskListStatus.CompletedWithoutUpload,
        expectedTitleKey = "tasklist.landorproperty.title",
        expectedLinkContentKey = "tasklist.landorproperty.armslength.title",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, ArmsLengthLandOrProperty, NormalMode)
          .url
      )
    }

    "completed when not set and asset count > 0" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

      testViewModel(
        userAnswers,
        1,
        1,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.landorproperty.title",
        expectedLinkContentKey = "tasklist.landorproperty.armslength.title.change",
        expectedLinkUrl = controllers.routes.NewFileUploadController
          .onPageLoad(srn, ArmsLengthLandOrProperty, JourneyType.Standard)
          .url,
        Some(psrAssetCountsResponse)
      )
    }

    "not started when not set and asset count == 0" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

      testViewModel(
        userAnswers,
        1,
        1,
        expectedStatus = TaskListStatus.NotStarted,
        expectedTitleKey = "tasklist.landorproperty.title",
        expectedLinkContentKey = "tasklist.landorproperty.armslength.title",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, ArmsLengthLandOrProperty, NormalMode)
          .url,
        Some(psrAssetCountsResponse.copy(landArmsLengthCount = 0))
      )
    }
  }

  "schemeDetailsSection - Tangible moveable property" - {

    "Incomplete" - {
      "basic details section not complete" in {
        testViewModel(
          defaultUserAnswers,
          2,
          0,
          expectedStatus = TaskListStatus.UnableToStart,
          expectedTitleKey = "tasklist.tangibleproperty.title",
          expectedLinkContentKey = "tasklist.tangibleproperty.details.title",
          expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
        )
      }
    }

    "NotStarted" - {
      "yes / no is not selected" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

        testViewModel(
          userAnswers,
          2,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "tasklist.tangibleproperty.title",
          expectedLinkContentKey = "tasklist.tangibleproperty.details.title",
          expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
            .onPageLoad(srn, TangibleMoveableProperty, NormalMode)
            .url
        )
      }
    }

    "completed with true" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
          .unsafeSet(
            TaskListStatusPage(srn, TangibleMoveableProperty),
            TaskListStatusPage.Status(completedWithNo = false)
          )

      testViewModel(
        userAnswers,
        2,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.tangibleproperty.title",
        expectedLinkContentKey = "tasklist.tangibleproperty.details.title.change",
        expectedLinkUrl =
          controllers.routes.NewFileUploadController.onPageLoad(srn, TangibleMoveableProperty, JourneyType.Standard).url
      )
    }

    "completed with false" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
          .unsafeSet(
            TaskListStatusPage(srn, TangibleMoveableProperty),
            TaskListStatusPage.Status(completedWithNo = true)
          )

      testViewModel(
        userAnswers,
        2,
        0,
        expectedStatus = TaskListStatus.CompletedWithoutUpload,
        expectedTitleKey = "tasklist.tangibleproperty.title",
        expectedLinkContentKey = "tasklist.tangibleproperty.details.title",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, TangibleMoveableProperty, NormalMode)
          .url
      )
    }

    "completed when not set and asset count > 0" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
          .unsafeSet(
            TaskListStatusPage(srn, TangibleMoveableProperty),
            TaskListStatusPage.Status(completedWithNo = true)
          )

      testViewModel(
        userAnswers,
        2,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.tangibleproperty.title",
        expectedLinkContentKey = "tasklist.tangibleproperty.details.title.change",
        expectedLinkUrl = controllers.routes.NewFileUploadController
          .onPageLoad(srn, TangibleMoveableProperty, JourneyType.Standard)
          .url,
        Some(psrAssetCountsResponse)
      )
    }

    "not started when not set and asset count == 0" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

      testViewModel(
        userAnswers,
        2,
        0,
        expectedStatus = TaskListStatus.NotStarted,
        expectedTitleKey = "tasklist.tangibleproperty.title",
        expectedLinkContentKey = "tasklist.tangibleproperty.details.title",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, TangibleMoveableProperty, NormalMode)
          .url,
        Some(psrAssetCountsResponse.copy(tangibleMoveablePropertyCount = 0))
      )
    }
  }

  "schemeDetailsSection - Outstanding loans" - {

    "Incomplete" - {
      "basic details section not complete" in {
        testViewModel(
          defaultUserAnswers,
          3,
          0,
          expectedStatus = TaskListStatus.UnableToStart,
          expectedTitleKey = "tasklist.loans.title",
          expectedLinkContentKey = "tasklist.loans.details.title",
          expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
        )
      }
    }

    "NotStarted" - {
      "yes / no is not selected" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

        testViewModel(
          userAnswers,
          3,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "tasklist.loans.title",
          expectedLinkContentKey = "tasklist.loans.details.title",
          expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
            .onPageLoad(srn, OutstandingLoans, NormalMode)
            .url
        )
      }
    }

    "completed with true" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
          .unsafeSet(TaskListStatusPage(srn, OutstandingLoans), TaskListStatusPage.Status(completedWithNo = false))

      testViewModel(
        userAnswers,
        3,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.loans.title",
        expectedLinkContentKey = "tasklist.loans.details.title.change",
        expectedLinkUrl =
          controllers.routes.NewFileUploadController.onPageLoad(srn, OutstandingLoans, JourneyType.Standard).url
      )
    }

    "completed with false" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
          .unsafeSet(
            TaskListStatusPage(srn, OutstandingLoans),
            TaskListStatusPage.Status(completedWithNo = true)
          )

      testViewModel(
        userAnswers,
        3,
        0,
        expectedStatus = TaskListStatus.CompletedWithoutUpload,
        expectedTitleKey = "tasklist.loans.title",
        expectedLinkContentKey = "tasklist.loans.details.title",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, OutstandingLoans, NormalMode)
          .url
      )
    }

    "completed when not set and asset count > 0" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

      testViewModel(
        userAnswers,
        3,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.loans.title",
        expectedLinkContentKey = "tasklist.loans.details.title.change",
        expectedLinkUrl = controllers.routes.NewFileUploadController
          .onPageLoad(srn, OutstandingLoans, JourneyType.Standard)
          .url,
        Some(psrAssetCountsResponse)
      )
    }

    "not started when not set and asset count == 0" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

      testViewModel(
        userAnswers,
        3,
        0,
        expectedStatus = TaskListStatus.NotStarted,
        expectedTitleKey = "tasklist.loans.title",
        expectedLinkContentKey = "tasklist.loans.details.title",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, OutstandingLoans, NormalMode)
          .url,
        Some(psrAssetCountsResponse.copy(outstandingLoansCount = 0))
      )
    }
  }

  "schemeDetailsSection - Unquoted shares" - {

    "Incomplete" - {
      "basic details section not complete" in {
        testViewModel(
          defaultUserAnswers,
          4,
          0,
          expectedStatus = TaskListStatus.UnableToStart,
          expectedTitleKey = "tasklist.shares.title",
          expectedLinkContentKey = "tasklist.shares.details.title",
          expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
        )
      }
    }

    "NotStarted" - {
      "yes / no is not selected" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

        testViewModel(
          userAnswers,
          4,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "tasklist.shares.title",
          expectedLinkContentKey = "tasklist.shares.details.title",
          expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
            .onPageLoad(srn, UnquotedShares, NormalMode)
            .url
        )
      }
    }

    "completed with true" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
          .unsafeSet(TaskListStatusPage(srn, UnquotedShares), TaskListStatusPage.Status(completedWithNo = false))

      testViewModel(
        userAnswers,
        4,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.shares.title",
        expectedLinkContentKey = "tasklist.shares.details.title.change",
        expectedLinkUrl =
          controllers.routes.NewFileUploadController.onPageLoad(srn, UnquotedShares, JourneyType.Standard).url
      )
    }

    "completed with false" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
          .unsafeSet(TaskListStatusPage(srn, UnquotedShares), TaskListStatusPage.Status(completedWithNo = true))

      testViewModel(
        userAnswers,
        4,
        0,
        expectedStatus = TaskListStatus.CompletedWithoutUpload,
        expectedTitleKey = "tasklist.shares.title",
        expectedLinkContentKey = "tasklist.shares.details.title",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, UnquotedShares, NormalMode)
          .url
      )
    }

    "completed when not set and asset count > 0" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

      testViewModel(
        userAnswers,
        4,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.shares.title",
        expectedLinkContentKey = "tasklist.shares.details.title.change",
        expectedLinkUrl = controllers.routes.NewFileUploadController
          .onPageLoad(srn, UnquotedShares, JourneyType.Standard)
          .url,
        Some(psrAssetCountsResponse)
      )
    }

    "not started when not set and asset count == 0" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

      testViewModel(
        userAnswers,
        4,
        0,
        expectedStatus = TaskListStatus.NotStarted,
        expectedTitleKey = "tasklist.shares.title",
        expectedLinkContentKey = "tasklist.shares.details.title",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, UnquotedShares, NormalMode)
          .url,
        Some(psrAssetCountsResponse.copy(unquotedSharesCount = 0))
      )
    }
  }

  "schemeDetailsSection - Asset from a connected party" - {

    "Incomplete" - {
      "basic details section not complete" in {
        testViewModel(
          defaultUserAnswers,
          5,
          0,
          expectedStatus = TaskListStatus.UnableToStart,
          expectedTitleKey = "tasklist.assets.title",
          expectedLinkContentKey = "tasklist.assets.details.title",
          expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
        )
      }
    }

    "NotStarted" - {
      "yes / no is not selected" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)

        testViewModel(
          userAnswers,
          5,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "tasklist.assets.title",
          expectedLinkContentKey = "tasklist.assets.details.title",
          expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
            .onPageLoad(srn, AssetFromConnectedParty, NormalMode)
            .url
        )
      }
    }

    "completed with true" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
          .unsafeSet(
            TaskListStatusPage(srn, AssetFromConnectedParty),
            TaskListStatusPage.Status(completedWithNo = false)
          )

      testViewModel(
        userAnswers,
        5,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.assets.title",
        expectedLinkContentKey = "tasklist.assets.details.title.change",
        expectedLinkUrl =
          controllers.routes.NewFileUploadController.onPageLoad(srn, AssetFromConnectedParty, JourneyType.Standard).url
      )
    }

    "completed with false" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
          .unsafeSet(
            TaskListStatusPage(srn, AssetFromConnectedParty),
            TaskListStatusPage.Status(completedWithNo = true)
          )

      testViewModel(
        userAnswers,
        5,
        0,
        expectedStatus = TaskListStatus.CompletedWithoutUpload,
        expectedTitleKey = "tasklist.assets.title",
        expectedLinkContentKey = "tasklist.assets.details.title",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, AssetFromConnectedParty, NormalMode)
          .url
      )
    }
  }

  "schemeDetailsSection - Declaration" - {

    "Cannot start yet" - {
      "One section is not complete (ArmsLengthLandOrProperty)" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
            .unsafeSet(
              TaskListStatusPage(srn, InterestInLandOrProperty),
              TaskListStatusPage.Status(completedWithNo = true)
            )
            .unsafeSet(
              TaskListStatusPage(srn, TangibleMoveableProperty),
              TaskListStatusPage.Status(completedWithNo = true)
            )
            .unsafeSet(TaskListStatusPage(srn, OutstandingLoans), TaskListStatusPage.Status(completedWithNo = true))
            .unsafeSet(TaskListStatusPage(srn, UnquotedShares), TaskListStatusPage.Status(completedWithNo = true))
            .unsafeSet(
              TaskListStatusPage(srn, AssetFromConnectedParty),
              TaskListStatusPage.Status(completedWithNo = true)
            )

        testViewModel(
          userAnswers,
          6,
          0,
          expectedStatus = TaskListStatus.UnableToStart,
          expectedTitleKey = "tasklist.declaration.title",
          expectedLinkContentKey = "tasklist.declaration.incomplete",
          expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
        )
      }
    }

    "NotStarted" - {
      "all previous sections are complete with No" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
            .unsafeSet(
              TaskListStatusPage(srn, InterestInLandOrProperty),
              TaskListStatusPage.Status(completedWithNo = true)
            )
            .unsafeSet(
              TaskListStatusPage(srn, ArmsLengthLandOrProperty),
              TaskListStatusPage.Status(completedWithNo = true)
            )
            .unsafeSet(
              TaskListStatusPage(srn, TangibleMoveableProperty),
              TaskListStatusPage.Status(completedWithNo = true)
            )
            .unsafeSet(TaskListStatusPage(srn, OutstandingLoans), TaskListStatusPage.Status(completedWithNo = true))
            .unsafeSet(TaskListStatusPage(srn, UnquotedShares), TaskListStatusPage.Status(completedWithNo = true))
            .unsafeSet(
              TaskListStatusPage(srn, AssetFromConnectedParty),
              TaskListStatusPage.Status(completedWithNo = true)
            )

        testViewModel(
          userAnswers,
          6,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "tasklist.declaration.title",
          expectedLinkContentKey = "tasklist.declaration.complete",
          expectedLinkUrl = controllers.routes.DeclarationController.onPageLoad(srn, None).url
        )
      }

      "all previous sections are complete with mixed answers" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRange)
            .unsafeSet(
              TaskListStatusPage(srn, InterestInLandOrProperty),
              TaskListStatusPage.Status(completedWithNo = true)
            )
            .unsafeSet(
              TaskListStatusPage(srn, ArmsLengthLandOrProperty),
              TaskListStatusPage.Status(completedWithNo = false)
            )
            .unsafeSet(
              TaskListStatusPage(srn, TangibleMoveableProperty),
              TaskListStatusPage.Status(completedWithNo = true)
            )
            .unsafeSet(
              TaskListStatusPage(srn, OutstandingLoans),
              TaskListStatusPage.Status(completedWithNo = false)
            )
            .unsafeSet(TaskListStatusPage(srn, UnquotedShares), TaskListStatusPage.Status(completedWithNo = true))
            .unsafeSet(
              TaskListStatusPage(srn, AssetFromConnectedParty),
              TaskListStatusPage.Status(completedWithNo = true)
            )

        testViewModel(
          userAnswers,
          6,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "tasklist.declaration.title",
          expectedLinkContentKey = "tasklist.declaration.complete",
          expectedLinkUrl = controllers.routes.DeclarationController.onPageLoad(srn, None).url
        )
      }
    }
  }

  private def testViewModel(
    userAnswersPopulated: UserAnswers,
    sectionIndex: Int,
    itemIndex: Int,
    expectedStatus: TaskListStatus,
    expectedTitleKey: String,
    expectedLinkContentKey: String,
    expectedLinkUrl: String,
    counts: Option[PsrAssetCountsResponse] = None
  ) = {
    val customViewModel = TaskListController.viewModel(
      srn,
      schemeName,
      taxYearDates.from,
      taxYearDates.to,
      userAnswersPopulated,
      counts,
      dashboardUrl(srn)
    )
    val sections = customViewModel.page.sections.toList
    sections(sectionIndex).title.key mustBe expectedTitleKey
    val items = sections(sectionIndex).taskListViewItems
    val item = items(itemIndex)
    item.status mustBe Some(expectedStatus)
    item.link match {
      case LinkMessage(content, url, _) =>
        content.key mustBe expectedLinkContentKey
        url mustBe expectedLinkUrl

      case Message(key, _) =>
        key mustBe expectedLinkContentKey

      case other => fail(s"unexpected display message $other")
    }
  }
}
