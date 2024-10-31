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
import services.view.TaskListViewModelService.ViewMode
import viewmodels.DisplayMessage.*
import viewmodels.implicits.*
import viewmodels.models.TaskListSectionViewModel.TaskListItem
import viewmodels.models.TaskListStatus.TaskListStatus

case class TaskListSectionViewModel(
  title: Message,
  items: NonEmptyList[TaskListItem],
  postActionLink: Option[LinkMessage]
)

object TaskListSectionViewModel {
  sealed trait TaskListItem
  case class MessageTaskListItem(inlineMessage: InlineMessage, hint: Option[Message]) extends TaskListItem

  case class TaskListItemViewModel(
    link: InlineMessage,
    hint: Option[Message],
    status: TaskListStatus
  ) extends TaskListItem

  object TaskListItemViewModel {
    def apply(link: InlineMessage, status: TaskListStatus): TaskListItemViewModel =
      TaskListItemViewModel(link, hint = None, status)
  }

  def apply(
    title: Message,
    headItem: TaskListItemViewModel,
    tailItems: TaskListItemViewModel*
  ): TaskListSectionViewModel =
    TaskListSectionViewModel(title, NonEmptyList(headItem, tailItems.toList), None)

  def apply(
    title: Message,
    item: TaskListItemViewModel
  ): TaskListSectionViewModel =
    TaskListSectionViewModel(title, NonEmptyList.one(item), None)

  def apply(
    title: Message,
    item: InlineMessage,
    postActionLink: LinkMessage
  ): TaskListSectionViewModel =
    TaskListSectionViewModel(title, NonEmptyList.one(MessageTaskListItem(item, None)), Some(postActionLink))

  implicit class TaskListOps(val taskListSectionViewModel: TaskListSectionViewModel) extends AnyVal {
    def taskListViewItems: List[TaskListItemViewModel] = taskListSectionViewModel.items.collect {
      case t: TaskListItemViewModel => t
    }
  }
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

  sealed abstract class TaskListStatus(val description: Message, val viewMode: ViewMode = ViewMode.View)

  case class UnableToStart(override val viewMode: ViewMode) extends TaskListStatus("tasklist.unableToStart", viewMode)

  case class NotStarted(override val viewMode: ViewMode) extends TaskListStatus("tasklist.notStarted", viewMode)

  case object InProgress extends TaskListStatus("tasklist.inProgress")

  case class Completed(override val viewMode: ViewMode) extends TaskListStatus("tasklist.completed", viewMode)

  case class Updated(override val viewMode: ViewMode) extends TaskListStatus("tasklist.updated", viewMode)

  case object CompletedWithoutUpload extends TaskListStatus("tasklist.completedWithoutUpload")
}
