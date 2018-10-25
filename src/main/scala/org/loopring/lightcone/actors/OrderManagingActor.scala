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
import akka.actor._
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.{ ExecutionContext, Future }

class OrderManagingActor(
    owner: Address
)(
    implicit
    dustOrderEvaluator: DustOrderEvaluator,
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor
  with ActorLogging {

  val ethereumAccessActor: ActorRef = ???
  val marketManagingActor: ActorRef = ???

  implicit val orderPool = new OrderPool()
  val updatedOrderReceiver = new UpdatedOrderReceiver()
  orderPool.addCallback(updatedOrderReceiver.onUpdatedOrder)
  val manager: OrderManager = OrderManager.default(10000)

  // todo: 如何初始化？在本actor初始化订单数据还是在其他actor获取所有数据后批量发送过来？？？

  // todo: 返回到上一层调用时status到error的映射关系
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
        updatedOrders = if (success) updatedOrderReceiver.getOrders() else Seq.empty
      } yield {
        updatedOrders.foreach { order ⇒
          marketManagingActor ! SubmitOrderReq(Some(order.toProto))
        }
      }

    case CancelOrderReq(id) ⇒ manager.cancelOrder(id) match {
      case true ⇒ for {
        updatedOrders ← Future(updatedOrderReceiver.getOrders())
        _ ← Future(updatedOrders.foreach { order ⇒
          marketManagingActor ! SubmitOrderReq(Some(order.toProto))
        })
      } yield CancelOrderRes(ErrorCode.OK)
      case false ⇒ CancelOrderRes(ErrorCode.ORDER_NOT_EXIST)
    }

    // amountS为剩余量
    case UpdateFilledAmountReq(id, amountS) ⇒ manager.adjustOrder(id, amountS) match {
      case true ⇒ for {
        updatedOrders ← Future(updatedOrderReceiver.getOrders())
        _ ← Future(
          updatedOrders.foreach { order ⇒
            marketManagingActor ! SubmitOrderReq(Some(order.toProto))
          }
        )
      } yield UpdateFilledAmountRes(ErrorCode.OK)
      case false ⇒ UpdateFilledAmountRes(ErrorCode.ORDER_NOT_EXIST)
    }

    // TODO: 这里不应该是一个req
    case UpdateBalanceAndAllowanceReq(address, token, accountOpt) ⇒
      assert(accountOpt.nonEmpty)
      val account = accountOpt.get
      assert(account.balance > 0)
      assert(account.allowance > 0)

      manager.getTokenManager(token).init(account.balance, account.allowance)

      val updatedOrders = updatedOrderReceiver.getOrders()
      updatedOrders.foreach { order ⇒
        marketManagingActor ! SubmitOrderReq(Some(order.toProto))
      }
  }

  private def getTokenManagerAsFuture(token: Address): Future[TokenManager] = {
    if (manager.hasTokenManager(token)) {
      Future.successful(manager.getTokenManager(token))
    } else {

      val tm = manager.addTokenManager(new TokenManager(token, 10000))

      (ethereumAccessActor ? GetBalanceAndAllowancesReq(owner, Seq(token)))
        .mapTo[GetBalanceAndAllowancesRes].map(_.balanceAndAllowanceMap(token)).map {
          ba ⇒
            tm.init(ba.balance, ba.allowance)
            tm
        }
    }
  }
}

class UpdatedOrderReceiver {
  var receivedOrders = Seq.empty[COrder]

  def onUpdatedOrder(order: COrder) = {
    receivedOrders :+ order
  }

  def getOrders(): Seq[COrder] = {
    receivedOrders.drop(receivedOrders.size)
  }

}
