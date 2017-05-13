package com.pucilowski.shoutflow

import java.util.UUID

import org.json4s.ext.UUIDSerializer
import org.json4s.{DefaultFormats, Formats, ShortTypeHints}

package object events {

  trait DomainEvent

  trait UserEvent extends DomainEvent {
    val userId: UUID
  }

  trait UserPostEvent extends UserEvent {
    val postId: UUID
  }

  trait UserNameUpdateEvent extends UserEvent {
    val userName: String
  }

  case class UserCreated(userId: UUID, avatarHash: String, userName: String, fullName: String) extends UserEvent with UserNameUpdateEvent

  case class UserNameChanged(userId: UUID, userName: String) extends UserNameUpdateEvent

  case class UserProfileUpdated(userId: UUID, avatarHash: String, fullName: String, location: String, bio: String) extends UserEvent

  case class UserFollowedUser(userId: UUID, followeeId: UUID, undo: Boolean = false) extends UserEvent

  case class UserPosted(userId: UUID, postId: UUID, body: String) extends UserPostEvent

  case class UserRemovedPost(userId: UUID, postId: UUID) extends UserPostEvent

  case class UserLikedPost(userId: UUID, postId: UUID, undo: Boolean = false) extends UserEvent

  object UserEvent {
    implicit val formats: Formats = new DefaultFormats {
      //override val typeHintFieldName = "type"
      override val typeHints = ShortTypeHints(List(
        classOf[UserCreated], classOf[UserNameChanged], classOf[UserProfileUpdated],
        classOf[UserFollowedUser],
        classOf[UserPosted], classOf[UserRemovedPost], classOf[UserLikedPost]
      ))
    } + UUIDSerializer
  }

}