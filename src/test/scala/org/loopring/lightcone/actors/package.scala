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

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.util.Timeout
import akka.pattern.ask
import java.util.concurrent.ForkJoinPool

import org.loopring.lightcone.core._

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._

package object helper {

  val eth = "ETH"
  val lrc = "LRC"
  val vite = "VITE"

  implicit val system = ActorSystem()
  implicit val context = ExecutionContext.fromExecutor(ForkJoinPool.commonPool())
  implicit val timeout = Timeout(5 seconds)
  implicit val timeProvider = new SystemTimeProvider()

  val accessorimpl = new EthAccessSpecActor()
  implicit val tokenValueEstimator = new TokenValueEstimatorImpl()
  tokenValueEstimator.setMarketCaps(Map[Address, Double](lrc → 1, eth → 2000, vite -> 0.5))
  tokenValueEstimator.setTokens(Map[Address, BigInt](lrc → BigInt(1), eth → BigInt(1), vite -> BigInt(1)))

  implicit val dustEvaluator = new DustOrderEvaluatorImpl(1)

  val incomeEvaluator = new RingIncomeEstimatorImpl(10)
  val simpleMatcher = new SimpleRingMatcher(incomeEvaluator)

  def newOrderManagerActor(owner: String) = {
    system.actorOf(Props(new OrderManagingActor(owner)), "order-manager-" + owner)
  }

  def newEthAccessorActor() = {
    system.actorOf(Props(accessorimpl), "ethereum-accessor")
  }

  def ethUpdateBalanceAndAllowance(req: UpdateBalanceAndAllowanceReq) = {
    var map = accessorimpl.map.getOrElse(req.address, Map.empty[String, BalanceAndAllowance])
    map += req.token -> req.getBalanceAndAllowance
    accessorimpl.map += req.address -> map
  }

  def newMarketManagerActor(tokenS: String, tokenB: String) = {
    val marketId = MarketId(tokenS, tokenB)
    val marketConfig = MarketManagerConfig(0, 0)
    val pendingRingPool = new PendingRingPoolImpl()
    val incomeEvaluator = new RingIncomeEstimatorImpl(10)
    val ringMatcher = new SimpleRingMatcher(incomeEvaluator)
    val marketManager = new MarketManagerImpl(marketId, marketConfig, ringMatcher)(pendingRingPool, dustEvaluator)
    val marketManaging = new MarketManagingActor(marketManager)
    system.actorOf(Props(marketManaging), "market-manager-" + tokenS + "-" + tokenB)
  }

  def askAndResp(actor: ActorRef, req: Any) = {
    Await.result(actor ? req, timeout.duration)
  }
}
