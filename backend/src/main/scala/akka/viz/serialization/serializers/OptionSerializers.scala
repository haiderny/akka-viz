package akka.viz.serialization.serializers

import akka.actor.ActorRef
import akka.viz.serialization.{MessageSerialization, AkkaVizSerializer}
import upickle.Js

case object OptionSerializer extends AkkaVizSerializer {
  override def serialize(obj: Any): Js.Value = {
    obj match {
      case Some(x) => Js.Obj("$type" -> MessageSerialization.serialize(x))
      case None => Js.Obj("$type" -> Js.Str(s"scala.util.None"))
    }
  }

  override def canSerialize(obj: Any): Boolean = obj match {
    case t: Option[_] => true
    case _ => false
  }
}


