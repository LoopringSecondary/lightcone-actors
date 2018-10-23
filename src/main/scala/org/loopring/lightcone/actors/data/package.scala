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

import com.google.protobuf.ByteString

import org.loopring.lightcone.core.{
  Order ⇒ COrder,
  OrderState ⇒ COrderState,
  OrderStatus ⇒ COrderStatus
}

package object data {
  implicit class RichOrderStatus(status: OrderStatus) {
    def toPojo(): COrderStatus.Value = status match {
      case OrderStatus.NEW ⇒ COrderStatus.NEW
      case OrderStatus.PENDING ⇒ COrderStatus.PENDING
      case OrderStatus.EXPIRED ⇒ COrderStatus.EXPIRED
      case OrderStatus.CANCELLED_BY_USER ⇒ COrderStatus.CANCELLED_BY_USER
      case OrderStatus.CANCELLED_LOW_BALANCE ⇒ COrderStatus.CANCELLED_LOW_BALANCE
      case OrderStatus.CANCELLED_LOW_FEE_BALANCE ⇒ COrderStatus.CANCELLED_LOW_FEE_BALANCE
      case OrderStatus.CANCELLED_TOO_MANY_ORDERS ⇒ COrderStatus.CANCELLED_TOO_MANY_ORDERS
      case OrderStatus.CANCELLED_TOO_MANY_FAILED_SETTLEMENTS ⇒ COrderStatus.CANCELLED_TOO_MANY_FAILED_SETTLEMENTS
      case v ⇒ throw new IllegalArgumentException(s"$v not suppported")
    }
  }

  implicit class RichCOrderStatus(status: COrderStatus.Value) {
    def toProto(): OrderStatus = status match {
      case COrderStatus.NEW ⇒ OrderStatus.NEW
      case COrderStatus.PENDING ⇒ OrderStatus.PENDING
      case COrderStatus.EXPIRED ⇒ OrderStatus.EXPIRED
      case COrderStatus.CANCELLED_BY_USER ⇒ OrderStatus.CANCELLED_BY_USER
      case COrderStatus.CANCELLED_LOW_BALANCE ⇒ OrderStatus.CANCELLED_LOW_BALANCE
      case COrderStatus.CANCELLED_LOW_FEE_BALANCE ⇒ OrderStatus.CANCELLED_LOW_FEE_BALANCE
      case COrderStatus.CANCELLED_TOO_MANY_ORDERS ⇒ OrderStatus.CANCELLED_TOO_MANY_ORDERS
      case COrderStatus.CANCELLED_TOO_MANY_FAILED_SETTLEMENTS ⇒ OrderStatus.CANCELLED_TOO_MANY_FAILED_SETTLEMENTS
      case v ⇒ throw new IllegalArgumentException(s"$v not suppported")
    }
  }

  implicit class RichOrderState(state: OrderState) {
    def toPojo(): COrderState = COrderState(
      BigInt(state.amountS.toByteArray),
      BigInt(state.amountB.toByteArray),
      BigInt(state.amountFee.toByteArray)
    )
  }

  implicit class RichCOrderState(state: COrderState) {
    def toProto(): OrderState = OrderState(
      ByteString.copyFrom(state.amountS.toByteArray),
      ByteString.copyFrom(state.amountB.toByteArray),
      ByteString.copyFrom(state.amountFee.toByteArray)
    )
  }

  implicit class RichOrder(order: Order) {
    def toPojo(): COrder = COrder(
      order.id,
      order.tokenS,
      order.tokenB,
      Some(order.tokenFee),
      BigInt(order.amountS.toByteArray),
      BigInt(order.amountB.toByteArray),
      BigInt(order.amountFee.toByteArray),
      order.createdAt,
      order.status.toPojo,
      order.walletSplitPercentage,
      order.outstanding.map(_.toPojo),
      order.reserved.map(_.toPojo),
      order.actual.map(_.toPojo),
      order.matchable.map(_.toPojo)
    )
  }

  implicit class RichCOrder(order: COrder) {
    def toProto(): Order = Order(
      order.id,
      order.tokenS,
      order.tokenB,
      order.tokenFee.getOrElse(""),
      ByteString.copyFrom(order.amountS.toByteArray),
      ByteString.copyFrom(order.amountB.toByteArray),
      ByteString.copyFrom(order.amountFee.toByteArray),
      order.createdAt,
      order.status.toProto,
      order.walletSplitPercentage,
      order._outstanding.map(_.toProto),
      order._reserved.map(_.toProto),
      order._actual.map(_.toProto),
      order._matchable.map(_.toProto)
    )

  }
}
