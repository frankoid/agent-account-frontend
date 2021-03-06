package uk.gov.hmrc.agentaccountfrontend.support

import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentaccountfrontend.controllers.routes
import uk.gov.hmrc.agentaccountfrontend.stubs.AuthStub
import uk.gov.hmrc.agentaccountfrontend.support.SampleUsers.{individual, subscribingAgent}
import uk.gov.hmrc.play.test.UnitSpec

trait EndpointBehaviours {
  me: UnitSpec with WireMockSupport with OneAppPerSuite =>
  type PlayRequest = Request[AnyContent] => Result
  private implicit val materializer = app.materializer

  protected def authenticatedRequest(): FakeRequest[AnyContentAsEmpty.type]

  protected def anAgentAffinityGroupOnlyEndpoint(doRequest: PlayRequest): Unit = {
    "redirect to the Agent next steps page if the current user is logged in and does have affinity group = Agent" in {
      val sessionKeys = AuthStub.userIsAuthenticated(subscribingAgent)

      val request = FakeRequest().withSession(sessionKeys: _*)
      val result = await(doRequest(request))

      status(result) shouldBe 303
     // redirectLocation(result).get shouldBe routes.LandingController.agentAccountLanding().url
    }
  }

  protected def aPageWithFeedbackLinks(action: PlayRequest, request: => Request[AnyContent] = FakeRequest()): Unit = {

  /*  "have a 'get help with this page' link" in {
      val result = await(action(request))

      bodyOf(result) should include("Get help with this page.")
    }

    "have a beta feedback banner" in {
      val result = await(action(request))

      bodyOf(result) should include("This is a new service")
    }

    "have a beta feedback link" in {
      val result = await(action(request))

      bodyOf(result) should include("/contact/beta-feedback")
    }*/
  }

  protected def aWhitelistedEndpoint(doRequest: PlayRequest): Unit = {
    "prevent access if passcode authorisation fails" in {
      AuthStub.isNotEnrolled(subscribingAgent)

      AuthStub.passcodeAuthorisationFails()

      val request = authenticatedRequest()
      val result = await(doRequest(request))

      status(result) shouldBe 303
      result.header.headers("Location") should include("verification/otac")
    }

    "allow access if passcode authorisation succeeds" in {
      AuthStub.isNotEnrolled(subscribingAgent)

      val sessionKeys = AuthStub.passcodeAuthorisationSucceeds()

      val request = authenticatedRequest().withSession(sessionKeys: _*)
      val result = await(doRequest(request))

      redirectLocation(result) match {
        case Some(location) => location should not include "verification/otac"
        case None =>
      }
    }
  }
}
