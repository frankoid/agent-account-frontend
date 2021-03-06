package uk.gov.hmrc.agentaccountfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentaccountfrontend.support.{SampleUser, SessionKeysForTesting}
import uk.gov.hmrc.play.http.SessionKeys

object AuthStub {
  def userIsNotAuthenticated(): Unit = {
    stubFor(get(urlEqualTo("/auth/authority")).willReturn(aResponse().withStatus(401)))
  }

  def userIsAuthenticated(user: SampleUser): Seq[(String, String)] = {
    stubFor(get(urlEqualTo("/auth/authority")).willReturn(aResponse().withStatus(200).withBody(user.authJson)))
    stubFor(get(urlMatching("/auth/oid/[^/]+$")).willReturn(aResponse().withStatus(200).withBody(user.authJson)))
    stubFor(get(urlEqualTo(user.userDetailsLink)).willReturn(aResponse().withStatus(200).withBody(user.userDetailsJson)))

    sessionKeysForMockAuth(user)
  }

  private def sessionKeysForMockAuth(user: SampleUser): Seq[(String, String)] = Seq(
    SessionKeys.userId -> user.authorityUri,
    SessionKeysForTesting.token -> "fakeToken")

  def isNotEnrolled(user: SampleUser): Unit = {
    stubFor(get(urlEqualTo(user.enrolmentsLink)).willReturn(aResponse().withStatus(200).withBody("[]")))
  }

  def isEnrolled(user: SampleUser, state: String = "Activated"): Unit = {
    stubFor(get(urlEqualTo(user.enrolmentsLink)).willReturn(aResponse().withStatus(200).withBody(
      s"""|[{"key":"IR-SA-AGENT","identifiers":[{"key":"IrAgentReference","value":"${user.saAgentReference.get}"}],"state":"$state"}]""".stripMargin)))
  }

  def passcodeAuthorisationSucceeds(regime: String = "agent-mapping", otacToken: String = "dummy-otac-token"): Seq[(String, String)] = {
    stubPasscodeAuthorisation(regime, 200)

    Seq(SessionKeys.otacToken -> otacToken)
  }

  def passcodeAuthorisationFails(regime: String = "agent-mapping"): Unit = {
    stubPasscodeAuthorisation(regime, 403)
  }

  private def stubPasscodeAuthorisation(regime: String, status: Int) = {
    stubFor(get(urlEqualTo(s"/authorise/read/$regime"))
      .willReturn(
        aResponse()
          .withStatus(status)))
  }
}
