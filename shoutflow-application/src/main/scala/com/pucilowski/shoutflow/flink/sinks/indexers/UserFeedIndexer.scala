package com.pucilowski.shoutflow.flink.sinks.indexers

import com.pucilowski.shoutflow.events.derived.PostDelivered
import io.deepstream.DeepstreamClient
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction

class UserFeedIndexer extends RichSinkFunction[PostDelivered] {
  lazy val ds: DeepstreamClient = {
    val client = new DeepstreamClient("ws://localhost:6020")
    client.login()
    client
  }

  override def invoke(in: PostDelivered): Unit = {
    val list = ds.record.getList(s"user/${in.recipientId}/feed")
    list.addEntry(s"post/${in.shoutId}", 0)
  }
}