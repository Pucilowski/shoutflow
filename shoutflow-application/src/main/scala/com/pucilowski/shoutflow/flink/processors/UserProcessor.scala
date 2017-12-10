package com.pucilowski.shoutflow.flink.processors

import com.pucilowski.shoutflow.commands.{CreateUser, UserCommand}
import com.pucilowski.shoutflow.domain.{ConcreteUserRepository, UserBehavior, UserRepository, UserRoot}
import com.pucilowski.shoutflow.events._
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala.OutputTag
import org.apache.flink.util.Collector

class UserProcessor extends ProcessFunction[UserCommand, UserEvent] {

  lazy val behavior = new UserBehavior(new ConcreteUserRepository)

  lazy val state: ValueState[UserRoot] = getRuntimeContext.getState(
    new ValueStateDescriptor("UserAggregate", classOf[UserRoot]))

  override def processElement(in: UserCommand, context: ProcessFunction[UserCommand, UserEvent]#Context, collector: Collector[UserEvent]): Unit = {
    processCommand(in) match {
      case Left(error) =>
        context.output(UserProcessor.errorsOutput, error)
      case Right(events) => events.foreach(e => {
        val root = e match {
          case evt: UserCreated =>
            UserRoot.seed(evt)
          case evt =>
            val user = state.value()
            user.apply(evt)
            user
        }

        state.update(root)
        collector.collect(e)
      })
    }
  }

  private def processCommand(cmd: UserCommand): Either[String, Seq[UserEvent]] = cmd match {
    case cmd: CreateUser =>
      Right(Seq(UserRoot.seed(cmd)))
    case _ =>
      val user = state.value()
      behavior.handle(user, cmd)
  }
}

object UserProcessor {
  val errorsOutput: OutputTag[String] = OutputTag[String]("errors")
}
