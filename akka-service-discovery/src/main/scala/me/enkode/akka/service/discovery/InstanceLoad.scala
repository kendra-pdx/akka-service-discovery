package me.enkode.akka.service.discovery

import java.lang.management.ManagementFactory

object InstanceLoad {
  def memory(): Float = {
    val runtime = Runtime.getRuntime
    val max = runtime.maxMemory()
    val used = runtime.totalMemory()
    used / max
  }

  def cpu(): Float = {
    ManagementFactory.getOperatingSystemMXBean.getSystemLoadAverage.toFloat / Runtime.getRuntime.availableProcessors()
  }
}
