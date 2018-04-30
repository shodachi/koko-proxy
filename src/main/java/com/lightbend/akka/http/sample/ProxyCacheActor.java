package com.lightbend.akka.http.sample;


/*
    This actor will save the response somewhere on disk.
 */

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;

public class ProxyCacheActor extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    static Props props() {
        return Props.create(ProxyCacheActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ProxyMessages.WriteCache.class, w -> saveResponseToCache(w.getHttpRequest(),w.getHttpResponse()))
                .build();
    }


    private void saveResponseToCache (HttpRequest httpRequest, HttpResponse httpResponse){
        log.info("Write cache for "+httpRequest.getUri());


    }
}
