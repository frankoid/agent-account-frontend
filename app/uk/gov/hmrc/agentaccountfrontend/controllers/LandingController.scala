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

import javax.inject.{Inject, Singleton}

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentaccountfrontend.auth.AuthActions
import uk.gov.hmrc.agentaccountfrontend.config.AppConfig
import uk.gov.hmrc.agentaccountfrontend.config.FrontendAppConfig
import uk.gov.hmrc.agentaccountfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.passcode.authentication.{PasscodeAuthentication, PasscodeAuthenticationProvider, PasscodeVerificationConfig}
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.agentaccountfrontend.connectors.{AuthArnDetails, AuthenticationConnector}
import uk.gov.hmrc.agentaccountfrontend.service.AuthenticationService
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class LandingController @Inject()(
  override val messagesApi: MessagesApi,
  override val authConnector: AuthConnector,
  mappingConnector: MappingConnector,
  override val config: PasscodeVerificationConfig,
  override val passcodeAuthenticationProvider: PasscodeAuthenticationProvider,
  authenticationService: AuthenticationService)(implicit appConfig: FrontendAppConfig) extends FrontendController
  with I18nSupport
  with Actions
  with AuthActions
  with PasscodeAuthentication {

  val root: Action[AnyContent] = PasscodeAuthenticatedAction {
    implicit request =>
      Redirect(routes.LandingController.agentAccountLanding())
  }

  val agentAccountLanding: Action[AnyContent] = AuthorisedWithAgentAsync { implicit authContext => implicit request =>

    for {
      mapping <- hasMapping
      enrolment = hasMtdEnrolment
    } yield (enrolment, mapping) match {
      case (false, _) => Ok(uk.gov.hmrc.agentaccountfrontend.views.html.agent_account_page(1))
      case (true, false) => Ok(uk.gov.hmrc.agentaccountfrontend.views.html.agent_account_page(2))
      case (true, true) => Ok(uk.gov.hmrc.agentaccountfrontend.views.html.agent_account_page(3))
      case (_, _) => Ok(uk.gov.hmrc.agentaccountfrontend.views.html.agent_account_page(4))
    }
  }

  private def hasMtdEnrolment(implicit request: AgentRequest[_]): Boolean =
    request.enrolments.exists(_.key == "HMRC-AS-AGENT")

  private def hasMapping(implicit request: AgentRequest[_]): Future[Boolean] = {
    for {
      arn <- authenticationService.getArn()
      status <- mappingConnector.hasMapping(arn)
    } yield status
  }
}
