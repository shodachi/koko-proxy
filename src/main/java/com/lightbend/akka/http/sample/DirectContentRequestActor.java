package com.lightbend.akka.http.sample;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import scala.concurrent.ExecutionContextExecutor;

import java.util.concurrent.CompletionStage;
import static akka.pattern.PatternsCS.pipe;

public class DirectContentRequestActor extends AbstractActor{

    final Http http = Http.get(context().system());
    final ExecutionContextExecutor dispatcher = context().dispatcher();
    static Props props() {
        return Props.create(DirectContentRequestActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, url -> pipe(fetch(url), dispatcher).to(getSender()))
                .build();
    }

    CompletionStage<HttpResponse> fetch(String url) {
        return http.singleRequest(HttpRequest.create(url));
    }
}
