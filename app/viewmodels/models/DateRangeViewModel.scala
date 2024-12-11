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

import models.DateRange
import play.api.mvc.Call
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{InlineMessage, Message}

case class DateRangeViewModel(
  startDateLabel: Message,
  endDateLabel: Message,
  startDateHint: Option[Message] = None,
  endDateHint: Option[Message] = None,
  currentDateRanges: List[DateRange]
)

object DateRangeViewModel {

  def apply(
    title: Message,
    heading: InlineMessage,
    description: Option[DisplayMessage],
    currentDateRanges: List[DateRange],
    onSubmit: Call
  ): FormPageViewModel[DateRangeViewModel] =
    FormPageViewModel(
      title,
      heading,
      DateRangeViewModel(
        Message("site.startDate"),
        Message("site.endDate"),
        Some(Message("site.startDateHint")),
        Some(Message("site.endDateHint")),
        currentDateRanges
      ),
      onSubmit
    ).withDescription(description)
}
