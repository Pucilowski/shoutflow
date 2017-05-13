package com.pucilowski.shoutflow

import java.util.{Properties, UUID}
import java.util.concurrent.Future

import com.pucilowski.shoutflow.commands.Command
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.json4s.JsonAST.{JNull, JString}
import org.json4s.{CustomSerializer, DefaultFormats, Formats, JValue, Serializer, ShortTypeHints, TypeInfo}
import org.json4s.native.Serialization


/**
  * Hello world!
  *
  */

trait CommandPublisher {
  def send(command: Command): Boolean
}

class CommandPublisherImpl(props: Properties) extends CommandPublisher {

  val producer = new KafkaProducer[String, String](props)

  val topic = props.getProperty("kafka.topic.commands")

  override def send(command: Command): Boolean = {
    import com.pucilowski.shoutflow.commands.UserCommand._

    val message = Serialization.write(command)

    val record = new ProducerRecord[String, String](topic, message)
    val future: Future[RecordMetadata] = producer.send(record)

    val x = future.get()
    future.isDone
  }
}

class CommandSerializer extends Serializer[Command] {
  override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Command] = ???

  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = ???
}

case object UUIDSerializer extends CustomSerializer[UUID](format => ( {
  case JString(s) => UUID.fromString(s)
  case JNull => null
}, {
  case x: UUID => JString(x.toString)
}
)
)