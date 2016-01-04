package me.enkode.akka.service.discovery.cluster

import akka.serialization.SerializerWithStringManifest
import argonaut.CodecJson
import me.enkode.akka.service.discovery.core._

class JsonAkkaSerializer extends SerializerWithStringManifest {
  import argonaut._, Argonaut._

  override def identifier: Int = 880004
  override def manifest(o: AnyRef): String = o.getClass.getName

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    if (manifest == classOf[Heartbeat].getName) {
      Parse.decode[Heartbeat](new String(bytes))
    } else if (manifest == classOf[Observation].getName) {
      Parse.decode[Observation](new String(bytes))
    } else {
      throw new RuntimeException("not implemented")
    }

  }

  override def toBinary(obj: AnyRef): Array[Byte] = {
    val json: Json = obj match {
      case h: Heartbeat ⇒ h.asJson
      case o: Observation ⇒ o.asJson
    }
    json.nospaces.getBytes
  }
}
