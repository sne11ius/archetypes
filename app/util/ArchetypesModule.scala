package util

import com.google.inject.{/* Provides, */AbstractModule }
import net.codingwell.scalaguice.ScalaModule
import services.impl._
import services._
import models.User
import models.daos._
import models.daos.slick._
import com.google.inject.Provides
import play.api.Play
import play.api.Play.current
import com.mohiva.play.silhouette.core.services.AuthenticatorService
import com.mohiva.play.silhouette.core.EventBus
import com.mohiva.play.silhouette.core.providers.oauth2.GitHubProvider
import com.mohiva.play.silhouette.contrib.services.CachedCookieAuthenticator
import com.mohiva.play.silhouette.core.Environment
import com.mohiva.play.silhouette.core.utils.CacheLayer
import com.mohiva.play.silhouette.core.utils.IDGenerator
import com.mohiva.play.silhouette.contrib.services.CachedCookieAuthenticatorService
import com.mohiva.play.silhouette.contrib.services.CachedCookieAuthenticatorSettings
import com.mohiva.play.silhouette.core.utils.Clock
import com.mohiva.play.silhouette.core.utils.HTTPLayer
import com.mohiva.play.silhouette.core.providers.OAuth2Settings
import com.mohiva.play.silhouette.core.providers.OAuth2Info
import com.mohiva.play.silhouette.contrib.utils.PlayCacheLayer
import com.mohiva.play.silhouette.contrib.utils.SecureRandomIDGenerator
import com.mohiva.play.silhouette.core.utils.PlayHTTPLayer
import com.mohiva.play.silhouette.core.services.AvatarService
import com.mohiva.play.silhouette.contrib.services.GravatarService
import com.mohiva.play.silhouette.contrib.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.core.services.AuthInfoService
import com.mohiva.play.silhouette.contrib.services.DelegableAuthInfoService
import com.mohiva.play.silhouette.core.utils.PasswordHasher
import com.mohiva.play.silhouette.contrib.utils.BCryptPasswordHasher

class ArchetypesModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[ArchetypesService].to[ArchetypesServiceImpl]
    bind[ArchetypeDao].to[ArchetypeDaoSlick]
    bind[SourcePrettifyService].to[HiliteMeSourcePrettifyService]
    bind[GithubService].to[GithubServiceImpl]
    bind[controllers.Application]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[HTTPLayer].to[PlayHTTPLayer]
    bind[DelegableAuthInfoDAO[OAuth2Info]].to[OAuth2InfoDAOSlick]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[UserService].to[UserServiceImpl]
    bind[UserDao].to[UserDAOSlick]
    bind[PasswordHasher].toInstance(new BCryptPasswordHasher)
  }
  
  /**
   * Provides the Silhouette environment.
   *
   * @param userService The user service implementation.
   * @param authenticatorService The authentication service implementation.
   * @param eventBus The event bus instance.
   * @return The Silhouette environment.
   */
  @Provides
  def provideEnvironment(
    userService: UserService,
    authenticatorService: AuthenticatorService[CachedCookieAuthenticator],
    eventBus: EventBus,
    githubProvider: GitHubProvider) : Environment[User, CachedCookieAuthenticator] = {

    Environment[User, CachedCookieAuthenticator](
      userService,
      authenticatorService,
      Map(
        githubProvider.id -> githubProvider
      ),
      eventBus
    )
  }
  
  /**
   * Provides the auth info service.
   *
   * @param passwordInfoDAO The implementation of the delegable password auth info DAO.
   * @param oauth1InfoDAO The implementation of the delegable OAuth1 auth info DAO.
   * @param oauth2InfoDAO The implementation of the delegable OAuth2 auth info DAO.
   * @return The auth info service instance.
   */
  @Provides
  def provideAuthInfoService(oauth2InfoDAO: DelegableAuthInfoDAO[OAuth2Info]): AuthInfoService = {
    new DelegableAuthInfoService(oauth2InfoDAO)
  }
  
  @Provides
  def provideAvatarService(httpLayer: HTTPLayer): AvatarService = new GravatarService(httpLayer)
  
  /**
   * Provides the authenticator service.
   *
   * @param cacheLayer The cache layer implementation.
   * @param idGenerator The ID generator used to create the authenticator ID.
   * @return The authenticator service.
   */
  @Provides
  def provideAuthenticatorService(
    cacheLayer: CacheLayer,
    idGenerator: IDGenerator): AuthenticatorService[CachedCookieAuthenticator] = {

    new CachedCookieAuthenticatorService(CachedCookieAuthenticatorSettings(
      cookieName = Play.configuration.getString("silhouette.authenticator.cookieName").get,
      cookiePath = Play.configuration.getString("silhouette.authenticator.cookiePath").get,
      cookieDomain = Play.configuration.getString("silhouette.authenticator.cookieDomain"),
      secureCookie = Play.configuration.getBoolean("silhouette.authenticator.secureCookie").get,
      httpOnlyCookie = Play.configuration.getBoolean("silhouette.authenticator.httpOnlyCookie").get,
      cookieIdleTimeout = Play.configuration.getInt("silhouette.authenticator.cookieIdleTimeout").get,
      cookieAbsoluteTimeout = Play.configuration.getInt("silhouette.authenticator.cookieAbsoluteTimeout"),
      authenticatorExpiry = Play.configuration.getInt("silhouette.authenticator.authenticatorExpiry").get
    ), cacheLayer, idGenerator, Clock())
  }
  
  @Provides
  def provideGitHubProvider(cacheLayer: CacheLayer, httpLayer: HTTPLayer): GitHubProvider = {
    GitHubProvider(cacheLayer, httpLayer, OAuth2Settings(
      authorizationURL = Play.configuration.getString("silhouette.github.authorizationURL").get,
      accessTokenURL = Play.configuration.getString("silhouette.github.accessTokenURL").get,
      redirectURL = Play.configuration.getString("silhouette.github.redirectURL").get,
      clientID = Play.configuration.getString("silhouette.github.clientID").get,
      clientSecret = Play.configuration.getString("silhouette.github.clientSecret").get,
      scope = Play.configuration.getString("silhouette.github.scope")))
  }

}
