package com.pucilowski.shoutflow.flink.projections

import com.pucilowski.shoutflow.events._
import com.pucilowski.shoutflow.events.derived.UserFollowedByUser
import com.pucilowski.shoutflow.projections.UserReadModel
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector

class UserProjector extends CoProcessFunction[UserEvent, UserFollowedByUser, UserReadModel] {
  lazy val state: ValueState[UserReadModel] = getRuntimeContext
    .getState(new ValueStateDescriptor[UserReadModel]("UserProfileReadModel", classOf[UserReadModel]))

  override def processElement1(event: UserEvent, context: CoProcessFunction[UserEvent, UserFollowedByUser, UserReadModel]#Context, collector: Collector[UserReadModel]): Unit = {
    val rm = event match {
      case evt: UserCreated =>
        Some(UserReadModel.seed(evt))
      case evt: UserEvent =>
        Some(UserReadModel.handle(state.value(), evt))
      case _ => None
    }

    rm.foreach(rm => {
      collector.collect(rm)
      state.update(rm)
    })
  }

  override def processElement2(event: UserFollowedByUser, context: CoProcessFunction[UserEvent, UserFollowedByUser, UserReadModel]#Context, collector: Collector[UserReadModel]): Unit = {
    val rm = UserReadModel.handle(state.value(), event)

    collector.collect(rm)
    state.update(rm)
  }
}


