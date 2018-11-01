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

import akka.actor.ActorRef
import org.loopring.lightcone.core.MarketId

//todo:simple routers for test
class SimpleRoutersImpl extends Routers {

  var orderManagerActors = Map[Address, ActorRef]()

  var marketManagingActors = Map[String, ActorRef]()

  var depthViewActors = Map[String, ActorRef]()

  var ethAccessActor = ActorRef.noSender

  var ringSubmitterActor = ActorRef.noSender

  def getOrderManagingActor(owner: Address): Option[ActorRef] = {
    orderManagerActors.get(owner)
  }

  def getMarketManagingActor(marketId: MarketId): Option[ActorRef] = {
    marketManagingActors.get(tokensToMarketHash(marketId.primary, marketId.secondary))
  }

  def getEthAccessActor: ActorRef = ethAccessActor

  def getRingSubmitterActor: ActorRef = ringSubmitterActor

  def getDepthActor(marketId: MarketId): Option[ActorRef] = {
    depthViewActors.get(tokensToMarketHash(marketId.primary, marketId.secondary))
  }

}
