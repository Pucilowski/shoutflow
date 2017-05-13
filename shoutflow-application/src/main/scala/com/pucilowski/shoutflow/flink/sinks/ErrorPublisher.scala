package com.pucilowski.shoutflow.flink.sinks

import io.deepstream.DeepstreamClient
import org.apache.flink.streaming.api.functions.sink.SinkFunction

class ErrorPublisher extends SinkFunction[String] {

  lazy val ds: DeepstreamClient = {
    val client = new DeepstreamClient("ws://localhost:6020")
    client.login()
    client
  }

  override def invoke(in: String): Unit = {
    //ds.event.emit(s"errors", in)
    println(s"[Err] $in")
  }
}
