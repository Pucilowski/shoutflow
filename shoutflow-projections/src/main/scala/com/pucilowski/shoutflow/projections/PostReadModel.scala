package com.pucilowski.shoutflow.projections

import java.util.UUID

import com.pucilowski.shoutflow.events._
import com.pucilowski.shoutflow.events.derived.UserPostLiked

case class PostReadModel
(userId: UUID, postId: UUID,
 avatarHash: String, userName: String, fullName: String, timestamp: Long,
 body: String,
 likes: Int = 0
) {
  def this(userPosted: UserPosted) = this(UUID.randomUUID(), UUID.randomUUID(), "", "", "", 0, "")

  def addLike(undo: Boolean) = {
    val change = if (!undo) 1 else -1
    copy(likes = likes + change)
  }
}

case object PostReadModel {
  def seed(avatarHash: String, userName: String, fullName: String, timestamp: Long, event: UserPosted): PostReadModel =
    PostReadModel(event.userId, event.postId, avatarHash, userName, fullName, timestamp, event.body)

  def handle(state: PostReadModel, event: UserPostEvent): PostReadModel = event match {
    case UserPostLiked(_, _, undo) => state.addLike(undo)
    case _ => state
  }
}