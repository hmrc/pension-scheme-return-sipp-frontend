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

import cats.data.NonEmptyList
import viewmodels.DisplayMessage._
import viewmodels.implicits._
import viewmodels.models.TaskListStatus.TaskListStatus

case class TaskListItemViewModel(
  link: InlineMessage,
  status: TaskListStatus
)

case class TaskListSectionViewModel(
  title: Message,
  items: Either[InlineMessage, NonEmptyList[TaskListItemViewModel]],
  postActionLink: Option[LinkMessage]
)

object TaskListSectionViewModel {

  def apply(
    title: Message,
    headItem: TaskListItemViewModel,
    tailItems: TaskListItemViewModel*
  ): TaskListSectionViewModel =
    TaskListSectionViewModel(title, Right(NonEmptyList(headItem, tailItems.toList)), None)

  def apply(
    title: Message,
    item: InlineMessage,
    postActionLink: LinkMessage
  ): TaskListSectionViewModel =
    TaskListSectionViewModel(title, Left(item), Some(postActionLink))
}

case class TaskListViewModel(
  sections: NonEmptyList[TaskListSectionViewModel],
  postActionLink: Option[LinkMessage] = None
)

object TaskListViewModel {

  def apply(
    headItem: TaskListSectionViewModel,
    tailItems: TaskListSectionViewModel*
  ): TaskListViewModel =
    TaskListViewModel(NonEmptyList(headItem, tailItems.toList))
}

object TaskListStatus {

  sealed abstract class TaskListStatus(val description: Message)

  case object UnableToStart extends TaskListStatus("tasklist.unableToStart")

  case object NotStarted extends TaskListStatus("tasklist.notStarted")

  case object InProgress extends TaskListStatus("tasklist.inProgress")

  case object Completed extends TaskListStatus("tasklist.completed")

  case object CompletedWithoutUpload extends TaskListStatus("tasklist.completedWithoutUpload")
}
