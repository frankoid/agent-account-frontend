/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package auth

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.{Configuration, Environment, Logger}
import play.api.mvc.Result
import uk.gov.hmrc.agentaccountfrontend.auth.AuthActions
import uk.gov.hmrc.auth.core.{MissingBearerToken, NoActiveSession, _}
import play.api.mvc.Results._
import play.api.test.{FakeRequest, ResultExtractors}
import support.ResettingMockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import play.api.mvc.Results.Ok
import uk.gov.hmrc.agentaccountfrontend.controllers.routes
import uk.gov.hmrc.agentaccountfrontend.config.GGConfig.ggSignInUrl
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class AuthActionsSpec extends UnitSpec with OneAppPerSuite with MockitoSugar {

  lazy val mockAuthConnector = mock[PlayAuthConnector]

  val agentEnrolment = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", "TARN0000001")), confidenceLevel = ConfidenceLevel.L200,
    state = "", delegatedAuthRule = None)

  def mockAuth(affinityGroup: AffinityGroup = AffinityGroup.Agent, enrolment: Set[Enrolment]) = when(mockAuthConnector.authorise(any(), any[Retrieval[~[Enrolments, Option[AffinityGroup]]]]())(any()))
    .thenReturn(Future successful new ~[Enrolments, Option[AffinityGroup]](Enrolments(enrolment), Some(affinityGroup)))

  implicit val hc: HeaderCarrier = HeaderCarrier()


  class TestAuth() extends AuthActions {
    def testAuthActions() = AuthorisedWithAgentAsync {
      implicit request =>
        implicit agentRequest =>
          Future.successful(Ok)
    }

    def testAuthActionsRequest = authorisedWithAgent {
      agentRequest =>
        Future.successful(agentRequest)
    }


    override def authConnector: AuthConnector = mockAuthConnector

    override def config: Configuration = app.injector.instanceOf[Configuration]

    override def env: Environment = app.injector.instanceOf[Environment]
  }

  val testAuthImpl = new TestAuth()

  "AuthorisedWithAgentAsync" should {
    "return an agent request" in {
      mockAuth(enrolment = Set(agentEnrolment))
      val result: Future[Result] = testAuthImpl.testAuthActions().apply(FakeRequest())
      await(result) shouldBe Ok
    }

    "redirect to GG if agent is not logged in" in {
      when(mockAuthConnector.authorise(any(), any[Retrieval[~[Enrolments, Option[AffinityGroup]]]]())(any()))
        .thenReturn(Future.failed(new MissingBearerToken))

      val result: Future[Result] = testAuthImpl.testAuthActions().apply(FakeRequest())
      status(result) shouldBe 303
      await(result).header.headers("Location") shouldBe "/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9025%2Fgg%2Fsign-in&origin=agent-account-frontend"
    }

    "redirect to root if GG is not agent" in {
      mockAuth(enrolment = Set(), affinityGroup = AffinityGroup.Individual)
      val result: Future[Result] = testAuthImpl.testAuthActions().apply(FakeRequest())
      status(result) shouldBe 303
      await(result).header.headers("Location") shouldBe routes.LandingController.root().url
    }
  }



  "authorisedWithAgent" should {
    "return an Agent enrolments details when authenticated user is an Agent" in {
      mockAuth(affinityGroup = AffinityGroup.Agent, enrolment = Set(agentEnrolment))

      await(testAuthImpl.testAuthActionsRequest).value.enrolments shouldBe Enrolments(Set(agentEnrolment))
    }


    "return None if authenticated user is not an Agent" in {
      mockAuth(affinityGroup = AffinityGroup.Individual, enrolment = Set(agentEnrolment))


      val result = testAuthImpl.testAuthActionsRequest
      await(result) shouldBe None
    }
  }

  // disable liftFuture implicit to avoid ambiguous implicit conversion when using .value from OptionValue
  override def liftFuture[A](v: A): Future[A] = super.liftFuture(v)
}
