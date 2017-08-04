package uk.gov.hmrc.agentaccountfrontend.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentaccountfrontend.config.AppConfig
import uk.gov.hmrc.agentaccountfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentaccountfrontend.support.TestAppConfig
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future


class LandingControllerISpec extends UnitSpec with MockitoSugar with OneAppPerSuite with BeforeAndAfterEach {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockMappingConnector: MappingConnector = mock[MappingConnector]
  val mockPlayAuthConnector: PlayAuthConnector = mock[PlayAuthConnector]
  implicit val appConfig: AppConfig = TestAppConfig

  protected implicit val materializer = app.materializer

  lazy val arn = "TARN0000001"

  lazy val agentWithEnrolment = Set(
    Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), confidenceLevel = ConfidenceLevel.L200,
      state = "", delegatedAuthRule = None))

  def authorisedAsAgentMockWithEnrolments =
    when(mockPlayAuthConnector.authorise(any(), any[Retrieval[~[Enrolments, Option[AffinityGroup]]]]())(any()))
      .thenReturn(Future successful new ~[Enrolments, Option[AffinityGroup]](Enrolments(agentWithEnrolment), Some(AffinityGroup.Agent)))

  def authorisedAsAgentMockWithoutEnrolments =
    when(mockPlayAuthConnector.authorise(any(), any[Retrieval[~[Enrolments, Option[AffinityGroup]]]]())(any()))
      .thenReturn(Future successful new ~[Enrolments, Option[AffinityGroup]](Enrolments(Set()), Some(AffinityGroup.Agent)))

  lazy val controller: LandingController = new LandingController(messagesApi, config, env, mockMappingConnector)(appConfig) {
    override lazy val authConnector: PlayAuthConnector = mockPlayAuthConnector
  }

  private def config = app.injector.instanceOf[Configuration]

  private def env = app.injector.instanceOf[Environment]

  override def beforeEach() {
    reset(mockMappingConnector, mockPlayAuthConnector)
  }

  "Landing Controller" should {

    "show number 1 if user has no enrolment" in {

      authorisedAsAgentMockWithoutEnrolments
      val result = await(controller.agentAccountLanding().apply(FakeRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include("One")
    }

    "show number 2 if user has enrolment with no mapping " in {

      authorisedAsAgentMockWithEnrolments
      when(mockMappingConnector.hasMapping(any())(any(), any())).thenReturn(Future successful false)
      val result = await(controller.agentAccountLanding().apply(FakeRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include("Two")
    }

    "show number 3 if user has enrolment and mapping" in {

      authorisedAsAgentMockWithEnrolments
      when(mockMappingConnector.hasMapping(any())(any(), any())).thenReturn(Future successful true)

      val result = await(controller.agentAccountLanding().apply(FakeRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include("Three")
    }
  }

}
