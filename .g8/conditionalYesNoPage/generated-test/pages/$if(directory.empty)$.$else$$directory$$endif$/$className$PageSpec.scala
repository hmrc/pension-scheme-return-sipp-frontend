$if(directory.empty)$
package pages
$else$
package pages.$directory$
$endif$

import pages.behaviours.PageBehaviours
import models.*
$if(!index.empty)$
import config.RefinedTypes.*
$endif$

class $className;format="cap"$PageSpec extends PageBehaviours {

  "$className$Page" - {

    $if(!index.empty) $
    val index = refineUnsafe[Int, $index$.Refined](1)

    beRetrievable[ConditionalYesNo[String, String]]($className;format="cap"$Page(srnGen.sample.value, index))

    beSettable[ConditionalYesNo[String, String]]($className;format="cap"$Page(srnGen.sample.value, index))

    beRemovable[ConditionalYesNo[String, String]]($className;format="cap"$Page(srnGen.sample.value, index))
    $else$
    beRetrievable[ConditionalYesNo[String, String]]($className;format="cap"$Page(srnGen.sample.value))

    beSettable[ConditionalYesNo[String, String]]($className;format="cap"$Page(srnGen.sample.value))

    beRemovable[ConditionalYesNo[String, String]]($className;format="cap"$Page(srnGen.sample.value))
    $endif$
  }
}