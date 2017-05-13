package com.pucilowski.shoutflow

import java.util.Properties

import com.pucilowski.shoutflow.events._
import com.pucilowski.shoutflow.events.derived.{PostDelivered, UserFollowedByUser, UserPostLiked}
import com.pucilowski.shoutflow.flink.joins.{PostDistributor, UserLikeJoiner}
import com.pucilowski.shoutflow.flink.projections._
import com.pucilowski.shoutflow.flink.schema.UserEventDeserializer
import com.pucilowski.shoutflow.flink.sinks._
import com.pucilowski.shoutflow.flink.sinks.indexers._
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
import org.apache.flink.util.Collector

object Projections {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.registerTypeWithKryoSerializer(classOf[java.util.UUID], classOf[de.javakaffee.kryoserializers.UUIDSerializer])
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("group.id", "ProjectionsJob")


    val userEvents: DataStream[UserEvent] = env
      .addSource(new FlinkKafkaConsumer010("shouterEvents", new UserEventDeserializer, props))
      .name("UserEvent")

    /*userEvents
      .map(e => s"[Evt] $e")
      .print()
      .name("print()")*/

    val userCreated = userEvents
      .flatMap { (event, out: Collector[UserCreated]) =>
        event match {
          case evt: UserCreated => out.collect(evt)
          case _ =>
        }
      }
      .name("UserCreated")

    val userFollowedUser = userEvents
      .flatMap { (event, out: Collector[UserFollowedUser]) =>
        event match {
          case followed: UserFollowedUser => out.collect(followed)
          case _ =>
        }
      }
      .name("UserFollowedUser")

    val userFollowedByUser = userEvents
      .flatMap { (event, out: Collector[UserFollowedByUser]) =>
        event match {
          case UserFollowedUser(userId, followeeId, undo) => out.collect(UserFollowedByUser(followeeId, userId, undo))
          case _ =>
        }
      }
      .name("UserFollowedByUser")

    val userPostedEvents = userEvents
      .flatMap { (event, out: Collector[UserPosted]) =>
        event match {
          case evt: UserPosted => out.collect(evt)
          case _ =>
        }
      }
      .name("UserPosted")

    val userLikedPost = userEvents
      .flatMap { (event, out: Collector[UserLikedPost]) =>
        event match {
          case followed: UserLikedPost => out.collect(followed)
          case _ =>
        }
      }
      .name("UserLikedPost")

    val userProfileUpdates: DataStream[UserEvent] = userEvents
      .flatMap { (event, out: Collector[UserEvent]) =>
        event match {
          case evt: UserCreated => out.collect(evt)
          case evt: UserProfileUpdated => out.collect(evt)
          case _ =>
        }
      }
      .name("UserProfileUpdated")

    val userNameUpdateEvents: DataStream[UserNameUpdateEvent] = userEvents
      .flatMap { (event, out: Collector[UserNameUpdateEvent]) =>
        event match {
          case evt: UserNameUpdateEvent => out.collect(evt)
          case _ =>
        }
      }
      .name("UserNameUpdateEvent")

    val userPostEvents: DataStream[UserPostEvent] = userEvents
      .flatMap { (event, out: Collector[UserPostEvent]) =>
        event match {
          case evt: UserPostEvent => out.collect(evt)
          case _ =>
        }
      }
      .name("UserPostEvent")


    /**
      * Joins
      */

    val selfDelivers: DataStream[PostDelivered] = userPostEvents
      .flatMap { (event, out: Collector[PostDelivered]) =>
        event match {
          case evt: UserPosted => out.collect(PostDelivered(evt.userId, evt.postId))
          case _ =>
        }
      }
      .name("PostDelivered (self)")

    val fanOuts: DataStream[PostDelivered] = userFollowedUser
      .keyBy(_.followeeId)
      .connect(userPostEvents.broadcast.keyBy(_.userId))
      .flatMap(new PostDistributor)
      .name("PostDelivered (fan-out)")


    val userPostLikedEvents: DataStream[UserPostLiked] = userPostedEvents
      .keyBy(_.postId)
      .connect(userLikedPost.broadcast.keyBy(_.postId))
      .flatMap(new UserLikeJoiner)
      .name("UserPostLiked")

    /**
      * Projections
      */
    val userReadModels = userEvents
      .keyBy(_.userId)
      .connect(userFollowedByUser.broadcast.keyBy(_.userId))
      .process(new UserProjector)
      .name("UserProjector")

    val postReadModels = userEvents
      .keyBy(_.userId)
      .connect(userPostLikedEvents.keyBy(_.userId))
      .process(new PostProjector)
      .name("PostProjector")

    /**
      * Sinks
      */
    userReadModels
      .addSink(new UserReadModelSink)
      .name("UserReadModelSink")

    postReadModels
      .addSink(new PostReadModelSink)
      .name("PostReadModelSink")

    userCreated
      .keyBy(_.userId)
      .addSink(new UserIndexer)
      .name("UserIndexer")

    userNameUpdateEvents
      .keyBy(_.userId)
      .addSink(new UserNameIndexer)
      .name("UserNameIndexer")

    userPostedEvents
      .keyBy(_.userId)
      .addSink(new UserPostIndexer)
      .name("UserPostIndexer")

    userFollowedUser
      .keyBy(_.userId)
      .addSink(new UserFollowIndexer)
      .name("UserFollowIndexer")

    userLikedPost
      .keyBy(_.userId)
      .addSink(new UserLikeIndexer)
      .name("UserLikeIndexer")

    selfDelivers
      .union(fanOuts)
      .keyBy(_.recipientId)
      .addSink(new UserFeedIndexer)
      .name("UserFeedIndexer")

    println(env.getExecutionPlan)

    env.execute("Projections")
  }
}


