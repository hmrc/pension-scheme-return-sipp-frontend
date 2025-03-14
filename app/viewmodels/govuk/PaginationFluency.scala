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

package viewmodels.govuk

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.*

object pagination extends PaginationFluency

trait PaginationFluency {

  object PaginationViewModel {
    def apply(pagination: models.Pagination)(implicit
      messages: Messages
    ): Pagination = {
      import pagination.*

      val showPreviousPageLink: Boolean = currentPage > 1
      val showNextPageLink: Boolean = (currentPage * pageSize) < totalSize
      val pageItems: List[PaginationItem] =
        List.tabulate(totalPages) { i =>
          val arrayIndex = i + 1
          PaginationItem(
            href = call(arrayIndex).url,
            number = Some(arrayIndex.toString),
            current = Option.when(arrayIndex == currentPage)(true)
          )
        }

      Pagination(
        items = Option.when(totalSize > pageSize)(pageItems),
        next = Option.when(showNextPageLink)(PaginationLink(call(currentPage + 1).url, Some(messages("site.next")))),
        previous =
          Option.when(showPreviousPageLink)(PaginationLink(call(currentPage - 1).url, Some(messages("site.previous"))))
      )
    }
  }
}
