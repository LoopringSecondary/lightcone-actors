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

import org.loopring.lightcone.lib.{Order ⇒ LOrder, Ring ⇒ LRing}
import org.web3j.crypto._
import org.web3j.tx.ChainId
import org.web3j.utils.Numeric

import scala.annotation.tailrec

class RingSubmitterImpl(
    protocol: String = "",
    chainId: Byte = 0.toByte,
    privateKey: String = "",
    feeReceipt: String = ""
) extends RingSubmitter {
  //防止一个tx中的订单过多，超过 gaslimit
  val maxRingsInOneTx = 5
  val credentials: Credentials = Credentials.create(privateKey)

  var currentNonce = new AtomicInteger(1) //todo: 启动时,nonce需要初始化,

  def getSubmitterAddress(): String = credentials.getAddress

  def generateInputData(rings: Seq[Ring]): Seq[String]  = {

    @tailrec
    def generateInputDataRec(rings:Seq[Ring], res:Seq[String]):Seq[String] = {
      if (rings.isEmpty) {
        return res
      }
      val (toSubmit, remained) = rings.splitAt(maxRingsInOneTx)
      var lRing = LRing(
        feeReceipt,
        credentials.getAddress,
        "",
        Seq.empty[Seq[Int]],
        Seq.empty[LOrder],
        "",
        "")

      val orders = rings.flatMap {
        ring ⇒
          Set(ring.getMaker.getOrder, ring.getTaker.getOrder)
      }.distinct

      val orderIndexes = rings.map{
        ring ⇒
          Seq(
            orders.indexOf(ring.getTaker.getOrder),
            orders.indexOf(ring.getMaker.getOrder))
      }

      lRing = lRing.copy(
        ringOrderIndex = orderIndexes
      )

      val signatureData = Sign.signMessage(
        Numeric.hexStringToByteArray(lRing.hash),
        credentials.getEcKeyPair)
      val sigBytes = signatureData.getR ++ signatureData.getS
      lRing = lRing.copy(sig = Numeric.toHexString(sigBytes))
      //lRing.getInputData()
      val inputData = ""
      generateInputDataRec(remained, res :+ inputData)
    }

    generateInputDataRec(rings, Seq.empty[String])
  }

  def generateTxData(inputData: String): Array[Byte] = {
    val rawTransaction = RawTransaction.createTransaction(
      BigInt(currentNonce.getAndIncrement()).bigInteger,
      BigInt(0).bigInteger, //todo:需要确定gasprice和gaslimit
      BigInt(0).bigInteger,
      protocol,
      BigInt(0).bigInteger,
      inputData
    )
    signTx(rawTransaction)
  }

  private def signTx(rawTransaction: RawTransaction): Array[Byte] = {
    if (chainId > ChainId.NONE)
      TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
    else
      TransactionEncoder.signMessage(rawTransaction, credentials)
  }

}
