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

package viewmodels.models

import play.api.mvc.Call
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.Message

case class FurtherDetailsViewModel(title: Message, contents: DisplayMessage)

case class YesNoPageViewModel(
  legend: Option[Message] = None,
  hint: Option[Message] = None,
  yes: Option[Message] = None,
  no: Option[Message] = None,
  details: Option[FurtherDetailsViewModel] = None,
  legendAsHeading: Boolean = false
) {

  def withHint(message: Message): YesNoPageViewModel =
    copy(hint = Some(message))
}

object YesNoPageViewModel {

  def apply(title: Message, heading: Message, onSubmit: Call): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      title,
      heading,
      YesNoPageViewModel(),
      onSubmit
    )

  def apply(title: Message, heading: Message, legend: Message, onSubmit: Call): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      title,
      heading,
      YesNoPageViewModel(legend = Some(legend)),
      onSubmit
    )

  def apply(
    title: Message,
    heading: Message,
    furtherDetailsViewModel: Option[FurtherDetailsViewModel],
    onSubmit: Call
  ): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      title,
      heading,
      YesNoPageViewModel(),
      furtherDetailsViewModel,
      onSubmit
    )
}
