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
import akka.util.Timeout
import akka.pattern.ask
import com.google.protobuf.ByteString
import org.loopring.lightcone.core._

import scala.concurrent.Await
import scala.concurrent.duration._

package object helper {

  val eth = "ETH"
  val lrc = "LRC"
  val vite = "VITE"

  implicit val timeout = Timeout(5 seconds)
  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val timeProvider = new SystemTimeProvider()
  implicit val tokenValueEstimator = new TokenValueEstimatorImpl()
  tokenValueEstimator.setMarketCaps(Map[Address, Double](lrc → 1, eth → 2000, vite -> 0.5))
  tokenValueEstimator.setTokens(Map[Address, BigInt](lrc → BigInt(1), eth → BigInt(1), vite -> BigInt(1)))
  implicit val dustEvaluator = new DustOrderEvaluatorImpl(1)

  val marketId = MarketId(lrc, eth)
  val orderPool = new OrderPoolImpl
  val depthOrderPool = new DepthOrderPoolImpl
  val marketConfig = MarketManagerConfig(0, 0)
  val pendingRingPool = new PendingRingPoolImpl()
  val incomeEvaluator = new RingIncomeEstimatorImpl(10)
  val ringMatcher = new SimpleRingMatcher(incomeEvaluator)
  implicit val marketManager = new MarketManagerImpl(marketId, marketConfig, ringMatcher)(pendingRingPool, dustEvaluator)

  var r = new SimpleRoutersImpl()
  r.ethAccessActor = system.actorOf(Props(new EthAccessSpecActor()))
  r.marketManagingActors = Map(
    marketId.ID → system.actorOf(Props(newMarketManager()), "market-manager-lrc-eth")
  )
  r.ringSubmitterActor = system.actorOf(Props(new RingSubmitterActor("0xa")))
  implicit val routes: Routers = r

  def prepare(owner: String) = {
    system.actorOf(Props(new OrderManagingActor(owner, orderPool)), "order-manager-" + owner)
  }

  def updateAccountOnChain(req: UpdateBalanceAndAllowanceReq) = {
    var map = OnChainAccounts.map.getOrElse(req.address, Map.empty[String, BalanceAndAllowance])
    map += req.token -> req.getBalanceAndAllowance
    OnChainAccounts.map += req.address -> map
  }

  def newMarketManager() = {
    new MarketManagingActor(marketManager)
  }

  def askAndWait(actor: ActorRef, req: Any)(implicit timeout: Timeout) = {
    Await.result(actor ? req, timeout.duration)
  }

  def tell(actor: ActorRef, req: Any) = {
    actor ! req
  }

  implicit def int2byteString(src: Int): ByteString = bigIntToByteString(src)
}
