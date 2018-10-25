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

import akka.actor.{ ActorSystem, Props }
import akka.util.Timeout
import org.loopring.lightcone.core.{ Address, DustOrderEvaluatorImpl, MarketId, MarketManagerConfig, MarketManagerImpl, OrderPool, PendingRingPoolImpl, RingIncomeEstimatorImpl, SimpleRingMatcher, SystemTimeProvider, TokenValueEstimatorImpl }
import org.scalatest._
import akka.pattern.ask
import scala.concurrent.duration._

class MarketManagerSpec extends FlatSpec with Matchers {
  val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val timeout = new Timeout(1 seconds)

  val lrc = "LRC"
  val eth = "ETH"

  implicit val tve = new TokenValueEstimatorImpl()
  tve.setMarketCaps(Map[Address, Double](lrc → 0.8, eth → 1400))
  tve.setTokens(Map[Address, BigInt](lrc → BigInt(1), eth → BigInt(1)))

  val incomeEvaluator = new RingIncomeEstimatorImpl(10)
  val simpleMatcher = new SimpleRingMatcher(incomeEvaluator)

  implicit val dustEvaluator = new DustOrderEvaluatorImpl(1)

  implicit val timeProvider = new SystemTimeProvider()
  implicit val pendingRingPool = new PendingRingPoolImpl()

  //info("[sbt actors/'testOnly *MarketManagerSpec -- -z submitOrder']")
  "submitOrder" should "test ring" in {
    val marketManager = new MarketManagerImpl(MarketId(lrc, eth), MarketManagerConfig(0, 0), simpleMatcher)
    val marketManagerActor = system.actorOf(Props(new MarketManagingActor(marketManager)))
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
      matchable = Some(OrderState(amountS = BigInt(100).toByteArray, amountB = BigInt(10).toByteArray, amountFee = BigInt(10).toByteArray))
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
      matchable = Some(OrderState(amountS = BigInt(100).toByteArray, amountB = BigInt(10).toByteArray, amountFee = BigInt(10).toByteArray))
    )
    val taker = Order(
      id = "taker1",
      tokenS = eth,
      tokenB = lrc,
      tokenFee = lrc,
      amountS = BigInt(10).toByteArray,
      amountB = BigInt(100).toByteArray,
      amountFee = BigInt(10).toByteArray,
      walletSplitPercentage = 0.2,
      status = OrderStatus.NEW,
      matchable = Some(OrderState(amountS = BigInt(10).toByteArray, amountB = BigInt(100).toByteArray, amountFee = BigInt(10).toByteArray))
    )
    val delMarker1 = maker1.copy(status = OrderStatus.CANCELLED_BY_USER)
    marketManagerActor ? SubmitOrderReq(Some(maker1)) onComplete (
      res ⇒ println(res)
    )

    marketManagerActor ? SubmitOrderReq(Some(maker2)) onComplete (
      res ⇒ println(res)
    )
    marketManagerActor ! SubmitOrderReq(Some(delMarker1))

    marketManagerActor ? SubmitOrderReq(Some(taker)) onComplete (
      res ⇒ println(res)
    )

  }
}
