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

import akka.actor.ActorLogging
import akka.event.LoggingReceive
import akka.util.Timeout
import org.loopring.lightcone.actors.base._

import scala.concurrent._

class RingSubmitterActor(
    submitter: Address
)(
    implicit
    routers: Routers,
    ec: ExecutionContext,
    timeout: Timeout
)
  extends RepeatedJobActor
  with ActorLogging {

  val ringSubmitter = new RingSubmitterImpl() //todo:submitter，protocol，privatekey

  def ethereumAccessActor = routers.getEthAccessActor

  val resubmitJob = Job(id = 1, name = "resubmitTx", scheduleDelay = 120 * 1000, callMethod = resubmitTx _)

  initAndStartNextRound(resubmitJob)

  override def receive: Receive = super.receive orElse LoggingReceive {
    case req: SubmitRingReq ⇒
      val inputDatas = ringSubmitter.generateInputData(req.rings)
      inputDatas.foreach{
        inputData ⇒
          val txData = ringSubmitter.generateTxData(inputData)
          ethereumAccessActor ! SendRawTransaction(txData)
      }
  }

  //未被提交的交易需要使用新的gas和gasprice重新提交再次提交
  def resubmitTx(): Future[Unit] = {
    //todo：查询数据库等得到为能及时打块的交易
    val inputDataList = Seq.empty[String]
    inputDataList.foreach {
      inputData ⇒
        val txData = ringSubmitter.generateTxData(inputData)
        ethereumAccessActor ! SendRawTransaction(txData)
    }
    Future.successful(Unit)
  }
}
