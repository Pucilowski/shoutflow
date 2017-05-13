package com.pucilowski.shoutflow.flink.sinks.indexers

import com.pucilowski.shoutflow.events.UserPosted
import io.deepstream.DeepstreamClient
import org.apache.flink.streaming.api.functions.sink.SinkFunction

class UserPostIndexer extends SinkFunction[UserPosted] {
  lazy val ds: DeepstreamClient = {
    val client = new DeepstreamClient("ws://localhost:6020")
    client.login()
    client
  }

  override def invoke(evt: UserPosted): Unit = {
    val list = ds.record.getList(s"user/${evt.userId}/posts")
    list.addEntry(s"post/${evt.postId}", 0)
    println(s"Added ${evt.postId} to ${evt.userId}'s feed")

    val list2 = ds.record.getList(s"posts")
    list2.addEntry(s"post/${evt.postId}", 0)
    println(s"Added ${evt.postId} to the all feed")
  }
}
