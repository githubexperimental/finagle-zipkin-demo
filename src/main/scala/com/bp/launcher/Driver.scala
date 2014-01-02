package com.bp.launcher
import com.twitter.finagle.builder.ClientBuilder
import java.net.InetSocketAddress
import com.twitter.finagle.http.Http
import com.bp.service.BackendService
import com.twitter.finagle.builder.ServerBuilder
import com.bp.service.BackendService
import com.bp.service.FrontendService
import com.bp.service.FrontendService
import com.twitter.finagle.builder.Server
import org.jboss.netty.handler.codec.http.DefaultHttpRequest
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.buffer.ChannelBuffers
import com.twitter.util.Future
import com.twitter.finagle.zipkin.thrift.ZipkinTracer
import java.util.logging.LogManager
import java.io.FileInputStream
import java.util.logging.Logger
import java.util.logging.Level

object ClientLauncher extends App {

  val logManager = LogManager.getLogManager().readConfiguration(new FileInputStream("src/main/resources/logging.properties"));
  val clientLogger = Logger.getLogger("clientlogger");
  val client = ClientBuilder()
    .cluster(frontendServerSetCluster)
    .name("main client")
    .hostConnectionLimit(1) // TODO testing
    .codec(Http().enableTracing(true))
    .retries(5)
    .tracerFactory(ZipkinTracer.mk(host = "10.1.251.180", port = 9410, sampleRate = 1.0f))
    .logger(clientLogger)
    .build()

//  val (backendServers, frontendServers) = startServers()
  makeRequests()
  //   stopServers(backendServers)
  //   stopServers(frontendServers)
  client.close().get()
  //   zkClient.close()
  clientLogger.log(Level.INFO, "All done\n")

  def makeRequests() = {
    val futures = (1 to 10) map (i => {
      print(i)
      val requestBody = ((i * 10) to (i * 10 + 9)).foldRight("")(_ + "," + _).dropRight(1)
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/")
      request.setHeader("Content-Type", "text/plain")
      request.setHeader("Content-length", requestBody.getBytes().length)
      val buffer = ChannelBuffers.copiedBuffer(requestBody, "utf-8")
      request.setContent(buffer);
      client.apply(request)
        .onSuccess(f => print("Successfully requested - " + requestBody + "\n"))
        .onFailure(f => print("Issues with - " + requestBody + "\n"))
    })

    val ready = Future.join(futures);
    ready.get()
    // All the requests are done!
  }

}

trait Serverlauncher {
  val logManager = LogManager.getLogManager().readConfiguration(new FileInputStream("src/main/resources/logging.properties"));
  //abstract val backend
  def startServer(port: Int) : Option[com.twitter.finagle.builder.Server]
  def startServers(portRange : Range): IndexedSeq[Option[Server]] =
  {
      portRange map { startServer(_) }
  }

  def stopServers(servers: IndexedSeq[Option[Server]]) {
    servers.foreach(f => f match {
      case None =>
      case Some(s: Server) => {
        print("closing - " + s.localAddress + "\n")
        s.close().get()
      }
    })
  }

}

object FrontendLauncher extends App with Serverlauncher {

  val frontendLogger = Logger.getLogger("frontendlogger");
  startServers(19031 to 19035)
  override def startServer(port: Int) : Option[com.twitter.finagle.builder.Server] = {
    try {
      val name = "frontend_localhost_" + port
      val address = new InetSocketAddress(port)
      val server = ServerBuilder()
        .codec(Http().enableTracing(true))
        .bindTo(address)
        .name(name)
        .tracerFactory(ZipkinTracer.mk(host = "10.1.251.180", port = 9410, sampleRate = 1.0f))
        .logger(frontendLogger)
        .daemon(true)
        .build(new FrontendService(name, backendServerSetCluster))
      frontendServerSetCluster.join(address)
      print("Started - " + address + "\n")
      Some(server)
    } catch {
      case e: Exception => None // basically ignore the exception
    }
  }
}

object BackendLauncher extends App with Serverlauncher {
  val backendLogger = Logger.getLogger("backendLogger");
  startServers(18031 to 18035)
  
  
  override def startServer(port: Int) : Option[com.twitter.finagle.builder.Server] = {
    try {
      val name = "backend_localhost_" + port
      val address = new InetSocketAddress(port)
      val server = ServerBuilder()
        .codec(Http())
        .bindTo(address)
        .name(name)
        .tracerFactory(ZipkinTracer.mk(host = "10.1.251.180", port = 9410, sampleRate = 1.0f))
        .logger(backendLogger)
        .daemon(false)
        .build(new BackendService())
      backendServerSetCluster.join(address)
      backendLogger.info("Started - " + address)
      Some(server)
    } catch {
      case e: Exception => {
        backendLogger.info(e.toString())
        None // basically ignore the exception
      }
    }
  }

}