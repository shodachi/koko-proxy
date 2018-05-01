package com.shodachi.kokoproxy;

import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Uri;

import java.io.Serializable;

public interface ProxyMessages {
    class WriteCache implements Serializable {

        private final HttpRequest httpRequest;
        private final HttpResponse httpResponse;


        public WriteCache(HttpRequest httpRequest, HttpResponse httpResponse) {
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

    class ReadCache implements Serializable {
        private final Uri uri;

        public ReadCache(Uri uri) {
            this.uri = uri;
        }

        public Uri getUri() {
            return uri;
        }
    }
}
