#!/bin/bash

set -e

echo ""
echo "Applying migration $className;format="snake"$"

echo "Adding routes to conf/app.routes"

$if(directory.empty)$
DIR=../conf/app.routes
PACKAGE="controllers"
$else$
DIR=../conf/$directory$.routes
PACKAGE="controllers.$directory$"
$endif$

echo -en "\n\n" >> \$DIR
$if(index.empty)$
echo "GET        /:srn/$urlPath$                        \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, mode: Mode = NormalMode)" >> \$DIR
echo "POST       /:srn/$urlPath$                        \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, mode: Mode = NormalMode)" >> \$DIR

echo "GET        /:srn/change-$urlPath$                 \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, mode: Mode = CheckMode)" >> \$DIR
echo "POST       /:srn/change-$urlPath$                 \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, mode: Mode = CheckMode)" >> \$DIR
$else$
echo "GET        /:srn/$urlPath$/:index                 \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, index: $index$, mode: Mode = NormalMode)" >> \$DIR
echo "POST       /:srn/$urlPath$/:index                 \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, index: $index$, mode: Mode = NormalMode)" >> \$DIR

echo "GET        /:srn/change-$urlPath$/:index          \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, index: $index$, mode: Mode = CheckMode)" >> \$DIR
echo "POST       /:srn/change-$urlPath$/:index          \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, index: $index$, mode: Mode = CheckMode)" >> \$DIR
$endif$

echo "Adding messages to conf.messages"

echo -en "\n\n" >> ../conf/messages.en
echo "$className;format="decap"$.title = $title$" >> ../conf/messages.en
echo "$className;format="decap"$.heading = $heading$" >> ../conf/messages.en
$if(!hint.empty)$
echo "$className;format="decap"$.hint = $hint$" >> ../conf/messages.en
$endif$
echo "$className;format="decap"$.error.required = $errorRequired$" >> ../conf/messages.en
echo "$className;format="decap"$.yes.conditional = $yesHeading$"  >> ../conf/messages.en
echo "$className;format="decap"$.yes.conditional.error.required = $yesErrorRequired$"  >> ../conf/messages.en
echo "$className;format="decap"$.yes.conditional.error.invalid = $yesErrorInvalid$"  >> ../conf/messages.en
echo "$className;format="decap"$.yes.conditional.error.length = $yesErrorLength$"  >> ../conf/messages.en
echo "$className;format="decap"$.no.conditional = $noHeading$"  >> ../conf/messages.en
echo "$className;format="decap"$.no.conditional.error.required = $noErrorRequired$"  >> ../conf/messages.en
echo "$className;format="decap"$.no.conditional.error.invalid = $noErrorInvalid$"  >> ../conf/messages.en
echo "$className;format="decap"$.no.conditional.error.length = $noErrorLength$"  >> ../conf/messages.en

echo "Add to navigator"

echo "case $className;format="cap"$Page(srn) => controllers.routes.UnauthorisedController.onPageLoad()"

echo "Migration $className;format="snake"$ completed"
