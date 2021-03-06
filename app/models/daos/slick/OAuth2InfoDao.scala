package models.daos

import com.mohiva.play.silhouette.core.LoginInfo
import com.mohiva.play.silhouette.core.providers.OAuth2Info
import com.mohiva.play.silhouette.contrib.daos.DelegableAuthInfoDAO
import play.api.db.slick._
import scala.concurrent.Future
import play.api.db.slick.Config.driver.simple._
import models.daos.slick.LoginInfoSlickDB._
import models.daos.slick.OAuth2InfoSlickDB._
import play.api.Logger

/**
 * The DAO to store the OAuth2 information.
 */
class OAuth2InfoDAOSlick extends DelegableAuthInfoDAO[OAuth2Info] {

  import play.api.Play.current

  /**
   * Saves the OAuth2 info.
   *
   * @param loginInfo The login info for which the auth info should be saved.
   * @param authInfo The OAuth2 info to save.
   * @return The saved OAuth2 info or None if the OAuth2 info couldn't be saved.
   */
  def save(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    Future.successful(
      DB withSession { implicit session =>
        val infoId = slickLoginInfos.filter(
          x => x.providerID === loginInfo.providerID && x.providerKey === loginInfo.providerKey
        ).first.id.get
        slickOAuth2Infos.filter(_.loginInfoId === infoId).firstOption match {
          case Some(info) =>
            val dbOauth2Info = DBOAuth2Info(info.id, authInfo.accessToken, authInfo.tokenType, authInfo.expiresIn, authInfo.refreshToken, infoId)
            slickOAuth2Infos.filter(_.id === info.id) update dbOauth2Info
          case None => {
            val dbOauth2Info = DBOAuth2Info(None, authInfo.accessToken, authInfo.tokenType, authInfo.expiresIn, authInfo.refreshToken, infoId)
            slickOAuth2Infos insert dbOauth2Info
          }
        }
        authInfo
      }
    )
  }

  /**
   * Finds the OAuth2 info which is linked with the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The retrieved OAuth2 info or None if no OAuth2 info could be retrieved for the given login info.
   */
  def find(loginInfo: LoginInfo): Future[Option[OAuth2Info]] = {
    Logger.debug(s"Searching for: $loginInfo")
    Future.successful(
      DB withSession { implicit session =>
        slickLoginInfos.filter(info => info.providerID === loginInfo.providerID && info.providerKey === loginInfo.providerKey).firstOption match {
          case Some(info) =>
            val oAuth2Info = slickOAuth2Infos.filter(_.loginInfoId === info.id).first
            Some(OAuth2Info(oAuth2Info.accessToken, oAuth2Info.tokenType, oAuth2Info.expiresIn, oAuth2Info.refreshToken))
          case None => {
            None
          }
        }
      }
    )
  }
}
