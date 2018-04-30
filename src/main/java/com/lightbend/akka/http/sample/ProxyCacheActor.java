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
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Framing;
import akka.stream.javadsl.FramingTruncation;
import akka.util.ByteString;

import java.io.File;
import java.util.function.Function;

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
        final int maximumFrameLength = 30000;
        final Function<ByteString, ByteString> transformEachLine = line -> line /* some transformation here */;
        final ActorMaterializer materializer = ActorMaterializer.create(getContext().getSystem());


        //Will fix this for different path later
        String cacheFileName = "/tmp/"+httpRequest.getUri().getHost().toString();
        log.info("Write cache for "+httpRequest.getUri() + " at ("+cacheFileName+")");

        httpResponse.entity().getDataBytes()
                .via(Framing.delimiter(ByteString.fromString("\n"), maximumFrameLength, FramingTruncation.ALLOW))
                .map(transformEachLine::apply)
                .runWith(FileIO.toPath(new File(cacheFileName).toPath()), materializer);
    }
}
