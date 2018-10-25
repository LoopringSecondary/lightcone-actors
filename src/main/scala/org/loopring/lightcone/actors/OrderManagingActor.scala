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

import org.loopring.lightcone.core._
import com.google.protobuf.ByteString
import akka.actor._
import akka.event.{ Logging, LoggingReceive }
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.{ ExecutionContext, Future }

class OrderManagingActor(
    owner: Address
)(
    implicit
    ethereumAccessActor: ActorRef,
    ec: ExecutionContext,
    timeout: Timeout,
    dustOrderEvaluator: DustOrderEvaluator,
)
  extends Actor
  with ActorLogging {

  implicit val orderPool = new OrderPool()
  val manager: OrderManager = OrderManager.default(10000)

  def receive() = LoggingReceive {
    case SubmitOrderReq(orderOpt) ⇒
      assert(orderOpt.nonEmpty)
      val order = orderOpt.get.toPojo
      assert(order.outstanding.amountS > 0)

      var undefinedTokenSet = Set.empty[String]
      Set(order.tokenS, order.tokenFee).map(token ⇒ {
        if (!manager.hasTokenManager(token)) {
          undefinedTokenSet += token
          val tokenManager = new TokenManager(token)
          manager.addTokenManager(tokenManager)
        }
      })

      if (undefinedTokenSet.nonEmpty) {
        Future.successful(for {
          resp <- ethereumAccessActor ? GetBalanceAndAllowancesReq(owner, undefinedTokenSet.toSeq)
        } yield resp match {
          case Some(res: GetBalanceAndAllowancesRes) => res.balanceAndAllowanceMap.map(x => {
            val token = x._1
            val balance  = byteString2BigInt(x._2.balance)
            val allowance = byteString2BigInt(x._2.allowance)
            manager.getTokenManager(token).init(balance, allowance)
          })
        })
      }

      manager.submitOrder(order)
      SubmitOrderRes()

    case CancelOrderReq(id) =>
      manager.cancelOrder(id)

    case UpdateFilledAmountReq(id, orderFilledAmountS) =>
      manager.adjustOrder(id, orderFilledAmountS)

    case UpdateBalanceAndAllowanceReq(address, token, accountOpt) =>
      assert(accountOpt.nonEmpty)
      val balance = byteString2BigInt(accountOpt.get.balance)
      val allowance = byteString2BigInt(accountOpt.get.allowance)

      assert(balance > 0)
      assert(allowance > 0)

      manager.getTokenManager(token).init(balance, allowance)
  }
}
