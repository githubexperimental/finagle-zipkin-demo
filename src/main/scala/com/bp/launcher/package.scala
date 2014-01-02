package com.bp

import com.twitter.common.zookeeper.ZooKeeperClient
import com.twitter.common.quantity.Amount
import java.net.InetSocketAddress
import com.twitter.common.quantity.Time
import com.twitter.common.zookeeper.ServerSetImpl
import com.twitter.finagle.zookeeper.ZookeeperServerSetCluster
import com.twitter.finagle.zipkin.thrift.ZipkinTracer

package object launcher {
  val zkClient = new ZooKeeperClient(Amount.of(100, Time.MILLISECONDS), new InetSocketAddress("localhost", 2181))
  val backendServerSet = new ServerSetImpl(zkClient, "/backend")
  val backendServerSetCluster = new ZookeeperServerSetCluster(backendServerSet)
  val frontendServerSet = new ServerSetImpl(zkClient, "/frontend")
  val frontendServerSetCluster = new ZookeeperServerSetCluster(frontendServerSet)
  val zipkinTracer = ZipkinTracer.mk(host = "10.1.251.180", port = 9410, sampleRate = 1.0f)
  
}

