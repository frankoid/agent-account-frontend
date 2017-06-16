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

package uk.gov.hmrc.agentaccountfrontend.connectors

import java.net.URL
import javax.inject.{Inject, Named, Singleton}

import play.api.http.Status
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MappingConnector @Inject()(@Named("agent-mapping-baseUrl") baseUrl: URL, http: HttpGet) {

  def hasMapping(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    http.GET(createUrl(arn))
      .map(_.status match {
        case Status.OK => true
        case _ => false
      })
      .recover {
        case _: NotFoundException => false
      }
  }

  private def createUrl(arn: Arn): String = {
    new URL(baseUrl, s"/agent-mapping/mappings/${arn.value}").toString
  }

}
