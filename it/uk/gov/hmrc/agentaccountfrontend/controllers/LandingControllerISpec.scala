package uk.gov.hmrc.agentaccountfrontend.controllers

import akka.stream.Materializer
import org.scalatest.{BeforeAndAfterEach, MustMatchers}
import org.scalatest.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentaccountfrontend.stubs.AuthStub
import uk.gov.hmrc.agentaccountfrontend.stubs.MappingStubs
import uk.gov.hmrc.agentaccountfrontend.support.{SampleUser, SampleUsers}
import uk.gov.hmrc.agentaccountfrontend.support.SampleUsers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.agentaccountfrontend.config.AppConfig
import uk.gov.hmrc.agentaccountfrontend.connectors.MappingConnector
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future


class LandingControllerISpec extends BaseControllerISpec with MockitoSugar with BeforeAndAfterEach {

 // implicit lazy val materializer = app.materializer

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockMappingConnector: MappingConnector = mock[MappingConnector]
  val mockPlayAuthConnector: PlayAuthConnector = mock[PlayAuthConnector]
  val mockAppConfg: AppConfig = mock[AppConfig]


  lazy val arn = "TARN0000001"

  lazy val agentWithEnrolment = Set(
    Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), confidenceLevel = ConfidenceLevel.L200,
      state = "", delegatedAuthRule = None))

  lazy val agentWithoutEnrolment = Set(
    Enrolment("", Seq(), confidenceLevel = ConfidenceLevel.L200,
      state = "", delegatedAuthRule = None))

  def authorisedAsAgentMockWithEnrolments() =
    when(mockPlayAuthConnector.authorise(any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(any()))
      .thenReturn(Future successful new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent), Enrolments(agentWithEnrolment)))

  def authorisedAsAgentMockWithoutEnrolments() =
    when(mockPlayAuthConnector.authorise(any(), any[Retrieval[~[Option[AffinityGroup], Enrolments]]]())(any()))
      .thenReturn(Future successful new ~[Option[AffinityGroup], Enrolments](Some(AffinityGroup.Agent), Enrolments(agentWithoutEnrolment)))


  lazy val controller: LandingController = new LandingController(messagesApi, mockMappingConnector, fakeApplication())(mockAppConfg){
    override lazy val authConnector: PlayAuthConnector = mockPlayAuthConnector
  }

  override def beforeEach() {
    reset(mockMappingConnector, mockPlayAuthConnector)
  }
 private lazy val configuredGovernmentGatewayUrl = "http://configured-government-gateway.gov.uk/"

override protected def appBuilder: GuiceApplicationBuilder = super.appBuilder
   .configure("government-gateway.url" -> configuredGovernmentGatewayUrl)


 "Landing Controller" should {


   "show number 1 if user has no enrolment" in {

     authorisedAsAgentMockWithoutEnrolments()
     val result = await(controller.agentAccountLanding().apply(FakeRequest()))
     status(result) shouldBe 200
     bodyOf(result) should contain("1")
   }

   "show number 2 if user has enrolment with no mapping " in {

     authorisedAsAgentMockWithEnrolments()
     when(mockMappingConnector.hasMapping(any())(any(), any())).thenReturn(Future successful false)

     val result = await(controller.agentAccountLanding().apply(FakeRequest()))
     status(result) shouldBe 200
     bodyOf(result) should contain("2")
   }

   "show number 3 if user has enrolment and mapping" in {

     authorisedAsAgentMockWithEnrolments()
     when(mockMappingConnector.hasMapping(Arn(arn))(any(), any())).thenReturn(Future successful true)

     val result = await(controller.agentAccountLanding().apply(FakeRequest()))
     status(result) shouldBe 200
     bodyOf(result) should contain("3")


   }
 }

}
