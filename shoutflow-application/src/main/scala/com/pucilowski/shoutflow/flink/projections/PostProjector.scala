package com.pucilowski.shoutflow.flink.projections

import java.util.UUID

import com.google.gson.{Gson, GsonBuilder}
import com.pucilowski.shoutflow.events._
import com.pucilowski.shoutflow.events.derived.UserPostLiked
import com.pucilowski.shoutflow.models.UserProfile
import com.pucilowski.shoutflow.projections.PostReadModel
import io.deepstream.DeepstreamClient
import org.apache.flink.api.common.state.{MapState, MapStateDescriptor, ValueState, ValueStateDescriptor}
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector

class PostProjector extends CoProcessFunction[UserEvent, UserPostLiked, PostReadModel] {

  lazy val userProfileUpdatedState: ValueState[UserProfile] = getRuntimeContext.getState(
    new ValueStateDescriptor[UserProfile]("UserProfile", classOf[UserProfile]))

  lazy val readModelMapState: MapState[UUID, PostReadModel] = getRuntimeContext.getMapState(
    new MapStateDescriptor[UUID, PostReadModel]("PostReadModels", classOf[UUID], classOf[PostReadModel]))

  lazy val ds: DeepstreamClient = {
    val client = new DeepstreamClient("ws://localhost:6020")
    client.login()
    client
  }

  lazy val gson: Gson = new GsonBuilder().setPrettyPrinting().create()

  override def processElement1(event: UserEvent, context: CoProcessFunction[UserEvent, UserPostLiked, PostReadModel]#Context, collector: Collector[PostReadModel]): Unit = {

    def updateModels(profile: UserProfile): Iterable[PostReadModel] = {
      import scala.collection.JavaConverters._
      readModelMapState.values().asScala.map(rm => {
        rm.copy(userName = profile.userName, fullName = profile.fullName)
      })
    }

    val newModel: Iterable[PostReadModel] = event match {
      case evt: UserCreated =>
        userProfileUpdatedState.update(UserProfile(evt.avatarHash, evt.userName, evt.fullName, "", ""))
        Seq.empty
      case evt: UserNameChanged =>
        val profile = userProfileUpdatedState.value()
          .copy(userName = evt.userName)
        userProfileUpdatedState.update(profile)
        updateModels(profile)
      case evt: UserProfileUpdated =>
        val profile = userProfileUpdatedState.value()
          .copy(avatarHash = evt.avatarHash, fullName = evt.fullName, location = evt.location, bio = evt.bio)
        userProfileUpdatedState.update(profile)
        updateModels(profile)
      case evt: UserPosted =>
        val profile = userProfileUpdatedState.value()
        Seq(PostReadModel.seed(profile.avatarHash, profile.userName, profile.fullName, context.timestamp(), evt))
      case evt: UserPostEvent =>
        val model = readModelMapState.get(evt.postId)
        Seq(PostReadModel.handle(model, evt))
      case _ =>
        Seq.empty
    }

    newModel.foreach(model => {
      collector.collect(model)
      readModelMapState.put(model.postId, model)
    })
  }

  override def processElement2(evt: UserPostLiked, context: CoProcessFunction[UserEvent, UserPostLiked, PostReadModel]#Context, collector: Collector[PostReadModel]): Unit = {
    val model = readModelMapState.get(evt.postId)
    val newModel = PostReadModel.handle(model, evt)

    collector.collect(newModel)
    readModelMapState.put(newModel.postId, newModel)
  }
}