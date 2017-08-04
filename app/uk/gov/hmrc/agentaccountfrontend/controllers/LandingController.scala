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

package uk.gov.hmrc.agentaccountfrontend.controllers

import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentaccountfrontend.auth.AuthActions
import uk.gov.hmrc.agentaccountfrontend.config.AppConfig
import uk.gov.hmrc.agentaccountfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LandingController @Inject()(
  override val messagesApi: MessagesApi,
  override val config: Configuration,
  override val env: Environment,
  mappingConnector: MappingConnector)
  (implicit appConfig: AppConfig)
  extends FrontendController with I18nSupport with AuthActions {

  val root: Action[AnyContent] = AuthorisedWithAgentAsync {
    implicit request => implicit agentRequest =>
      Future successful Redirect(routes.LandingController.agentAccountLanding())
  }

  val agentAccountLanding: Action[AnyContent] = AuthorisedWithAgentAsync { implicit request => implicit agentRequest =>
    for {
      mapping <- hasMapping
      enrolment = hasMtdEnrolment
    } yield (enrolment, mapping) match {
      case (false, _) => Ok(uk.gov.hmrc.agentaccountfrontend.views.html.agent_account_page("One"))
      case (true, false) => Ok(uk.gov.hmrc.agentaccountfrontend.views.html.agent_account_page("Two"))
      case (true, true) => Ok(uk.gov.hmrc.agentaccountfrontend.views.html.agent_account_page("Three"))
    }
  }

  val goToErrorPage: Action[AnyContent] = Action {
    implicit request =>
      Ok(uk.gov.hmrc.agentaccountfrontend.views.html.error())
  }

  private def hasMtdEnrolment(implicit request: AgentRequest[_]): Boolean =
    request.enrolments.enrolments.exists(_.key == "HMRC-AS-AGENT")

  private def hasMapping(implicit request: AgentRequest[_], hc: HeaderCarrier): Future[Boolean] = {
    request.arn match {
      case None => Future successful false
      case Some(arn) =>
        for {
          status <- mappingConnector.hasMapping(Arn(arn))
        } yield status
    }
  }
}