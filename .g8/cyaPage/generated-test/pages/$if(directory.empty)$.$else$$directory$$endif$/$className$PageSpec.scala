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

$! Generic !$
$if(directory.empty)$
package pages
$else$
package pages.$directory$
$endif$

$if(directory.empty)$
import pages.$className$Page
$else$
import pages.$directory$.$className$Page
$endif$

$if(!index.empty)$
import config.RefinedTypes.*
$endif$

import pages.behaviours.PageBehaviours
$! Generic end !$

import viewmodels.models.SectionCompleted

$! Generic (change page type) !$
class $className$PageSpec extends PageBehaviours {

  "$className$Page" - {

    $if(!index.empty)$
    val index = refineUnsafe[Int, $index$.Refined](1)
    $if(!secondaryIndex.empty)$
    val secondaryIndex = refineUnsafe[Int, $secondaryIndex$.Refined](1)

    beRetrievable[SectionCompleted.type]($className;format="cap"$Page(srnGen.sample.value, index, secondaryIndex))

    beSettable[SectionCompleted.type]($className;format="cap"$Page(srnGen.sample.value, index, secondaryIndex))

    beRemovable[SectionCompleted.type]($className;format="cap"$Page(srnGen.sample.value, index, secondaryIndex))
    $else$
    beRetrievable[SectionCompleted.type]($className;format="cap"$Page(srnGen.sample.value, index))

    beSettable[SectionCompleted.type]($className;format="cap"$Page(srnGen.sample.value, index))

    beRemovable[SectionCompleted.type]($className;format="cap"$Page(srnGen.sample.value, index))
    $endif$
    $else$
    beRetrievable[SectionCompleted.type]($className;format="cap"$Page(srnGen.sample.value))

    beSettable[SectionCompleted.type]($className;format="cap"$Page(srnGen.sample.value))

    beRemovable[SectionCompleted.type]($className;format="cap"$Page(srnGen.sample.value))
    $endif$
  }
}
$! Generic end !$
