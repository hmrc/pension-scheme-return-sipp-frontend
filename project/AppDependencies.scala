import play.core.PlayVersion
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.5.0"
  private val hmrcMongoVersion = "2.2.0"
  val http4sVersion = "0.23.27"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                %% "play-frontend-hmrc-play-30"             % "10.13.0",
    "uk.gov.hmrc"                %% "play-conditional-form-mapping-play-30"  % "3.2.0",
    "uk.gov.hmrc"                %% "bootstrap-frontend-play-30"             % bootstrapVersion,
    "uk.gov.hmrc.mongo"          %% "hmrc-mongo-play-30"                     % hmrcMongoVersion,
    "uk.gov.hmrc"                %% "tax-year"                               % "5.0.0",
    "org.typelevel"              %% "cats-core"                              % "2.12.0",
    "eu.timepit"                 %% "refined"                                % "0.11.2",
    "uk.gov.hmrc"                %% "domain-play-30"                         % "10.0.0",
    "uk.gov.hmrc"                %% "crypto-json-play-30"                    % "8.1.0",
    "org.http4s"                 %% "http4s-ember-client"                    % http4sVersion,
    "org.http4s"                 %% "http4s-dsl"                             % http4sVersion,
    "org.http4s"                 %% "http4s-circe"                           % http4sVersion,
    "io.circe"                   %% "circe-generic"                          % "0.14.9",
    "org.gnieh"                  %% "fs2-data-csv"                           % "1.11.1",
    "org.gnieh"                  %% "fs2-data-csv-generic"                   % "1.11.1",
    "co.fs2"                     %% "fs2-reactive-streams"                   % "3.10.2",
    "com.beachape"               %% "enumeratum-play-json"                   % "1.8.1",
    "com.softwaremill.quicklens" %% "quicklens"                              % "1.9.7"
  )

  val test = Seq(
    "uk.gov.hmrc"                %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"          %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatest"              %% "scalatest"               % "3.2.19",
    "org.scalatestplus"          %% "scalacheck-1-18"         % "3.2.19.0",
    "org.scalatestplus.play"     %% "scalatestplus-play"      % "7.0.1",
    "org.pegdown"                %  "pegdown"                 % "1.6.0",
    "org.jsoup"                  %  "jsoup"                   % "1.18.1"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
