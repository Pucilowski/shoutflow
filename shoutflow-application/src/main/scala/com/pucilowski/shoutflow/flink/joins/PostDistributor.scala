package com.pucilowski.shoutflow.flink.joins

import com.pucilowski.shoutflow.events.derived.PostDelivered
import com.pucilowski.shoutflow.events.{UserFollowedUser, UserPostEvent}
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction
import org.apache.flink.util.Collector

class PostDistributor extends RichCoFlatMapFunction[UserFollowedUser, UserPostEvent, PostDelivered] {

  lazy val followed: ValueState[UserFollowedUser] = getRuntimeContext
    .getState(new ValueStateDescriptor[UserFollowedUser]("followed", classOf[UserFollowedUser]))

  override def flatMap1(in1: UserFollowedUser, collector: Collector[PostDelivered]): Unit = {
    if (!in1.undo)
      followed.update(in1)
    else
      followed.update(null)

    println(s"Now distributing for: $in1")
  }

  override def flatMap2(in2: UserPostEvent, collector: Collector[PostDelivered]): Unit = {
    if (followed.value() != null) {
      val e = PostDelivered(followed.value().userId, in2.postId)
      println(s"Now delivering: $in2 to $e")

      collector.collect(e)
    } else {
      println(s"No one to deliver to: $in2")
    }
  }
}