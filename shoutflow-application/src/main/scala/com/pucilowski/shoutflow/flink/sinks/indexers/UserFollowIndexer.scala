package com.pucilowski.shoutflow.flink.sinks.indexers

import com.pucilowski.shoutflow.events.UserFollowedUser
import io.deepstream.DeepstreamClient
import org.apache.flink.streaming.api.functions.sink.SinkFunction

class UserFollowIndexer extends SinkFunction[UserFollowedUser] {
  lazy val ds: DeepstreamClient = {
    val client = new DeepstreamClient("ws://localhost:6020")
    client.login()
    client
  }

  override def invoke(evt: UserFollowedUser): Unit = {
    // add followee
    index(s"user/${evt.userId}/following", s"user/${evt.followeeId}", evt.undo)

    // add follower
    index(s"user/${evt.followeeId}/followers", s"user/${evt.userId}", evt.undo)
  }

  private def index(listPath: String, itemPath: String, undo: Boolean): Unit = {
    val list = ds.record.getList(listPath)

    if (!undo) {
      list.addEntry(itemPath, 0)
    } else {
      list.removeEntry(itemPath)
    }
  }
}