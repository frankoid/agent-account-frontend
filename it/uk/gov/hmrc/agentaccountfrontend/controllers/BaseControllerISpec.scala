package uk.gov.hmrc.agentaccountfrontend.controllers

import com.google.inject.AbstractModule
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentType, _}
import uk.gov.hmrc.agentaccountfrontend.support.WireMockSupport
import uk.gov.hmrc.play.test.UnitSpec


abstract class BaseControllerISpec extends UnitSpec with OneAppPerSuite with WireMockSupport  {

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.agent-subscription.port" -> wireMockPort,
        "passcodeAuthentication.enabled" -> passcodeAuthenticationEnabled
      )
      .overrides(new TestGuiceModule)
  }

  protected def passcodeAuthenticationEnabled: Boolean = false


  private class TestGuiceModule extends AbstractModule {
    override def configure(): Unit = {
    }
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
  }

  protected implicit val materializer = app.materializer

  protected def checkHtmlResultWithBodyText(result: Result, expectedSubstrings: String*): Unit = {
    status(result) shouldBe OK
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")
    expectedSubstrings.foreach(s => bodyOf(result) should include(s))
  }
}
