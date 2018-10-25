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
    val owner = "me"
    val manager = newOrderManagerActor(owner)
    val order = Order("order1", lrc, eth, vite, BigInt(100).toByteArray, BigInt(100).toByteArray, BigInt(100).toByteArray)
    val req = SubmitOrderReq(Some(order))

    val resp = askAndResp(manager, req)

    info(resp.toString)
    resp should be(1)
  }

}
