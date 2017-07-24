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

package uk.gov.hmrc.agentaccountfrontend.auth

import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.agentaccountfrontend.config.{FrontendAppConfig, FrontendAuthConnector}
import uk.gov.hmrc.agentaccountfrontend.controllers.routes
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.Retrievals._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.frontend.Redirects
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait AuthActions extends AuthorisedFunctions with Redirects {

  case class AgentInfo(enrolments: Enrolments, arn: Option[String])
  case class AgentRequest[A](enrolments: Enrolments, arn: Option[String], request: Request[A]) extends WrappedRequest[A](request)

  override def authConnector: AuthConnector = new FrontendAuthConnector

  lazy val frontendAppConfig = new FrontendAppConfig

  protected type AsyncPlayUserRequest = Request[AnyContent] => AgentRequest[AnyContent] => Future[Result]

  private implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  private def getArn(enrolment: Set[Enrolment]) =
    enrolment.find(_.key equals "HMRC-AS-AGENT").flatMap(_.identifiers.find(_.key equals "AgentReferenceNumber").map(_.value))

  def AuthorisedWithAgentAsync(body: AsyncPlayUserRequest): Action[AnyContent] = {
    Action.async { implicit request =>
      authorisedWithAgent[Result] { agentInfo =>
        body(request)(AgentRequest(agentInfo.enrolments, agentInfo.arn, request))
      } map { maybeResult =>
        // TODO add test and change back to pre-new-auth URL:
        // Redirect(routes.LandingController.root())
        maybeResult.getOrElse (Redirect(routes.LandingController.goToErrorPage()))
      } recover {
        case x: NoActiveSession ⇒
          Logger.warn(s"could not authenticate user due to: No Active Session " + x)
          // TODO use authentication.login-callback.url ?
          toGGLogin(frontendAppConfig.getAccountPageCallbackUrl)
      }
    }
  }

  def authorisedWithAgent[R](body: (AgentInfo => Future[R]))(implicit hc: HeaderCarrier): Future[Option[R]] =
    authorised(AuthProviders(GovernmentGateway)).retrieve(allEnrolments and affinityGroup) {
      case enrol ~ affinityG =>
        affinityG match {
          case Some(Agent) => body(AgentInfo(enrol, getArn(enrol.enrolments))).map(result => Some(result))
          case _ => Future successful None
        }
    }
}