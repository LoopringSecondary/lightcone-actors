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
import org.loopring.lightcone.core._

import scala.concurrent.ExecutionContext

class DepthViewActor(
    marketId: MarketId,
    orderPool: OrderPool[DepthOrder],
    depthManagers: Map[Double, DepthView], // 同一市场不同精读
                    )(
  implicit
  ec: ExecutionContext,
  timeout: Timeout
) extends Actor with ActorLogging {

  var latestMatchPrice = 0d

  def routers(granularity: Double): DepthView = {
    assert(depthManagers.contains(granularity))
    depthManagers.get(granularity).get
  }

  def receive() = LoggingReceive {
    case event: DepthEvent ⇒
      process(event.toPojo())

    case events: DepthEventList =>
      events.events.map(x=>process(x.toPojo))

    case event: LatestMatchPriceEvent =>
      val price = Rational(event.amountS, event.amountB).doubleValue()
      latestMatchPrice = price

    case DepthReq(granularity, length) =>
      val (asks, bids) = routers(granularity).get(latestMatchPrice, length)
      DepthRes(asks.values.map(_.toProto()).toSeq, bids.values.map(_.toProto()).toSeq)
  }

  private def process(order: DepthOrder) = {
    depthManagers.map(_._2.set(order))
  }

}
