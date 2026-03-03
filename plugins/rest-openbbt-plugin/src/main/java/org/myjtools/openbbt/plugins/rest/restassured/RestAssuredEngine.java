package org.myjtools.openbbt.plugins.rest.restassured;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.myjtools.openbbt.plugins.rest.RestEngine;

public class RestAssuredEngine implements RestEngine {

    private String baseUrl;
    private Integer httpCodeThreshold;
    private Long timeoutMs;
    private Response lastResponse;

    @Override
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public void setHttpCodeThreshold(Integer httpCode) {
        this.httpCodeThreshold = httpCode;
    }

    @Override
    public void setTimeout(Long milliseconds) {
        this.timeoutMs = milliseconds;
    }

    @Override
    public void requestGET(String endpoint) {
        lastResponse = spec().get(endpoint);
        checkThreshold();
    }

    @Override
    public void requestPOST(String endpoint) {
        lastResponse = spec().post(endpoint);
        checkThreshold();
    }

    @Override
    public void requestPOST(String endpoint, String content) {
        lastResponse = spec().body(content).post(endpoint);
        checkThreshold();
    }

    @Override
    public void requestPUT(String endpoint, String content) {
        lastResponse = spec().body(content).put(endpoint);
        checkThreshold();
    }

    @Override
    public void requestPATCH(String endpoint, String content) {
        lastResponse = spec().body(content).patch(endpoint);
        checkThreshold();
    }

    @Override
    public void requestDELETE(String endpoint) {
        lastResponse = spec().delete(endpoint);
        checkThreshold();
    }

    @Override
    public Integer responseHttpCode() {
        return lastResponse != null ? lastResponse.statusCode() : null;
    }

    @Override
    public String responseBody() {
        return lastResponse != null ? lastResponse.body().asString() : null;
    }

    private RequestSpecification spec() {
        RequestSpecification spec = RestAssured.given().baseUri(baseUrl);
        if (timeoutMs != null) {
            spec = spec.config(RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", timeoutMs.intValue())
                    .setParam("http.socket.timeout", timeoutMs.intValue())));
        }
        return spec;
    }

    private void checkThreshold() {
        if (httpCodeThreshold != null && lastResponse.statusCode() >= httpCodeThreshold) {
            throw new AssertionError(
                "HTTP response status " + lastResponse.statusCode() +
                " exceeds threshold " + httpCodeThreshold
            );
        }
    }

}