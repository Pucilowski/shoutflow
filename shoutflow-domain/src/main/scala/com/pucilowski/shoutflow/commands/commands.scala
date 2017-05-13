package com.pucilowski.shoutflow

import java.util.UUID

import org.json4s.ext.UUIDSerializer
import org.json4s.{DefaultFormats, Formats, ShortTypeHints}

package object commands {

  trait Command

  sealed trait UserCommand extends Command {
    val userId: UUID
  }

  case class CreateUser(userId: UUID, avatarHash: String, userName: String, fullName: String) extends UserCommand

  case class ChangeUserName(userId: UUID, userName: String) extends UserCommand

  case class UpdateUserProfile(userId: UUID, avatarHash: String, fullName: String, location: String, bio: String) extends UserCommand

  case class FollowUser(userId: UUID, followeeId: UUID, undo: Boolean = false) extends UserCommand

  case class SubmitPost(userId: UUID, shoutId: UUID, shout: String) extends UserCommand

  case class RemovePost(userId: UUID, shoutId: UUID) extends UserCommand

  case class LikePost(userId: UUID, shoutId: UUID, undo: Boolean = false) extends UserCommand

  object UserCommand {
    implicit val formats: Formats = new DefaultFormats {
      override val typeHints = ShortTypeHints(List(
        classOf[CreateUser], classOf[ChangeUserName], classOf[UpdateUserProfile],
        classOf[FollowUser],
        classOf[SubmitPost], classOf[RemovePost],
        classOf[LikePost]
      ))
    } + UUIDSerializer
  }

}