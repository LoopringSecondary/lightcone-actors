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
import com.google.protobuf.ByteString
import org.loopring.lightcone.actors.routing.Routers
import org.loopring.lightcone.proto.deployment.RingSubmitterSettings

import scala.concurrent.ExecutionContext

object RingSubmitActor
  extends base.Deployable[RingSubmitterSettings] {
  val name = "ring_submit_actor"

  def getCommon(s: RingSubmitterSettings) =
    base.CommonSettings(None, Seq(), 1)
}

class RingSubmitActor(
    submitter: Address
)(
    implicit
    ec: ExecutionContext,
    timeout: Timeout
)
  extends Actor
  with ActorLogging {

  def receive: Receive = {
    case req: SubmitRingReq ⇒
      //todo:生成ring数据、签名以及生成rawtransction
      val data = ByteString.copyFromUtf8(req.rings.toString())
      Routers.ethereumAccessActor ! SendRawTransaction(data)
  }

}