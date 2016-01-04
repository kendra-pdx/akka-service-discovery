package me.enkode.akka.service.discovery

import java.time.Instant

import argonaut._, Argonaut._
import enumeratum.{EnumEntry, Enum}
import me.enkode.akka.service.discovery.core._

import scala.concurrent.duration._
import scala.language.implicitConversions

package object cluster {
  def enumCodec[T <: EnumEntry](toValue: String ⇒ T): CodecJson[T] = {
    val encoder: T ⇒ Json = t ⇒ jString(t.entryName)
    val decoder: HCursor ⇒ DecodeResult[T] = json ⇒ for {
      name ← json.as[String]
    } yield {
      toValue(name)
    }
    CodecJson(encoder, decoder)
  }

  implicit val instantCodec: CodecJson[Instant] = {
    CodecJson(i ⇒ jNumber(i.toEpochMilli), json ⇒ for {
      epochMilli ← json.as[Long]
    } yield {
      Instant.ofEpochMilli(epochMilli)
    })
  }

  implicit val finiteDurationCodec: CodecJson[FiniteDuration] = {
    CodecJson(d ⇒ jNumber(d.toMillis), _.as[Long].map(d ⇒ FiniteDuration(d, MILLISECONDS)))
  }

  implicit val rackCodec: CodecJson[Rack] = {
    val encoder: Rack ⇒ Json = {
      case Cloud.Local.LocalRack ⇒ jString("local")
      case az: Cloud.AWS.AZ ⇒ jString(az.azId)
    }

    val decoder: HCursor ⇒ DecodeResult[Rack] = _.as[String] map {
      case "local" ⇒ Cloud.Local.LocalRack
      case azId ⇒ Cloud.AWS.AZ.fromId(azId)
    }

    CodecJson(encoder, decoder)
  }

  implicit val schemeCodec = enumCodec[Scheme](Scheme.withName)
  implicit val statusCodec = enumCodec[Status](Status.withName)

  implicit val hostCodec = casecodec2(Host.apply, Host.unapply)("publicName", "rack")
  implicit val serviceCodec = casecodec1(Service.apply, Service.unapply)("serviceId")
  implicit val accessCodec = casecodec3(Access.apply, Access.unapply)("scheme", "host", "port")
  implicit val instanceCodec = casecodec3(Instance.apply, Instance.unapply)("instanceId", "service", "access")

  implicit val heartbeatMetaCodec = casecodec2(HeartbeatMeta.apply, HeartbeatMeta.unapply)("cpuLoad", "memoryLoad")
  implicit val heartbeatCodec = casecodec4(Heartbeat.apply, Heartbeat.unapply)("instance", "meta", "when", "status")

  implicit val observationMetaCodec = casecodec1(ObservationMeta.apply, ObservationMeta.unapply)("latency")
  implicit val observationCodec = casecodec5(Observation.apply, Observation.unapply)("instance", "observedBy", "meta", "when", "status")
}
