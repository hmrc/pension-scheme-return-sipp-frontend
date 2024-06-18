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

import models.DateRange
import uk.gov.hmrc.time.TaxYear
import views.html.TaskListView

class ViewTaskListControllerSpec extends ControllerBaseSpec {

  val schemeDateRange: DateRange = DateRange.from(TaxYear(2023))

  "ViewTaskListController" - {

    lazy val viewModel = ViewTaskListController.viewModel(
      srn,
      schemeName,
      schemeDateRange.from,
      schemeDateRange.to
    )
    lazy val onPageLoad = routes.ViewTaskListController.onPageLoad(srn)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[TaskListView]
      view(viewModel)
    }.withName("task list renders OK"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

  }
}
