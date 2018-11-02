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
      amountS = BigInt(100).toByteArray,
      amountB = BigInt(10).toByteArray,
      amountFee = BigInt(10).toByteArray,
      walletSplitPercentage = 0.2,
      status = OrderStatus.NEW,
      actual = Some(OrderState(amountS = BigInt(100).toByteArray, amountB = BigInt(10).toByteArray, amountFee = BigInt(10).toByteArray))
    )
    val maker2 = Order(
      id = "maker2",
      tokenS = lrc,
      tokenB = eth,
      tokenFee = lrc,
      amountS = BigInt(100).toByteArray,
      amountB = BigInt(10).toByteArray,
      amountFee = BigInt(10).toByteArray,
      walletSplitPercentage = 0.2,
      status = OrderStatus.NEW,
      actual = Some(OrderState(amountS = BigInt(100).toByteArray, amountB = BigInt(10).toByteArray, amountFee = BigInt(10).toByteArray))
    )

    var events: Seq[Event] = Seq.empty[Event]
    events :+= OrderEvent(
      event = maker1,
      asserts = Seq(
        MarketManagerBidsSizeAssert(1),
        MarketManagerBidsContainsOrderAssert(maker1.toPojo())
      ),
      info = s"submit first order, tokenS:${maker1.tokenS}, tokenB:${maker1.tokenB}, amountS:${BigInt(maker1.amountS)}, amountB:${BigInt(maker1.amountB)}," +
        s" then the bids size should be 1 and bids contains it."
    )

    events :+= OrderEvent(
      event = maker2,
      asserts = Seq(
        MarketManagerBidsSizeAssert(2)
      ),
      info = "submit second order, the bids size should be 2"
    )
    events :+= OrderEvent(
      event = maker2.copy(status = OrderStatus.CANCELLED_BY_USER),
      asserts = Seq(
        MarketManagerBidsSizeAssert(1)
      ),
      info = "cancel the second order, the bids size should be 1"
    )
    events :+= OrderEvent(
      event = Order(
        id = "taker1",
        tokenS = eth,
        tokenB = lrc,
        tokenFee = lrc,
        amountS = BigInt(10).toByteArray,
        amountB = BigInt(100).toByteArray,
        amountFee = BigInt(10).toByteArray,
        walletSplitPercentage = 0.2,
        status = OrderStatus.NEW,
        actual = Some(OrderState(amountS = BigInt(10).toByteArray, amountB = BigInt(100).toByteArray, amountFee = BigInt(10).toByteArray))
      ),
      asserts = Seq(
        MarketManagerBidsSizeAssert(0)
      ),
      info = "submit first taker order, then should be fullfilled, the bids size should be 0"
    )

    events :+= UpdatedGasPriceEvent(
      event = UpdatedGasPrice(),
      asserts = Seq(),
      info = "updated gas price, then should triggerMatch "
    )

    batchTest(events)

  }
}
