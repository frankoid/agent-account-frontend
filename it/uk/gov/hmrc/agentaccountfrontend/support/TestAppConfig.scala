package uk.gov.hmrc.agentaccountfrontend.support

import uk.gov.hmrc.agentaccountfrontend.config.AppConfig

object TestAppConfig extends AppConfig {

  override val analyticsToken: String = "N/A"
  override val analyticsHost: String = ""
  override val reportAProblemPartialUrl: String = ""
  override val reportAProblemNonJSUrl: String = ""
}
