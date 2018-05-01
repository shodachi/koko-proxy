package com.lightbend.akka.http.sample;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Uri;


import java.sql.Timestamp;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyCacheActor extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);


    public static class PageCache {
        private Uri uri;
        private HttpResponse httpResponse;
        private Timestamp lastTimeCached;

        public PageCache(Uri uri, HttpResponse httpResponse, Timestamp lastTimeCached) {
            this.uri = uri;
            this.httpResponse = httpResponse;
            this.lastTimeCached = new Timestamp(System.currentTimeMillis());
        }
    }

    private final ConcurrentHashMap<Uri, PageCache> pageCaches = new ConcurrentHashMap();

    static Props props() {
        return Props.create(ProxyCacheActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ProxyMessages.WriteCache.class, w -> saveResponseToCache(w.getHttpRequest(), w.getHttpResponse()))
                .match(ProxyMessages.ReadCache.class, this::apply)
                .build();
    }

    private void apply(ProxyMessages.ReadCache r) {
        getSender().tell(readResponseFromCache(r.getUri()), getSelf());
    }

    private void saveResponseToCache(HttpRequest httpRequest, HttpResponse httpResponse) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        PageCache pageCache = new PageCache(httpRequest.getUri(), httpResponse, timestamp);
        pageCaches.put(httpRequest.getUri(), pageCache);

        log.info(httpRequest.getUri() + " Saved to cache!!!");
        log.info("We have " + pageCaches.size() + "cached pages");
    }

    private Optional<HttpResponse> readResponseFromCache(Uri uri) {
        log.info(" Reading from cache for " + uri);
        PageCache pageCache = pageCaches.get(uri);
        if ((pageCache != null) && !hasCacheExpired(pageCache)) {
            log.info(" Cache hit!! for " + uri);
            return Optional.of(pageCache.httpResponse);
        }
        log.info(" Cache miss!! for " + uri);
        return Optional.empty();
    }

    private boolean hasCacheExpired(PageCache pageCache) {
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        Long delta = currentTime.getTime() - pageCache.lastTimeCached.getTime();

        if (delta > Duration.ofSeconds(20).toMillis()) {
            log.info("Cache expired!!");
            return true;
        } else {
            return false;
        }

    }
}
