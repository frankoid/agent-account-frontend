package uk.gov.hmrc.agentaccountfrontend.connectors

import uk.gov.hmrc.agentaccountfrontend.controllers.BaseControllerISpec
import uk.gov.hmrc.agentaccountfrontend.stubs.MappingStubs._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global


class MappingConnectorISpec extends BaseControllerISpec {

  private val arn = Arn("ARN0001")
  private def connector = app.injector.instanceOf[MappingConnector]
  private implicit val hc = HeaderCarrier()

  "createMapping" should {
    "mapping is found" in {
      mappingIsFound(arn)
      await(connector.hasMapping(arn)) shouldBe true
    }

    "does not find an existing mapping store" in {
      mappingNotFound(arn)
      await(connector.hasMapping(arn)) shouldBe false
    }
  }
}
