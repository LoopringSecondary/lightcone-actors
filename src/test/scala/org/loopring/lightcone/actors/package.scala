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

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout
import akka.pattern.ask
import java.util.concurrent.ForkJoinPool

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

package object helper {

  implicit val system = ActorSystem()
  implicit val context = ExecutionContext.fromExecutor(ForkJoinPool.commonPool())
  implicit val timeout = Timeout(5 seconds)

  val lrc = "LRC"
  val xyz = "XYZ"
  val gto = "GTO"

  def newOrderManagerActor(owner: String) = {
    system.actorOf(Props(new OrderManagingActor(owner)), "ordermanager-" + owner)
  }

  def askAndResp(actor: ActorRef, req: Any) = {
    Await.result(actor ? req, timeout.duration)
  }
}
