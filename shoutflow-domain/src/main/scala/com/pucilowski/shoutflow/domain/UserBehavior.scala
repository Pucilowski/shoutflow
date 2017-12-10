package com.pucilowski.shoutflow.domain

import com.pucilowski.shoutflow.commands._
import com.pucilowski.shoutflow.events._

class UserBehavior(userRepository: UserRepository) {

  def handle(aggr: UserRoot, command: UserCommand): Either[String, Seq[UserEvent]] = command match {
    case cmd: ChangeUserName =>
      Right(Seq(UserNameChanged(aggr.userId, cmd.userName)))
    case cmd: UpdateUserProfile =>
      Right(Seq(UserProfileUpdated(aggr.userId, cmd.avatarHash, cmd.fullName, cmd.location, cmd.bio)))
    case FollowUser(_, followeeId, undo) =>
      if (aggr.userId == followeeId)
        Left(s"Cannot follow self! ${aggr.userId}")
      else if (aggr.following.contains(followeeId) && !undo)
        Left("Already following this user")
      else if (!aggr.following.contains(followeeId) && undo)
        Left("Not following this user")
      else
        Right(Seq(UserFollowedUser(aggr.userId, followeeId, undo)))
    case SubmitPost(_, shoutId, shout) =>
      Right(Seq(UserPosted(aggr.userId, shoutId, shout)))
    case RemovePost(_, shoutId) =>
      if (!aggr.posts.contains(shoutId))
        Left("No such shout")
      else
        Right(Seq(UserRemovedPost(aggr.userId, shoutId)))
    case LikePost(_, shoutId, undo) =>
      if (aggr.likes.contains(shoutId) && !undo)
        Left("Already liked")
      else if (!aggr.likes.contains(shoutId) && undo)
        Left("Shout isn't liked")
      else
        Right(Seq(UserLikedPost(aggr.userId, shoutId, undo)))
    case cmd => Left(s"Unhandled command: $cmd")
  }

}
