package services

import models.Archetype

trait GithubService {
  
  def createRepo(reponame: String, zipData: Array[Byte], archetype: Archetype, accessToken: String): Option[String]
  
}
