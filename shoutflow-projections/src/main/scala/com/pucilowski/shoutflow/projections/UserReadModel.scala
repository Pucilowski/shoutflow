package com.pucilowski.shoutflow.projections

import java.util.UUID

import com.pucilowski.shoutflow.events._
import com.pucilowski.shoutflow.events.derived._

case class UserReadModel
(userId: UUID, avatarHash: String, userName: String,
 fullName: String, location: String = "", bio: String = "",
 posts: Int = 0, followees: Int = 0, followers: Int = 0, likes: Int = 0
) {
  def addPost() = {
    copy(posts = posts + 1)
  }

  def addFollowee(undo: Boolean) = {
    val change = if (!undo) 1 else -1
    copy(followees = followees + change)
  }

  def addFollower(undo: Boolean) = {
    val change = if (!undo) 1 else -1
    copy(followers = followers + change)
  }

  def addLike(undo: Boolean) = {
    val change = if (!undo) 1 else -1
    copy(likes = likes + change)
  }
}

case object UserReadModel {
  def seed(evt: UserCreated): UserReadModel =
    UserReadModel(evt.userId, evt.avatarHash, evt.userName, evt.fullName)

  def handle(state: UserReadModel, evt: UserEvent): UserReadModel = evt match {
    case evt: UserNameChanged =>
      state.copy(userName = evt.userName)
    case evt: UserProfileUpdated =>
      state.copy(avatarHash = evt.avatarHash, fullName = evt.fullName, location = evt.location, bio = evt.bio)
    case UserFollowedUser(_, _, undo) =>
      state.addFollowee(undo)
    case UserFollowedByUser(_, _, undo) =>
      state.addFollower(undo)
    case UserPosted(_, _, _) =>
      state.addPost()
    case UserRemovedPost(_, _) =>
      state.copy(posts = state.posts - 1)
    case UserLikedPost(_, _, undo) =>
      state.addLike(undo)
    case _ => state
  }
}