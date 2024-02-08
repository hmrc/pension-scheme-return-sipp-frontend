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

import viewmodels.DisplayMessage.{InlineMessage, Message}

case class LoadingViewModel(
  title: Message,
  heading: InlineMessage,
  description: InlineMessage,
  refresh: Option[Int]
) {
  def refreshPage(refresh: Option[Int]): LoadingViewModel =
    copy(refresh = refresh)
}

object LoadingViewModel {
  def apply[A](
    title: Message,
    heading: InlineMessage,
    description: InlineMessage
  ): LoadingViewModel =
    LoadingViewModel(
      title,
      heading,
      description,
      refresh = None
    )
}
