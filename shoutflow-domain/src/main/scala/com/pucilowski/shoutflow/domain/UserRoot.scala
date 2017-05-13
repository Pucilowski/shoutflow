package com.pucilowski.shoutflow.domain

import java.util.UUID

import com.pucilowski.shoutflow.commands._
import com.pucilowski.shoutflow.events._

class UserRoot(userId: UUID) {

  var following: Set[UUID] = Set.empty

  var posts: Set[UUID] = Set.empty

  var likes: Set[UUID] = Set.empty

  def handle(command: UserCommand): Either[String, Seq[UserEvent]] = command match {
    case cmd: ChangeUserName =>
      Right(Seq(UserNameChanged(userId, cmd.userName)))
    case cmd: UpdateUserProfile =>
      Right(Seq(UserProfileUpdated(userId, cmd.avatarHash, cmd.fullName, cmd.location, cmd.bio)))
    case FollowUser(_, followeeId, undo) =>
      if (userId == followeeId)
        Left(s"Cannot follow self! $userId")
      else if (following.contains(followeeId) && !undo)
        Left("Already following this user")
      else if (!following.contains(followeeId) && undo)
        Left("Not following this user")
      else
        Right(Seq(UserFollowedUser(userId, followeeId, undo)))
    case SubmitPost(_, shoutId, shout) =>
      Right(Seq(UserPosted(userId, shoutId, shout)))
    case RemovePost(_, shoutId) =>
      if (!posts.contains(shoutId))
        Left("No such shout")
      else
        Right(Seq(UserRemovedPost(userId, shoutId)))
    case LikePost(_, shoutId, undo) =>
      if (likes.contains(shoutId) && !undo)
        Left("Already liked")
      else if (!likes.contains(shoutId) && undo)
        Left("Shout isn't liked")
      else
        Right(Seq(UserLikedPost(userId, shoutId, undo)))
    case cmd => Left(s"Unhandled command: $cmd")
  }

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