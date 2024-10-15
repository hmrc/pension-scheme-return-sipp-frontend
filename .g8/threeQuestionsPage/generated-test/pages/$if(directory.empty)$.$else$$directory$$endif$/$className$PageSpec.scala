package pages

import pages.behaviours.PageBehaviours
import models.*
$if(!index.empty)$
import config.RefinedTypes.*
$endif$

class $className$PageSpec extends PageBehaviours {

  "$className$Page" - {

    $if(!index.empty) $
    val index = refineUnsafe[Int, $index$.Refined](1)

    beRetrievable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srnGen.sample.value, index))

    beSettable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srnGen.sample.value, index))

    beRemovable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srnGen.sample.value, index))
    $else$
    beRetrievable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srnGen.sample.value))

    beSettable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srnGen.sample.value))

    beRemovable[($field1Type$, $field2Type$, $field3Type$)]($className;format="cap"$Page(srnGen.sample.value))
    $endif$
  }
}
