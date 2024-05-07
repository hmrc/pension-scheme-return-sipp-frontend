import play.core.PlayVersion
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "8.5.0"
  private val hmrcMongoVersion = "1.8.0"
  val http4sVersion = "0.23.26"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"        %% "play-frontend-hmrc-play-30"             % "8.5.0",
    "uk.gov.hmrc"        %% "play-conditional-form-mapping-play-30"  % "2.0.0",
    "uk.gov.hmrc"        %% "bootstrap-frontend-play-30"             % bootstrapVersion,
    "uk.gov.hmrc.mongo"  %% "hmrc-mongo-play-30"                     % hmrcMongoVersion,
    "uk.gov.hmrc"        %% "tax-year"                               % "3.0.0",
    "org.typelevel"      %% "cats-core"                              % "2.10.0",
    "eu.timepit"         %% "refined"                                % "0.11.1",
    "uk.gov.hmrc"        %% "domain-play-30"                         % "9.0.0",
    "uk.gov.hmrc"        %% "crypto-json-play-30"                    % "7.6.0",
    "org.http4s"         %% "http4s-ember-client"                    % http4sVersion,
    "org.http4s"         %% "http4s-dsl"                             % http4sVersion,
    "org.http4s"         %% "http4s-circe"                           % http4sVersion,
    "io.circe"           %% "circe-generic"                          % "0.14.6",
    "org.gnieh"          %% "fs2-data-csv"                           % "1.11.0",
    "org.gnieh"          %% "fs2-data-csv-generic"                   % "1.11.0",
    "co.fs2"             %% "fs2-reactive-streams"                   % "3.9.4"
  )

  val test = Seq(
    "uk.gov.hmrc"                %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"          %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatest"              %% "scalatest"               % "3.2.10",
    "org.scalatestplus"          %% "scalacheck-1-17"         % "3.2.15.0",
    "org.scalatestplus"          %% "mockito-4-6"             % "3.2.15.0",
    "org.scalatestplus.play"     %% "scalatestplus-play"      % "7.0.1",
    "org.pegdown"                %  "pegdown"                 % "1.6.0",
    "org.jsoup"                  %  "jsoup"                   % "1.17.2",
    "org.mockito"                %% "mockito-scala"           % "1.17.31",
    "org.scalacheck"             %% "scalacheck"              % "1.17.0",
    "com.softwaremill.quicklens" %% "quicklens"               % "1.9.7"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
