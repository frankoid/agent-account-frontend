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

import play.api.mvc._
import uk.gov.hmrc.agentaccountfrontend.controllers.routes
import uk.gov.hmrc.passcode.authentication.PasscodeAuthentication
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.http.HeaderCarrier

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.Future

trait AuthActions extends Actions with PasscodeAuthentication {

  case class AgentRequest[A](enrolments: List[Enrolment], request: Request[A]) extends WrappedRequest[A](request)

  protected type AsyncPlayUserRequest = AuthContext => AgentRequest[AnyContent] => Future[Result]
  private implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  def AuthorisedWithAgentAsync(body: AsyncPlayUserRequest): Action[AnyContent] =
    AuthorisedFor(NoOpRegime, pageVisibility = GGConfidence).async {
      implicit authContext => implicit request =>
        withVerifiedPasscode {
          isAgentAffinityGroup.flatMap {
            case true => enrolments.flatMap(e => body(authContext)(AgentRequest(e, request)))
            case false => Future.successful(Redirect(routes.LandingController.root))
          }
        }
    }

  protected def enrolments(implicit authContext: AuthContext, hc: HeaderCarrier): Future[List[Enrolment]] =
    authConnector.getEnrolments[List[Enrolment]](authContext)

  protected def isAgentAffinityGroup()(implicit authContext: AuthContext, hc: HeaderCarrier): Future[Boolean] =
    authConnector.getUserDetails(authContext).map { userDetailsResponse =>
      val affinityGroup = (userDetailsResponse.json \ "affinityGroup").as[String]
      affinityGroup == "Agent"
    }

}
