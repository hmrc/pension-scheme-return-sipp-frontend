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

package pages

import models.{Journey, JourneyType}
import pages.behaviours.PageBehaviours

class RemoveFilePageSpec extends PageBehaviours {

  "RemoveFilePage" - {

    val srn = srnGen.sample.value

    Journey.values.foreach { journey =>
      s"must be retrievable - standard for journey: ${journey.entryName}" - {
        beRetrievable[TaskListStatusPage.Status](RemoveFilePage(srn, journey, JourneyType.Standard))
      }

      s"must be settable - standard for journey: ${journey.entryName}" - {
        beSettable[TaskListStatusPage.Status](RemoveFilePage(srn, journey, JourneyType.Standard))
      }

      s"must be removable - standard for journey: ${journey.entryName}" - {
        beRemovable[TaskListStatusPage.Status](RemoveFilePage(srn, journey, JourneyType.Standard))
      }

      s"must be retrievable - amend for journey: ${journey.entryName}" - {
        beRetrievable[TaskListStatusPage.Status](RemoveFilePage(srn, journey, JourneyType.Amend))
      }

      s"must be settable - amend for journey: ${journey.entryName}" - {
        beSettable[TaskListStatusPage.Status](RemoveFilePage(srn, journey, JourneyType.Amend))
      }

      s"must be removable - amend for journey: ${journey.entryName}" - {
        beRemovable[TaskListStatusPage.Status](RemoveFilePage(srn, journey, JourneyType.Amend))
      }
    }

  }
}
