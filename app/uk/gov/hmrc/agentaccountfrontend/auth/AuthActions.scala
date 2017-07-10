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

import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Application, Configuration, Environment, Logger}
import uk.gov.hmrc.agentaccountfrontend.config.{FrontendAppConfig, FrontendAuthConnector}
import uk.gov.hmrc.agentaccountfrontend.controllers.routes
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.Retrievals._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.frontend.Redirects
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait AuthActions extends AuthorisedFunctions with Redirects {

  case class AgentRequest[A](enrolments: Enrolments, arn: Option[String], request: Request[A]) extends WrappedRequest[A](request)

  val app: Application

  override def authConnector: AuthConnector = new FrontendAuthConnector

  override def config: Configuration = app.configuration

  override def env: Environment = Environment(app.path, app.classloader, app.mode)

  lazy val frontendAppConfig = new FrontendAppConfig

  protected type AsyncPlayUserRequest = Request[AnyContent] => AgentRequest[AnyContent] => Future[Result]

  private implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  private def isAgentAffinityGroup(affinityGroup: AffinityGroup): Boolean = affinityGroup.toString.equals("Agent")

  private def getArn(enrolment: Set[Enrolment]) =
    enrolment.find(_.key equals "HMRC-AS-AGENT").flatMap(_.identifiers.find(_.key equals "AgentReferenceNumber").map(_.value))

  def AuthorisedWithAgentAsync(body: AsyncPlayUserRequest): Action[AnyContent] = {
    Action.async { implicit request =>
      authorised(AuthProviders(GovernmentGateway)).retrieve(allEnrolments and affinityGroup) {
        case enrol ~ affinityG =>
          (isAgentAffinityGroup(affinityG.get), getArn(enrol.enrolments)) match {
            case (true, Some(arn)) => body(request)(AgentRequest(enrol, Some(arn), request))
            case (true, None) => body(request)(AgentRequest(enrol, None, request))
            case _ => Future.successful(Redirect(routes.LandingController.goToErrorPage()))
          }
        case _ => Future.successful(Redirect(routes.LandingController.goToErrorPage()))
      } recover {
        case e => handleFailure(e)
      }
    }
  }

  def handleFailure(e: Throwable): Result =
    e match {
      case x: NoActiveSession ⇒
        Logger.warn(s"could not authenticate user due to: No Active Session " + x)
        toGGLogin(frontendAppConfig.getAccountPageCallbackUrl)
      case ex ⇒
        Logger.warn(s"could not authenticate user due to: $ex")
        Redirect(routes.LandingController.goToErrorPage())
    }
}