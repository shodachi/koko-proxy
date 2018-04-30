package com.lightbend.akka.http.sample;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.pattern.PatternsCS;
import akka.stream.ActorMaterializer;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;

import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import scala.concurrent.duration.Duration;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class KokoProxyServer extends AllDirectives {


    Timeout timeout = new Timeout(Duration.create(30, TimeUnit.SECONDS)); // usually we'd obtain the timeout from the system's configuration

    public static void main(String[] args) throws Exception {
        //#server-bootstrapping
        // boot up server using the route as defined below
        ActorSystem system = ActorSystem.create("kokoProxyHttpServer");

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final KokoProxyServer app = new KokoProxyServer();
        //#server-bootstrapping

        ActorRef directContentRequestActor = system.actorOf(DirectRequestActor.props(), "directRequestActor");
        ActorRef requestCacheActor = system.actorOf(ProxyCacheActor.props(), "proxyCacheActor");

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute(directContentRequestActor, requestCacheActor).flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", 3128), materializer);
        System.out.println("Server online at http://localhost:3128/");
    }

    private void createCache(ActorRef requestCacheActor, HttpRequest request, HttpResponse response) {
        PatternsCS.ask(requestCacheActor, new ProxyMessages.WriteCache(request,response), timeout);
    }

    public Route createRoute(ActorRef directContentRequestActor, ActorRef requestCacheActor) {

        final Route extractRequest = extractRequest(request -> {
                    CompletionStage<HttpResponse> futureHttpResponse = PatternsCS.ask(directContentRequestActor, request.getUri().toString(), timeout).
                            thenApply(obj -> (HttpResponse) obj);
                    return onSuccess(() -> futureHttpResponse, httpResponse ->
                            {
                                //TODO the httpResponse.entity is a stream so the second time it gets called (by either create cache or complete) it crashes
                                //Need to find a way to avoid this.. maybe toStrict might fix it ..
                                // or always return the cache after it was created..but I wanted to avoid to wait for the cache to be created to send the response..
                                //createCache(requestCacheActor, request, httpResponse);
                                //return complete("Dummy response");
                                return complete(httpResponse.entity());
                            }
                    );
                }
        );

        return get(() -> route(
                pathSingleSlash(() ->
                        extractRequest
                )
        ));
    }
}