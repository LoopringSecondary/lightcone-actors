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
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor
  with ActorLogging {

  val ethereumAccessActor: ActorRef = Routers.ethAccessActor

  implicit val orderPool = new OrderPool()
  val manager: OrderManager = OrderManager.default(10000)

  def receive() = LoggingReceive {
    case SubmitOrderReq(orderOpt) ⇒
      assert(orderOpt.nonEmpty)
      val order = orderOpt.get.toPojo
      assert(order.outstanding.amountS > 0)

      // Set(order.tokenS, order.tokenFee).map {
      //   token ⇒

      //     for {
      //       resp ← (ethereumAccessActor ? GetBalanceAndAllowancesReq(owner, token)).mapTo[GetBalanceAndAllowancesResp]
      //     }

      //     if (!manager.hasTokenManager(token)) {
      //       val tokenManager: TokenManager = ???
      //       tokenManager.init(123, 345)
      //       manager.addTokenManager(tokenManager)
      //     }
      // }
      manager.submitOrder(order)
  }
}
