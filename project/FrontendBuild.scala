import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object FrontendBuild extends Build with MicroService {

  val appName = "agent-account-frontend"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test() ++ test("it")
  
  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "frontend-bootstrap" % "7.25.0",
    "uk.gov.hmrc" %% "play-partials" % "5.3.0",
    "uk.gov.hmrc" %% "play-authorised-frontend" % "6.3.0",
    "uk.gov.hmrc" %% "play-config" % "4.3.0",
    "uk.gov.hmrc" %% "logback-json-logger" % "3.1.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.2.0",
    "uk.gov.hmrc" %% "play-health" % "2.1.0",
    "uk.gov.hmrc" %% "play-ui" % "7.4.0",
    "uk.gov.hmrc" %% "passcode-verification" % "4.1.0",
    "uk.gov.hmrc" %% "http-caching-client" % "6.2.0",
    "uk.gov.hmrc" %% "agent-mtd-identifiers" % "0.5.0"
  )

  def test(scope: String = "test") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "2.3.0" % scope,
    "org.scalatest" %% "scalatest" % "2.2.6" % scope,
    "com.github.tomakehurst" % "wiremock" % "2.3.1" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "org.jsoup" % "jsoup" % "1.8.1" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
  )

}
