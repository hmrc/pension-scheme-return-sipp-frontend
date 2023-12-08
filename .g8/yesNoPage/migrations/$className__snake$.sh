#!/bin/bash

set -e

$! Generic !$
echo ""
echo "Applying migration $className;format="snake"$"

$if(directory.empty)$
DIR=../conf/app.routes
PACKAGE="controllers"
$else$
DIR=../conf/$directory$.routes
PACKAGE="controllers.$directory$"
$endif$

echo "Adding routes to conf/app.routes"

echo -en "\n\n" >> ../conf/app.routes
$if(index.empty)$
echo "GET        /:srn/$urlPath$                        \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, mode: Mode = NormalMode)" >> \$DIR
echo "POST       /:srn/$urlPath$                        \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, mode: Mode = NormalMode)" >> \$DIR

echo "GET        /:srn/change-$urlPath$                 \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, mode: Mode = CheckMode)" >> \$DIR
echo "POST       /:srn/change-$urlPath$                 \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, mode: Mode = CheckMode)" >> \$DIR
$else$
  $if(secondaryIndex.empty)$
  echo "GET        /:srn/$urlPath$/:index                 \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, index: $index$, mode: Mode = NormalMode)" >> \$DIR
  echo "POST       /:srn/$urlPath$/:index                 \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, index: $index$, mode: Mode = NormalMode)" >> \$DIR

  echo "GET        /:srn/change-$urlPath$/:index          \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, index: $index$, mode: Mode = CheckMode)" >> \$DIR
  echo "POST       /:srn/change-$urlPath$/:index          \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, index: $index$, mode: Mode = CheckMode)" >> \$DIR
  $else$
  echo "GET        /:srn/$urlPath$/:index/:secondaryIndex                 \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, index: $index$, secondaryIndex: $secondaryIndex$, mode: Mode = NormalMode)" >> \$DIR
  echo "POST       /:srn/$urlPath$/:index/:secondaryIndex                 \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, index: $index$, secondaryIndex: $secondaryIndex$, mode: Mode = NormalMode)" >> \$DIR

  echo "GET        /:srn/change-$urlPath$/:index/:secondaryIndex          \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, index: $index$, secondaryIndex: $secondaryIndex$, mode: Mode = CheckMode)" >> \$DIR
  echo "POST       /:srn/change-$urlPath$/:index/:secondaryIndex          \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, index: $index$, secondaryIndex: $secondaryIndex$, mode: Mode = CheckMode)" >> \$DIR
  $endif$
$endif$
$! Generic end !$

echo "Adding messages to conf.messages"

echo -en "\n\n" >> ../conf/messages.en
echo "$className;format="decap"$.title = $title$" >> ../conf/messages.en
echo "$className;format="decap"$.heading = $heading$" >> ../conf/messages.en
echo "$className;format="decap"$.error.required = $errorRequired$"  >> ../conf/messages.en

$! Generic !$
DIR="$directory$"

if [ -z \$DIR ]; then
  echo "DIR empty, skipping"
else
  echo "Add to navigator"
  $if(index.empty)$
  ../.g8/scripts/updateNavigator $className;format="cap"$Page $directory$
  $else$
    $if(secondaryIndex.empty)$
    ../.g8/scripts/updateNavigator $className;format="cap"$Page $directory$ "$index$"
    $else$
    ../.g8/scripts/updateNavigator $className;format="cap"$Page $directory$ "$index$" "$secondaryIndex$"
    $endif$
  $endif$
fi

echo "Migration $className;format="snake"$ completed"
$! Generic end !$
