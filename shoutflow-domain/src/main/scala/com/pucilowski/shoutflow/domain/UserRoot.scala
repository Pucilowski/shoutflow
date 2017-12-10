package com.pucilowski.shoutflow.domain

import java.util.UUID

import com.pucilowski.shoutflow.commands._
import com.pucilowski.shoutflow.events._

class UserRoot(val userId: UUID) {

  var following: Set[UUID] = Set.empty

  var posts: Set[UUID] = Set.empty

  var likes: Set[UUID] = Set.empty


  def apply(event: UserEvent) = event match {
    //case evt: UserNameChanged =>
    //case evt: UserProfileUpdated =>
    case UserFollowedUser(_, followeeId, undo) =>
      following = if (!undo)
        following + followeeId
      else
        following - followeeId
    case UserPosted(_, shoutId, _) =>
      posts = posts + shoutId
    case UserRemovedPost(_, shoutId) =>
      posts = posts - shoutId
    case UserLikedPost(_, shoutId, undo) =>
      likes = if (!undo)
        likes + shoutId
      else
        likes - shoutId
    case _ =>
  }
}

object UserRoot {
  def seed(cmd: CreateUser): UserCreated = {
    UserCreated(cmd.userId, cmd.avatarHash, cmd.userName, cmd.fullName)
  }

  def seed(evt: UserCreated): UserRoot = {
    new UserRoot(evt.userId)
  }
}