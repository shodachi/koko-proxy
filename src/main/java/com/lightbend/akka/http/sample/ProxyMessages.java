package com.lightbend.akka.http.sample;

import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;

import java.io.Serializable;

public interface ProxyMessages {
    class WriteCache implements Serializable {

        private final HttpRequest httpRequest;
        private final HttpResponse httpResponse;


        public WriteCache(HttpRequest httpRequest,HttpResponse httpResponse) {
            this.httpResponse = httpResponse;
            this.httpRequest = httpRequest;

        }

        public HttpResponse getHttpResponse() {
            return httpResponse;
        }

        public HttpRequest getHttpRequest() {
            return httpRequest;
        }
    }
}
