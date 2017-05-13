package com.pucilowski.shoutflow.flink

import com.pucilowski.shoutflow.commands.UserCommand
import com.pucilowski.shoutflow.events.UserEvent
import org.apache.flink.streaming.util.serialization.{AbstractDeserializationSchema, SerializationSchema}
import org.json4s.native.Serialization

package object schema {

  class UserCommandDeserializer extends AbstractDeserializationSchema[UserCommand] {
    override def deserialize(bytes: Array[Byte]): UserCommand = {
      val line = new String(bytes)
      import UserCommand._
      Serialization.read[UserCommand](line)
    }
  }

  class UserEventSerializer extends SerializationSchema[UserEvent] {
    override def serialize(t: UserEvent): Array[Byte] = {
      import UserEvent._
      val string = Serialization.write(t)
      string.getBytes
    }
  }

  class UserEventDeserializer extends AbstractDeserializationSchema[UserEvent] {
    override def deserialize(bytes: Array[Byte]): UserEvent = {
      val line = new String(bytes)

      import UserEvent._

      Serialization.read[UserEvent](line)
    }
  }

}