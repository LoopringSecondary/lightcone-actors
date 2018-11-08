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

import akka.actor.ActorRef
import org.loopring.lightcone.actors.helper._
import org.loopring.lightcone.core._
import org.scalatest._

class RingSubmitterSpec extends FlatSpec with Matchers {
  val submitterActor: ActorRef = routes.getRingSubmitterActor

  info("[sbt actors/'testOnly *RingSubmitterSpec -- -z submitSingleRing']")
  "submitSingleRing" should "send one tx to EthAccessActor" in {
    val order1 = ExpectedFill()
    val order2 = ExpectedFill()
    val ring = Ring(
      maker = Some(order1),
      taker = Some(order2)
    )
    submitterActor ! SubmitRingReq(Seq(ring))
    Thread.sleep(1000)
  }
}
