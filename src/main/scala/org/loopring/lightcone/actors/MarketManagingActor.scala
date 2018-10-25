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

import org.loopring.lightcone.core._
import akka.actor._
import akka.pattern.ask
import akka.event.LoggingReceive
import akka.util.Timeout

import scala.concurrent.{ ExecutionContext, Future }

class MarketManagingActor(
    manager: MarketManager
)(
    implicit
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor
  with ActorLogging {
  var latestGasPrice = 0l

  val ringSubmitterActor = Routers.ringSubmitterActor

  override def receive() = LoggingReceive {
    case SubmitOrderReq(Some(order)) ⇒
      order.status match {
        case OrderStatus.NEW ⇒
          val res = manager.submitOrder(order.toPojo)
          val rings = res.rings map (_.toProto)
          if (rings.nonEmpty) {
            ringSubmitterActor ! SubmitRingReq(rings = rings)
          }
        case _ ⇒
          manager.deleteOrder(order.toPojo)
      }
    case updatedGasPrce: UpdatedGasPrice ⇒
      if (latestGasPrice > updatedGasPrce.gasPrice) {
        val res = manager.triggerMatch()
        ringSubmitterActor ! SubmitRingReq(rings = res.rings map (_.toProto))
      }
      latestGasPrice = updatedGasPrce.gasPrice
    case _ ⇒
  }

}
