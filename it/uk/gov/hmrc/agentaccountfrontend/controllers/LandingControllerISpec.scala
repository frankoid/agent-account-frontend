package uk.gov.hmrc.agentaccountfrontend.controllers

import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentaccountfrontend.stubs.AuthStub
import uk.gov.hmrc.agentaccountfrontend.stubs.MappingStubs
import uk.gov.hmrc.agentaccountfrontend.support.{SampleUser, SampleUsers}
import uk.gov.hmrc.agentaccountfrontend.support.SampleUsers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn


class LandingControllerISpec extends BaseControllerISpec {

  lazy val arn: Arn = Arn("AARN0000002")

"Landing Controller" should {

  lazy val controller: LandingController = app.injector.instanceOf[LandingController]

  "show number 1 if user has no enrolment" in {

    AuthStub.hasNoEnrolments(subscribingAgent)

    val result = await(controller.agentAccountLanding(authenticatedRequest()))
    status(result) shouldBe 303
    bodyOf(result) should include("1")
  }

  "show number 2 if user has enrolment with no mapping " in {

    AuthStub.isEnrolledForNonMtdServices(subscribingAgent)
    MappingStubs.mappingNotFound(arn)

    val result = await(controller.agentAccountLanding(authenticatedRequest()))
    status(result) shouldBe 303
    bodyOf(result) should include("2")


  }

  "show number 3 if user has enrolement and mapping" in {

    AuthStub.isEnrolledForNonMtdServices(subscribingAgent)

    MappingStubs.mappingIsFound(arn)
    val result = await(controller.agentAccountLanding(authenticatedRequest()))
    status(result) shouldBe 303
    bodyOf(result) should include("3")


  }
}

}
