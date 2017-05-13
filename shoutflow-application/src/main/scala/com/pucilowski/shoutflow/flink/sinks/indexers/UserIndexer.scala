package com.pucilowski.shoutflow.flink.sinks.indexers

import com.pucilowski.shoutflow.events.UserCreated
import io.deepstream.DeepstreamClient
import org.apache.flink.streaming.api.functions.sink.SinkFunction

class UserIndexer extends SinkFunction[UserCreated] {
  lazy val ds: DeepstreamClient = {
    val client = new DeepstreamClient("ws://localhost:6020")
    client.login()
    client
  }

  override def invoke(evt: UserCreated): Unit = {
    val list = ds.record.getList(s"users")
    list.addEntry(s"user/${evt.userId}", 0)

    println(s"Added ${evt.userId} to the all users")
  }
}
