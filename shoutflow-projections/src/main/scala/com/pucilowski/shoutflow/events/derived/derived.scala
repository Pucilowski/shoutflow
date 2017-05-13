package com.pucilowski.shoutflow.events

import java.util.UUID

/**
  * Created by martin on 05/05/17.
  */
package object derived {

  trait DerivedEvent

  case class PostDelivered(recipientId: UUID, shoutId: UUID) extends DerivedEvent

  case class UserPostLiked(userId: UUID, postId: UUID, undo: Boolean) extends UserPostEvent with DerivedEvent

  // the complementary event to UserFollowedUser
  case class UserFollowedByUser(userId: UUID, followerId: UUID, undo: Boolean) extends UserEvent with DerivedEvent

}