package com.bp.service
import org.jboss.netty.handler.codec.http._
import com.twitter.finagle.Service
import com.twitter.util.Future
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.builder.Cluster
import java.net.SocketAddress
import com.twitter.finagle.http.Http
import org.jboss.netty.buffer.ChannelBuffers
import com.twitter.finagle.zipkin.thrift.ZipkinTracer
import com.twitter.finagle.tracing.Trace
import com.twitter.finagle.tracing.Annotation
import scala.Array.canBuildFrom
import com.twitter.finagle.tracing.Tracer

class FrontendService(name : String, backendServerSetCluster: Cluster[SocketAddress], zipkinTracer : Tracer) extends Service[HttpRequest, HttpResponse] {
  //    val serverSetCluster=

  val client = ClientBuilder()
    .cluster(backendServerSetCluster)
    .hostConnectionLimit(1) // TODO testing
    .name("Client in FE Server " + name)
    .codec(Http().enableTracing(true))
    .tracerFactory(zipkinTracer)
    .retries(5)
    .build()

  def apply(request: HttpRequest): Future[HttpResponse] = {
    val content = request.getContent().array();
    val contentAsString = new String(content);
    print("In fronted service, requestBody = " + contentAsString + "\n")
    Trace.record(Annotation.Message(contentAsString))
    Trace.record(Annotation.BinaryAnnotation("contentLength", contentAsString.length() ))
    val futures = ",".r.split(contentAsString).map(x => {
	    val buffer = ChannelBuffers.copiedBuffer(x.trim(), "utf-8")
	    val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/")
	    request.setHeader("Content-Type", "text/plain")
	    request.setHeader("Content-length", x.trim().getBytes().length)
	    request.setContent(buffer)
	    client.apply(request)
    })
//     print("Before join in frontend services\n")
//    val join=Future.join(futures).onSuccess({x=>print("Succeeded\n")}).onFailure({rescueException => print("Failed\n")})
//    join.get()
//     print("After join in frontend services\n")
    Future(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
  }
}

class BackendService extends Service[HttpRequest, HttpResponse] {
  def apply(request: HttpRequest): Future[HttpResponse] = {
    val content = request.getContent().array();
    val contentAsString = new String(content);
    Trace.record(Annotation.Message(contentAsString))
    print("In backend service, requestBody = " + contentAsString + "\n")
//    print("Trace.isActivelyTracing" + Trace.isActivelyTracing)
//    print(Trace.record("In backend service, requestBody = " + contentAsString + "\n"))
    Future(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
  }
}
