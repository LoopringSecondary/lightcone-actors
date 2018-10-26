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

import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import akka.event.LoggingReceive
import akka.util.Timeout
import org.loopring.lightcone.actors.base._
import org.scalatest._
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

class RepeatedJobSpec extends FlatSpec with Matchers {

  val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val timeout = new Timeout(1 seconds)

  class JobActor(jobs: Job*)(
      implicit
      ec: ExecutionContext,
      timeout: Timeout
  )
    extends RepeatedJobActor with ActorLogging {

    initAndStartNextRound(jobs: _*)

    override def receive() = super.receive orElse LoggingReceive {
      case _ ⇒
    }
  }
  //info("[sbt actors/'testOnly *RepeatedJobSpec -- -z repeatedJob']")
  "repeatedJob" should "mutil jobs" in {
    var i1 = new AtomicInteger()
    var i2 = new AtomicInteger()

    def test1(): Future[Unit] = for {
      _ ← Future.successful(Unit)
    } yield {
      info("repeatedJob.test1 time:" + " i1:" + i1.getAndIncrement())
    }

    def test2(): Future[Unit] = for {
      _ ← Future.successful(Unit)
    } yield info("repeatedJob.test2 time:" + " i2:" + i2.getAndIncrement())

    val actor = system.actorOf(Props(new JobActor(
      Job(id = 1, name = "test1", scheduleDelay = 1000, callMethod = test1 _),
      Job(id = 2, name = "test2", scheduleDelay = 2000, callMethod = test2 _)
    )))

    Thread.sleep(4000)

    assert(i1.get() == 4)
    assert(i2.get() == 2)

    actor ! PoisonPill
  }

}
