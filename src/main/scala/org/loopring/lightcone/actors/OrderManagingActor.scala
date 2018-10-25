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

import org.loopring.lightcone.core.{ Order ⇒ COrder, _ }
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
    timeout: Timeout,
    dustOrderEvaluator: DustOrderEvaluator
)
  extends Actor
  with ActorLogging {

  val ethereumAccessActor: ActorRef = ???
  val marketManagingActor: ActorRef = ???

  implicit val orderPool = new OrderPool()
  val updatdOrderReceiver = new UpdatedOrderReceiver()
  orderPool.addCallback(updatdOrderReceiver.onUpdatedOrder)

  val manager: OrderManager = OrderManager.default(10000)

  def receive() = LoggingReceive {
    case SubmitOrderReq(orderOpt) ⇒
      assert(orderOpt.nonEmpty)
      val order = orderOpt.get.toPojo
      assert(order.outstanding.amountS > 0)

      for {
        _ ← Future.sequence(Seq( // run in parallel
          getTokenManagerAsFuture(order.tokenS),
          getTokenManagerAsFuture(order.tokenFee)
        ))
        success = manager.submitOrder(order)
        updatedOrders = if (success) updatdOrderReceiver.getOrders else Seq.empty
      } yield {
        updatedOrders.foreach { order ⇒
          marketManagingActor ! SubmitOrderReq(Some(order.toProto))
        }
      }
  }

  private def getTokenManagerAsFuture(token: Address): Future[TokenManager] = {
    if (manager.hasTokenManager(token)) {
      Future.successful(manager.getTokenManager(token))
    } else {

      val tm = manager.addTokenManager(new TokenManager(token, 10000))

      (ethereumAccessActor ? GetBalanceAndAllowancesReq(owner, Seq(token)))
        .mapTo[GetBalanceAndAllowancesResp].map(_.balanceAndAllowanceMap(token)).map {
          ba ⇒
            tm.init(ba.balance, ba.allowance)
            tm
        }
    }
  }
}

class UpdatedOrderReceiver {
  def onUpdatedOrder(order: COrder): Unit = {}

  def getOrders(): Seq[COrder] = ???
}
