package com.shodachi.kokoproxy;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.*;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.pattern.PatternsCS;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.ByteString;
import akka.util.Timeout;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class KokoProxyServer extends AllDirectives {

    private Timeout timeout = new Timeout(Duration.create(30, TimeUnit.SECONDS)); // usually we'd obtain the timeout from the system's configuration

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

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute(directContentRequestActor, requestCacheActor, materializer, system).flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", 3128), materializer);
        System.out.println("Server online at http://localhost:3128/");
    }

    private Route createRoute(ActorRef directContentRequestActor, ActorRef requestCacheActor, ActorMaterializer materializer, ActorSystem system) {

        LoggingAdapter log = Logging.getLogger(system, this);

        final Route extractRequest = extractRequest(request -> {
                    log.info("Trying to access " + request.getUri());

                    CompletionStage<Optional<HttpResponse>> futureCacheHttpResponse = PatternsCS.ask(requestCacheActor, new ProxyMessages.ReadCache(request.getUri()), timeout).
                            thenApply(
                                    obj -> (Optional<HttpResponse>) obj
                            );

                    return onSuccess(() -> futureCacheHttpResponse, httpResponse ->
                            {
                                if (httpResponse.isPresent()) {
                                    log.info("Got cache for you!!");
                                    HttpResponse responseCached = httpResponse.get();

                                    final HttpResponse res = HttpResponse.create().
                                            withEntity(responseCached.entity()).
                                            withStatus(responseCached.status()).
                                            withHeaders(responseCached.getHeaders()).
                                            addHeader(HttpHeader.parse("X-Cache", "HIT"));

                                    return complete(res);
                                } else {
                                    return routeWhenCacheMiss(request, directContentRequestActor, requestCacheActor, materializer, system);
                                }
                            }
                    );
                }
        );

        return get(() -> route(
                extractRequest
        ));
    }

    private Route routeWhenCacheMiss(HttpRequest request, ActorRef directContentRequestActor, ActorRef requestCacheActor, ActorMaterializer materializer, ActorSystem system) {
        CompletionStage<HttpResponse> futureHttpResponse = PatternsCS.ask(directContentRequestActor, request.getUri().toString(), timeout).
                thenApply(obj -> (HttpResponse) obj);
        return onSuccess(() -> futureHttpResponse, httpResponse ->
                {
                    final CompletionStage<HttpEntity.Strict> strictEntity = httpResponse.entity()
                            .toStrict(FiniteDuration.create(3, TimeUnit.SECONDS).toMillis(), materializer);

                    try {
                        ByteString content = strictEntity.toCompletableFuture().get().getData();
                        ContentType contentType = strictEntity.toCompletableFuture().get().getContentType();

                        final HttpResponse resposeToCache = HttpResponse.create().
                                withEntity(contentType, content).
                                withStatus(httpResponse.status()).
                                withHeaders(httpResponse.getHeaders()).
                                removeHeader("X-Cache");

                        createCache(requestCacheActor, request, resposeToCache);

                        final HttpResponse newResponse = HttpResponse.create().
                                withEntity(contentType, content).
                                withStatus(httpResponse.status()).
                                withHeaders(httpResponse.getHeaders()).
                                removeHeader("X-Cache").
                                addHeader(HttpHeader.parse("X-Cache", "MISS"));

                        return complete(newResponse);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                    return complete(StatusCodes.INTERNAL_SERVER_ERROR);
                }
        );
    }

    private void createCache(ActorRef requestCacheActor, HttpRequest request, HttpResponse response) {
        PatternsCS.ask(requestCacheActor, new ProxyMessages.WriteCache(request, response), timeout);
    }
}