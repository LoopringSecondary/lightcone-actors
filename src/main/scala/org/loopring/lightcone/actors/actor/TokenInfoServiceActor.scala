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

package org.loopring.lightcone.actors.actor

import akka.actor.{ Actor, ActorLogging, ActorSystem }
import akka.stream.ActorMaterializer
import akka.stream.alpakka.slick.scaladsl.SlickSession
import akka.pattern.pipe
import org.loopring.lightcone.actors.base
import org.loopring.lightcone.actors.marketcap.DatabaseAccesser
import org.loopring.lightcone.proto.market_cap._
import org.loopring.lightcone.actors.marketcap.{ CacherSettings, ProtoBufMessageCacher }
import org.loopring.lightcone.proto.deployment.TokenInfoServiceSettings
import scala.concurrent.Future

object TokenInfoServiceActor
  extends base.Deployable[TokenInfoServiceSettings] {
  val name = "token_info_service_actor"

  def getCommon(s: TokenInfoServiceSettings) =
    base.CommonSettings(None, s.roles, s.instances)
}

class TokenInfoServiceActor(
    implicit
    system: ActorSystem,
    mat: ActorMaterializer,
    session: SlickSession
)
  extends DatabaseAccesser with Actor with ActorLogging {

  import system.dispatcher
  import session.profile.api._

  implicit val settings = CacherSettings(system.settings.config)

  implicit val toTokenInfo = (r: ResultRow) ⇒
    TokenInfo(protocol = r <<, deny = r <<, isMarket = r <<, symbol = r <<, source = r <<, decimals = r <<)

  val cacherTokenInfo = new ProtoBufMessageCacher[GetTokenListRes]
  val tokenInfoKey = "TOKEN_INFO_KEY"

  override def receive: Receive = {

    case req: GetTokenListReq ⇒
      //优先查询缓存，缓存没有再查询数据表并存入缓存
      val res = cacherTokenInfo.getOrElse(tokenInfoKey, Some(600)) {
        val resp: Future[GetTokenListRes] =
          sql"""select protocol,deny,is_market,symbol,source,decimals
              from t_token_info
          """.list[TokenInfo].map(GetTokenListRes(_))

        resp.map(Some(_))
      }

      res.map {
        case Some(r) ⇒ r
        case _       ⇒ throw new Exception("data in table is null. Please find the reason!")
      } pipeTo sender

  }

}
