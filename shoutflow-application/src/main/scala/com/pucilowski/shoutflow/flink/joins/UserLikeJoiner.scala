package com.pucilowski.shoutflow.flink.joins

import com.pucilowski.shoutflow.events.derived.UserPostLiked
import com.pucilowski.shoutflow.events.{UserLikedPost, UserPosted}
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction
import org.apache.flink.util.Collector

class UserLikeJoiner extends RichCoFlatMapFunction[UserPosted, UserLikedPost, UserPostLiked] {
  lazy val state: ValueState[UserPosted] = getRuntimeContext
    .getState(new ValueStateDescriptor[UserPosted]("userPosted", classOf[UserPosted]))

  override def flatMap1(evt: UserPosted, collector: Collector[UserPostLiked]): Unit = {
    state.update(evt)
    println(s"Now distributing for: $evt")
  }

  override def flatMap2(evt: UserLikedPost, collector: Collector[UserPostLiked]): Unit = {
    val userPosted = state.value()

    if (userPosted != null) {
      val e = UserPostLiked(userPosted.userId, userPosted.postId, evt.undo)
      collector.collect(e)
      println(s"$e")
    }
  }
}