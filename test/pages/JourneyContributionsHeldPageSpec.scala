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

class JourneyContributionsHeldPageSpec extends PageBehaviours {
  "JourneyContributionsHeldPage" - {

    val srn = srnGen.sample.value
    val journeyType = JourneyType.Standard

    Journey.values.foreach { journey =>
      s"must be retrievable for journey: ${journey.entryName}" - {
        beRetrievable[Boolean](JourneyContributionsHeldPage(srn, journey, journeyType))
      }

      s"must be settable for journey: ${journey.entryName}" - {
        beSettable[Boolean](JourneyContributionsHeldPage(srn, journey, journeyType))
      }

      s"must be removable for journey: ${journey.entryName}" - {
        beRemovable[Boolean](JourneyContributionsHeldPage(srn, journey, journeyType))
      }
    }
  }
}
