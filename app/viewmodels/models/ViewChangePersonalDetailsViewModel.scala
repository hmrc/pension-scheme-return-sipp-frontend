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

package viewmodels.models

import controllers.routes
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.ViewChangePersonalDetailsViewModel.ViewChangePersonalDetailsRowViewModel

case class ViewChangePersonalDetailsViewModel(
  memberName: String,
  rows: Seq[ViewChangePersonalDetailsRowViewModel]
)

object ViewChangePersonalDetailsViewModel {
  case class ViewChangePersonalDetailsRowViewModel(
    key: Message,
    value: String,
    changeUrl: String,
    visuallyHiddenText: String
  )

  def apply(
    srn: Srn,
    title: Message,
    heading: Message,
    memberName: String,
    ishHiddenSubmit: Boolean,
    rows: ViewChangePersonalDetailsRowViewModel*
  ): FormPageViewModel[ViewChangePersonalDetailsViewModel] =
    FormPageViewModel(
      title,
      heading,
      ViewChangePersonalDetailsViewModel(memberName, rows),
      routes.ViewChangePersonalDetailsController.onSubmit(srn)
    ).withSubmitVisibility(ishHiddenSubmit)

}
