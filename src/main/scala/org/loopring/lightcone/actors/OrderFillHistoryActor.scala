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

class OrderFillHistoryActor()(
  implicit ec: ExecutionContext, timeout: Timeout)
  extends Actor with ActorLogging {

  val orderManagignActor: ActorRef = ???
  val ethereumAccessActor: ActorRef = ???

  def receive() = LoggingReceive {
    case req: GetFilledAmountReq ⇒ ethereumAccessActor forward req
    case req: UpdateFilledAmountReq ⇒ ethereumAccessActor forward req

    case SubmitOrderReq(orderOpt) if orderOpt.nonEmpty ⇒
      val order = orderOpt.get.toPojo

      for {
        filledAmountSMap ← getFilledAmount(Seq(order.id))
        filledAmountS: BigInt = filledAmountSMap(order.id)
        updated = if (filledAmountS == 0) order else {
          val outstanding = order.outstanding.scaleBy(
            Rational(order.amountS - filledAmountS, order.amountS))
          order.copy(_outstanding = Some(outstanding))
        }
      } yield {
        orderManagignActor forward SubmitOrderReq(Some(updated.toProto))
      }
  }

  def getFilledAmount(orderIds: Seq[String]): Future[Map[String, BigInt]] = {
    (ethereumAccessActor ? GetFilledAmountReq(orderIds))
      .mapTo[GetFilledAmountResp]
      .map(_.filledAmountSMap)
  }
}
