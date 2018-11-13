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

import akka.actor.{ Actor, ActorLogging }
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import org.web3j.utils.Numeric

object OnChainAccounts {
  var map = Map[Address, Map[String, BalanceAndAllowance]]()
}

class EthAccessSpecActor()(
    implicit
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor {

  override def receive: Receive = {
    case req: GetBalanceAndAllowancesReq ⇒
      val balanceAndAllowance = OnChainAccounts.map.get(req.address) map {
        tokens ⇒
          tokens map {
            token ⇒ (token._1, token._2)
          }
      }
      sender ! GetBalanceAndAllowancesRes(
        address = req.address,
        balanceAndAllowanceMap = balanceAndAllowance.get
      )
    case req: SendRawTransaction ⇒
      //todo：测试，只是打印下
      println("received raw transaction:", Numeric.toHexString(req.data.toByteArray))
  }
}
