package com.pucilowski.shoutflow.flink.sinks

import com.google.gson.{Gson, GsonBuilder}
import com.pucilowski.shoutflow.projections.PostReadModel
import io.deepstream.{DeepstreamClient, Record}
import org.apache.flink.streaming.api.functions.sink.SinkFunction

class PostReadModelSink extends SinkFunction[PostReadModel] {
  lazy val ds: DeepstreamClient = {
    val client = new DeepstreamClient("ws://localhost:6020")
    client.login()
    client
  }

  lazy val gson: Gson = new GsonBuilder().setPrettyPrinting().create()

  private def getRecord(readModel: PostReadModel) = {
    ds.record.getRecord(s"post/${readModel.postId}")
  }

  override def invoke(readModel: PostReadModel): Unit = {
    val record: Record = getRecord(readModel)

    val tree = gson.toJsonTree(readModel)
    record.set(tree)

    //println(gson.toJson(tree))
    println(readModel)
  }

}
