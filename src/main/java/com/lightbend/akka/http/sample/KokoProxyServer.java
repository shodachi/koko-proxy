package com.lightbend.akka.http.sample;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.pattern.PatternsCS;
import akka.stream.ActorMaterializer;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.HttpEntities;

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

        ActorRef directContentRequestActor = system.actorOf(DirectContentRequestActor.props(), "directContentRequestActor");

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute(directContentRequestActor).flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", 3128), materializer);
        System.out.println("Server online at http://localhost:3128/");
    }

    public Route createRoute(ActorRef directContentRequestActor) {

        final Route extractRequest = extractRequest(request -> {

            CompletionStage<HttpResponse>  futureHttpResponse = PatternsCS.ask(directContentRequestActor,request.getUri().toString(),timeout).
                    thenApply(obj -> (HttpResponse) obj);
            return onSuccess(() -> futureHttpResponse, httpResponse -> complete(httpResponse.entity()));
        }
        );

        return get(() -> route(
                pathSingleSlash(() ->
                        extractRequest
                )
        ));
    }




}