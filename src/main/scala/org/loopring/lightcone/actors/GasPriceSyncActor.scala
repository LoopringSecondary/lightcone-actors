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
import org.loopring.lightcone.core.GasPriceProvider

import scala.concurrent.{ ExecutionContext, Future }

class GasPriceSyncActor()(
    implicit
    val gasPriceProvider: GasPriceProvider,
    ec: ExecutionContext,
    timeout: Timeout
)
  extends RepeatedJobActor
  with ActorLogging {

  //todo：初始化
  override def receive: Receive = super.receive orElse LoggingReceive {
    case req: UpdatedGasPrice ⇒ gasPriceProvider.setGasPrice(req.gasPrice)
  }

}
