/*
 * Copyright 2018 Loopring Foundation
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

package org.loopring.lightcone.actors

import akka.actor.ActorLogging
import akka.event.LoggingReceive
import akka.util.Timeout
import org.loopring.lightcone.actors.base.{ Job, RepeatedJobActor }
import org.loopring.lightcone.core.TokenValueEstimatorImpl

import scala.concurrent.{ ExecutionContext, Future }

class TokenValueSyncActor()(
    implicit
    val tokenValueEstimator: TokenValueEstimatorImpl,
    ec: ExecutionContext,
    timeout: Timeout
)
  extends RepeatedJobActor
  with ActorLogging {
  val resubmitJob = Job(
    id = 1,
    name = "syncTokenValue",
    scheduleDelay = 5 * 60 * 1000,
    callMethod = syncMarketCap _
  )

  initAndStartNextRound(resubmitJob)

  //todo：初始化
  override def receive: Receive = super.receive orElse LoggingReceive {
    case req: UpdatedTokenBurnRate ⇒ tokenValueEstimator.burnRates += req.token → req.burnRate
  }

  def syncMarketCap(): Future[Unit] = {
    //todo:测试暂不实现，需要实现获取token以及marketcap的方法
    Future.successful()
  }

}
