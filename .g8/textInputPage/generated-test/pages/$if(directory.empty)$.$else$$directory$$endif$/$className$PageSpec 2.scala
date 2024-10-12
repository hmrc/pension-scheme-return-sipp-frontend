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

class $className;format="cap"$PageSpec extends PageBehaviours {

  "$className$Page" - {

    $if(!index.empty) $
    val index = refineUnsafe[Int, $index$.Refined](1)

    beRetrievable[String]($className;format="cap"$Page(srnGen.sample.value, index))

    beSettable[String]($className;format="cap"$Page(srnGen.sample.value, index))

    beRemovable[String]($className;format="cap"$Page(srnGen.sample.value, index))
    $else$
    beRetrievable[String]($className;format="cap"$Page(srnGen.sample.value))

    beSettable[String]($className;format="cap"$Page(srnGen.sample.value))

    beRemovable[String]($className;format="cap"$Page(srnGen.sample.value))
    $endif$
  }
}