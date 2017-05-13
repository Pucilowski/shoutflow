package com.pucilowski.shoutflow.flink.sinks.indexers

import com.pucilowski.shoutflow.events.UserLikedPost
import io.deepstream.DeepstreamClient
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction

class UserLikeIndexer extends RichSinkFunction[UserLikedPost] {
  lazy val ds: DeepstreamClient = {
    val client = new DeepstreamClient("ws://localhost:6020")
    client.login()
    client
  }

  override def invoke(evt: UserLikedPost): Unit = {
    val list = ds.record.getList(s"user/${evt.userId}/likes")
    val postPath = s"post/${evt.postId}"

    if (!evt.undo) {
      list.addEntry(postPath)
      println(s"Added ${evt.postId} to ${evt.userId}'s likes")
    } else {
      list.removeEntry(postPath)
      println(s"Removed ${evt.postId} to ${evt.userId}'s likes")
    }
  }
}