package com.pucilowski.shoutflow

import java.util.Properties

import com.pucilowski.shoutflow.commands.UserCommand
import com.pucilowski.shoutflow.events._
import com.pucilowski.shoutflow.flink.processors.UserProcessor
import com.pucilowski.shoutflow.flink.schema.{UserCommandDeserializer, UserEventSerializer}
import com.pucilowski.shoutflow.flink.sinks.ErrorPublisher
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010, FlinkKafkaProducer010}

object Application {

  val consumerProps = new Properties()
  consumerProps.put("bootstrap.servers", "localhost:9092")
  consumerProps.put("group.id", "ApplicationJob")

  val producerProps = new Properties()
  producerProps.put("bootstrap.servers", "localhost:9092")

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    env.registerTypeWithKryoSerializer(classOf[java.util.UUID], classOf[de.javakaffee.kryoserializers.UUIDSerializer])

    //env.getConfig.enableForceKryo()
    //env.getConfig.addDefaultKryoSerializer(classOf[java.util.UUID], classOf[de.javakaffee.kryoserializers.UUIDSerializer])
    //env.registerTypeWithKryoSerializer(classOf[java.util.UUID], classOf[de.javakaffee.kryoserializers.UUIDSerializer])

    //env.setStateBackend(new RocksDBStateBackend)

    val commands: DataStream[UserCommand] = env
      .addSource(new FlinkKafkaConsumer010("shouterCommands", new UserCommandDeserializer, consumerProps))
      .name("UserCommand")

/*    commands
      .map(x => s"[Cmd] ${x.toString}")
      .name("x => s\"[Cmd] ${x.toString}\"")
      .print()
      .name("ShouterCommands.print()")*/

    val events = commands
      .keyBy(_.userId)
      .process(new UserProcessor)
      .name("UserProcessor")

    events.getSideOutput(UserProcessor.errorsOutput)
      .addSink(new ErrorPublisher)
      .name("ErrorPublisher")

/*    events
      .map(x => s"[Evt] ${x.toString}")
      .name("x => s\"[Evt] ${x.toString}\"")
      .print()
      .name("ShouterEvents.print()")*/

    events
      .addSink(new FlinkKafkaProducer010[UserEvent]("shouterEvents", new UserEventSerializer, producerProps))
      .name("UserEvent")

    println(env.getExecutionPlan)

    env.execute("Application")
  }
}
