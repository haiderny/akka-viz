package akka.viz.serialization

import java.util

import upickle.Js
import upickle.json.FastRenderer
import scala.collection.JavaConversions._

case class SerializationContextImpl(depth: Int = 0) extends SerializationContext

object MessageSerialization extends SerializerFinder with ReflectiveSerialization {

  def preload() = {

  }

  def render(message: Any): String = {
    message match {
      case json: Js.Value => FastRenderer.render(json)
      case other          => FastRenderer.render(serialize(other, newSerializationContext))
    }
  }

  private def newSerializationContext: SerializationContext = SerializationContextImpl()

  def serialize(message: Any, serializationContext: SerializationContext): Js.Value = {
    def unableToSerialize(t: Throwable): Js.Value = {
      Js.Obj("error" -> Js.Str(s"Failed to serialize: ${t.getMessage} (${t.getClass.getName})"))
    }
    try {
      if (message == null) {
        Js.Null
      } else {
        getSerializerFor(message).serialize(message, SerializationContextImpl(serializationContext.depth + 1))
      }
    } catch {
      case t: Throwable => unableToSerialize(t)
    }
  }

  private val serializers: List[AkkaVizSerializer] = findSerializers

  private val mappers = DefaultSerializers.mappers

  private def getSerializerFor(obj: Any): AkkaVizSerializer = {
    def findSerializerForObject: AkkaVizSerializer = {
      serializers.find(_.canSerialize(obj)).getOrElse {
        println(s"WARNING: There is no serializer for ${obj.getClass.getName}, consider implementing AkkaVizSerializer")
        reflectiveSerializer
      }
    }
    mappers.getOrElseUpdate(obj.getClass, findSerializerForObject)
  }

  private val reflectiveSerializer = new AkkaVizSerializer {
    override def canSerialize(obj: Any): Boolean = {
      false
    }

    def serialize(obj: Any, context: SerializationContext): Js.Value = {
      reflectiveSerialize(obj, context)
    }
  }

}
