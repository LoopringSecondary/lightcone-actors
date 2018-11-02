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

package org.loopring.lightcone.core

import org.loopring.lightcone.actors._

case class UpdatedGasPriceEvent(event: UpdatedGasPrice, asserts: Seq[Assert], info: String) extends Event

case class MarketManagerBidsContainsOrderAssert(order: Order)(implicit marketManager: MarketManagerImpl) extends Assert {
  def assert() = {
    marketManager.bids.contains(order)
  }
}

case class MarketManagerBidsVolumeAssert(volumeExpect: Amount, sizeExpect: Int)(implicit marketManager: MarketManagerImpl) extends Assert {
  def assert() = {
    var volume = BigInt(0)
    marketManager.bids.foreach(volume += _.matchable.amountS)
    volume == volumeExpect && marketManager.bids.size == sizeExpect
  }
}

case class MarketManagerAsksContainsOrderAssert(order: Order)(implicit marketManager: MarketManagerImpl) extends Assert {
  def assert() = {
    marketManager.asks.contains(order)
  }
}

case class MarketManagerAsksVolumeAssert(volumeExpect: Amount, sizeExpect: Int)(implicit marketManager: MarketManagerImpl) extends Assert {
  def assert() = {
    var volume = BigInt(0)
    marketManager.asks.foreach(volume += _.matchable.amountS)
    volume == volumeExpect && marketManager.asks.size == sizeExpect
  }
}
