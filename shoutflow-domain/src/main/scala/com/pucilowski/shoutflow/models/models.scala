package com.pucilowski.shoutflow

/**
  * Created by martin on 06/05/17.
  */
package object models {

  case class UserProfile(avatarHash: String, userName: String, fullName: String,
                         location: String, bio: String)

}
