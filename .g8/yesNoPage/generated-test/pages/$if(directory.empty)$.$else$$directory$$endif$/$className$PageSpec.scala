$if(directory.empty)$
package pages
$else$
package pages.$directory$
$endif$

import pages.behaviours.PageBehaviours
import models.Money
$if(!index.empty)$
import config.RefinedTypes.*
$endif$

$! Generic (change page type) !$
class $className$PageSpec extends PageBehaviours {

  "$className$Page" - {

    $if(!index.empty)$
    val index = refineUnsafe[Int, $index$.Refined](1)
    $if(!secondaryIndex.empty)$
    val secondaryIndex = refineUnsafe[Int, $secondaryIndex$.Refined](1)

    beRetrievable[Boolean]($className;format="cap"$Page(srnGen.sample.value, index, secondaryIndex))

    beSettable[Boolean]($className;format="cap"$Page(srnGen.sample.value, index, secondaryIndex))

    beRemovable[Boolean]($className;format="cap"$Page(srnGen.sample.value, index, secondaryIndex))
    $else$
    beRetrievable[Boolean]($className;format="cap"$Page(srnGen.sample.value, index))

    beSettable[Boolean]($className;format="cap"$Page(srnGen.sample.value, index))

    beRemovable[Boolean]($className;format="cap"$Page(srnGen.sample.value, index))
    $endif$
    $else$
    beRetrievable[Boolean]($className;format="cap"$Page(srnGen.sample.value))

    beSettable[Boolean]($className;format="cap"$Page(srnGen.sample.value))

    beRemovable[Boolean]($className;format="cap"$Page(srnGen.sample.value))
    $endif$
  }
}
$! Generic end !$