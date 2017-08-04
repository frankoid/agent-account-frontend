package uk.gov.hmrc.agentaccountfrontend.stubs

import uk.gov.hmrc.domain.SaAgentReference
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}

object MappingStubs {

  def mappingIsFound(arn: Arn): Unit = {
    stubFor(get(urlPathEqualTo(s"/agent-mapping/mappings/${arn.value}"))
      willReturn aResponse().withStatus(200))
  }

  def mappingNotFound(arn: Arn): Unit = {
    stubFor(get(urlPathEqualTo(s"/agent-mapping/mappings/${arn.value}"))
      willReturn aResponse().withStatus(404))
  }

}