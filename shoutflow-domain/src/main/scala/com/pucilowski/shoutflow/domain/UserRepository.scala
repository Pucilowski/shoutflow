package com.pucilowski.shoutflow.domain

import java.util.UUID

import com.pucilowski.shoutflow.events.UserCreated

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

trait UserRepository {
  def get(id: UUID): Future[UserRoot]
}

class ConcreteUserRepository extends UserRepository {

  implicit val executor: ExecutionContext =  scala.concurrent.ExecutionContext.global

  override def get(id: UUID): Future[UserRoot] = Future {
    UserRoot.seed(UserCreated(UUID.randomUUID(), "", "", ""))
  }
}
