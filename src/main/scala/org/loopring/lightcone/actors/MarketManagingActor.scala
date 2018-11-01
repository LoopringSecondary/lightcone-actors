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

import akka.actor._
import akka.event.LoggingReceive
import akka.util.Timeout
import org.loopring.lightcone.core.{ Order ⇒ COrder, _ }
import scala.concurrent.ExecutionContext

class MarketManagingActor(
    manager: MarketManager
)(
    implicit
    routes: Routers,
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor
  with ActorLogging {
  var latestGasPrice = 0l

  val ringSubmitterActor = routes.getRingSubmitterActor

  val depthActor = routes.getDepthActor(manager.marketId)

  override def receive() = LoggingReceive {
    case SubmitOrderReq(Some(order)) ⇒
      order.status match {
        case OrderStatus.PENDING | OrderStatus.NEW ⇒
          val res = manager.submitOrder(order.toPojo)
          val rings = res.rings map (_.toProto)
          if (rings.nonEmpty) {
            ringSubmitterActor ! SubmitRingReq(rings = rings)
          }
          if (res.affectedOrders.nonEmpty) {
            createAndSendDepthEvents(res.affectedOrders)
          }
        case _ ⇒
          manager.deleteOrder(order.toPojo)
          depthActor.foreach(_ ! DepthEventList(
            Seq(
              DepthEvent(
                order.id,
                order.tokenS,
                order.tokenB,
                order.amountS,
                order.amountB,
                BigInt(0)
              )
            )
          ))
      }

    case updatedGasPrce: UpdatedGasPrice ⇒
      if (latestGasPrice > updatedGasPrce.gasPrice) {
        val res = manager.triggerMatch()
        ringSubmitterActor ! SubmitRingReq(rings = res.rings map (_.toProto))
        if (res.affectedOrders.nonEmpty) {
          createAndSendDepthEvents(res.affectedOrders)
        }
      }
      latestGasPrice = updatedGasPrce.gasPrice
  }

  private def createAndSendDepthEvents(affectedOrders: Map[ID, COrder]): Unit = {
    val depthEvents = affectedOrders map {
      o ⇒
        val remainedOrder = o._2
        DepthEvent(
          remainedOrder.id,
          remainedOrder.tokenS,
          remainedOrder.tokenB,
          remainedOrder.amountS,
          remainedOrder.amountB,
          remainedOrder.matchable.amountS
        )
    }
    if (depthEvents.nonEmpty) {
      depthActor.foreach(_ ! DepthEventList(depthEvents.toSeq))
    }
  }

}
