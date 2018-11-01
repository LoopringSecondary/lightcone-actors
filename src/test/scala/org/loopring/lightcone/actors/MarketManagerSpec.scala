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

import org.scalatest._
import helper._

class MarketManagerSpec extends FlatSpec with Matchers {
  "submitOrder" should "test ring" in {

    info("[sbt actors/'testOnly *MarketManagerSpec -- -z submitOrder']")

    val marketManagerActor = routes.getMarketManagingActor(marketId).get

    val maker1 = Order(
      id = "maker1",
      tokenS = lrc,
      tokenB = eth,
      tokenFee = lrc,
      amountS = 100,
      amountB = 10,
      amountFee = 10,
      walletSplitPercentage = 0.2,
      status = OrderStatus.NEW,
      actual = Some(OrderState(amountS = 100, amountB = 10, amountFee = 10))
    )
    val maker2 = Order(
      id = "maker2",
      tokenS = lrc,
      tokenB = eth,
      tokenFee = lrc,
      amountS = 100,
      amountB = 10,
      amountFee = 10,
      walletSplitPercentage = 0.2,
      status = OrderStatus.NEW,
      actual = Some(OrderState(amountS = 100, amountB = 10, amountFee = 10))
    )
    val taker = Order(
      id = "taker1",
      tokenS = eth,
      tokenB = lrc,
      tokenFee = lrc,
      amountS = 10,
      amountB = 100,
      amountFee = 10,
      walletSplitPercentage = 0.2,
      status = OrderStatus.NEW,
      actual = Some(OrderState(amountS = 10, amountB = 100, amountFee = 10))
    )

    val delMarker1 = maker1.copy(status = OrderStatus.CANCELLED_BY_USER)
    tell(marketManagerActor, SubmitOrderReq(Some(maker1)))
    tell(marketManagerActor, SubmitOrderReq(Some(taker)))

    // todo
    //    marketManagerActor ! SubmitOrderReq(Some(maker2))
    //    marketManagerActor ! SubmitOrderReq(Some(delMarker1))
  }
}
