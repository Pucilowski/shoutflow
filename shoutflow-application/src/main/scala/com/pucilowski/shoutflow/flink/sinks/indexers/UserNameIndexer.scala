package com.pucilowski.shoutflow.flink.sinks.indexers

import com.google.gson.{Gson, GsonBuilder, JsonObject}
import com.pucilowski.shoutflow.events._
import io.deepstream.DeepstreamClient
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction

class UserNameIndexer extends RichSinkFunction[UserNameUpdateEvent] {
  lazy val state: ValueState[UserNameUpdateEvent] = getRuntimeContext.getState(
    new ValueStateDescriptor[UserNameUpdateEvent]("lastProfileUpdate2", classOf[UserNameUpdateEvent]))

  lazy val ds: DeepstreamClient = {
    val client = new DeepstreamClient("ws://localhost:6020")
    client.login()
    client
  }

  lazy val gson: Gson = new GsonBuilder().setPrettyPrinting().create()

  override def invoke(in: UserNameUpdateEvent): Unit = {
    val lastUpdate = state.value()

    if (lastUpdate != null && in.userName != lastUpdate.userName) {
      val oldNormalizedUserName = lastUpdate.userName.toLowerCase
      val oldUserIdRecord = ds.record.getRecord(s"userName/$oldNormalizedUserName")

      oldUserIdRecord.delete()
    }

    if (lastUpdate == null || in.userName != lastUpdate.userName) {
      val normalizedUserName = in.userName.toLowerCase
      val userIdRecord = ds.record.getRecord(s"userName/$normalizedUserName")

      val document = new JsonObject
      document.addProperty("userId", in.userId.toString)
      userIdRecord.set(gson.toJsonTree(document))
    }

    state.update(in)
  }
}