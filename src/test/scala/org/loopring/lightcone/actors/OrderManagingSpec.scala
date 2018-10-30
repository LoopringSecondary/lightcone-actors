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

class OrderManagingSpec extends FlatSpec with Matchers {

  info("[sbt actors/'testOnly *OrderManagingSpec']")

  "simpleTest1" should "submit 2 orders" in {
    val user1 = "me"

    // 充值到链上
    val lrcaccount = UpdateBalanceAndAllowanceReq(user1, lrc,
      Some(BalanceAndAllowance(10000, 10000)))

    val ethaccount = UpdateBalanceAndAllowanceReq(user1, eth,
      Some(BalanceAndAllowance(10, 10)))

    val viteaccount = UpdateBalanceAndAllowanceReq(user1, vite,
      Some(BalanceAndAllowance(10000, 10000)))

    updateAccountOnChain(lrcaccount)
    updateAccountOnChain(ethaccount)
    updateAccountOnChain(viteaccount)

    // 下单
    val ordermanager = prepare(user1)

    val maker = Order("maker", lrc, eth, lrc, 1000, 1, 100)
    val req1 = SubmitOrderReq(Some(maker))
    askAndWait(ordermanager, req1) match {
      case SubmitOrderRes(e, _) ⇒ e.isOk should be(true)
      case _                    ⇒ true should be(false)
    }

    val taker = Order("order1", eth, lrc, lrc, 1, 1000, 100)
    val req2 = SubmitOrderReq(Some(taker))
    askAndWait(ordermanager, req2) match {
      case SubmitOrderRes(e, _) ⇒ e.isOk should be(true)
      case _                    ⇒ true should be(false)
    }
  }

}
