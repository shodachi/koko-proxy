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
import akka.http.javadsl.model.Uri;
import com.sun.jmx.snmp.Timestamp;


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
            this.lastTimeCached = new Timestamp(System.nanoTime());
        }
    }

    private final ConcurrentHashMap pageCaches = new ConcurrentHashMap();

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

        Timestamp timestamp = new Timestamp();
        PageCache pageCache = new PageCache(httpRequest.getUri(),httpResponse,timestamp);
        pageCaches.put(httpRequest.getUri(),pageCache);


        log.info(httpRequest.getUri() + " Saved to cache!!!");
        log.info("We have "+pageCaches.size()+ "cached pages");
        /*final int maximumFrameLength = 30000;
        final Function<ByteString, ByteString> transformEachLine = line -> line  some transformation here */;
        //final ActorMaterializer materializer = ActorMaterializer.create(getContext().getSystem());


        //Will fix this for different path later
        /*
        String cacheFileName = "/tmp/proxy-cache/"+httpRequest.getUri().getHost().toString();
        log.info("Write cache for "+httpRequest.getUri() + " at ("+cacheFileName+")");

        httpResponse.entity().getDataBytes()
                .via(Framing.delimiter(ByteString.fromString("\n"), maximumFrameLength, FramingTruncation.ALLOW))
                .map(transformEachLine::apply)
                .runWith(FileIO.toPath(new File(cacheFileName).toPath()), materializer);*/
    }
}
