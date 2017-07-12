package uk.gov.hmrc.agentaccountfrontend.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import uk.gov.hmrc.agentaccountfrontend.config.AppConfig
import uk.gov.hmrc.agentaccountfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentaccountfrontend.support.TestAppConfig
import uk.gov.hmrc.auth.core._

import scala.concurrent.Future


class LandingControllerISpec extends BaseControllerISpec with MockitoSugar with BeforeAndAfterEach {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockMappingConnector: MappingConnector = mock[MappingConnector]
  val mockPlayAuthConnector: PlayAuthConnector = mock[PlayAuthConnector]
  implicit val appConfig: AppConfig = TestAppConfig

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

  lazy val controller: LandingController = new LandingController(messagesApi, mockMappingConnector, fakeApplication())(appConfig) {
    override lazy val authConnector: PlayAuthConnector = mockPlayAuthConnector
  }

  override def beforeEach() {
    reset(mockMappingConnector, mockPlayAuthConnector)
  }

  "Landing Controller" should {

    "show number 1 if user has no enrolment" in {

      authorisedAsAgentMockWithoutEnrolments
      val result = await(controller.agentAccountLanding().apply(FakeRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include("1")
    }

    "show number 2 if user has enrolment with no mapping " in {

      authorisedAsAgentMockWithEnrolments
      when(mockMappingConnector.hasMapping(any())(any(), any())).thenReturn(Future successful false)
      val result = await(controller.agentAccountLanding().apply(FakeRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include("2")
    }

    "show number 3 if user has enrolment and mapping" in {

      authorisedAsAgentMockWithEnrolments
      when(mockMappingConnector.hasMapping(any())(any(), any())).thenReturn(Future successful true)

      val result = await(controller.agentAccountLanding().apply(FakeRequest()))
      status(result) shouldBe 200
      bodyOf(result) should include("3")
    }
  }

}
