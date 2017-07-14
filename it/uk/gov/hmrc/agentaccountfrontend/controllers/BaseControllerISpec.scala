package uk.gov.hmrc.agentaccountfrontend.controllers

import com.google.inject.AbstractModule
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentaccountfrontend.support.WireMockSupport
import uk.gov.hmrc.play.test.UnitSpec


abstract class BaseControllerISpec extends UnitSpec with OneAppPerSuite with WireMockSupport {

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.agent-mapping.port" -> wireMockPort,
        "metrics.enabled" -> false
      )
      .overrides(new TestGuiceModule)
  }


  private class TestGuiceModule extends AbstractModule {
    override def configure(): Unit = {
    }
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
  }

  protected implicit val materializer = app.materializer
}
