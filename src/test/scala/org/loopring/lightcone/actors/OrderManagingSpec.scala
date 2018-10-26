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

  "simpleTest1" should "say hello" in {
    val user1 = "me"

    // 充值到链上
    val lrcaccount = UpdateBalanceAndAllowanceReq(user1, lrc,
      Some(BalanceAndAllowance(bigIntToByteString(10000), bigIntToByteString(10000))))

    val ethaccount = UpdateBalanceAndAllowanceReq(user1, eth,
      Some(BalanceAndAllowance(bigIntToByteString(10), bigIntToByteString(10))))

    val viteaccount = UpdateBalanceAndAllowanceReq(user1, vite,
      Some(BalanceAndAllowance(bigIntToByteString(10000), bigIntToByteString(10000))))

    updateAccountOnChain(lrcaccount)
    updateAccountOnChain(ethaccount)
    updateAccountOnChain(viteaccount)

    // 下单
    val ordermanager = prepare(user1)

    val order1 = Order("order1", lrc, eth, vite, BigInt(1000).toByteArray, BigInt(1).toByteArray, BigInt(100).toByteArray)
    val req1 = SubmitOrderReq(Some(order1))
    askAndWait(ordermanager, req1)

    val order2 = Order("order1", eth, lrc, vite, BigInt(1).toByteArray, BigInt(1000).toByteArray, BigInt(100).toByteArray)
    val req2 = SubmitOrderReq(Some(order2))
    askAndWait(ordermanager, req2)
  }

}
