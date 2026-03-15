package org.myjtools.openbbt.plugins.rest.jdk;

import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.plugins.rest.RestEngine;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class JdkHttpEngine implements RestEngine {

    private String baseUrl;
    private Integer httpCodeThreshold;
    private Duration timeout = Duration.ofSeconds(10);
    private HttpResponse<String> lastResponse;

    private final HttpClient client = HttpClient.newHttpClient();

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
        this.timeout = Duration.ofMillis(milliseconds);
    }

    @Override
    public void requestGET(String endpoint) {
        lastResponse = send(builder(endpoint).GET().build());
        checkThreshold();
    }

    @Override
    public void requestPOST(String endpoint) {
        lastResponse = send(builder(endpoint).POST(HttpRequest.BodyPublishers.noBody()).build());
        checkThreshold();
    }

    @Override
    public void requestPOST(String endpoint, String content) {
        lastResponse = send(builder(endpoint).POST(HttpRequest.BodyPublishers.ofString(content)).build());
        checkThreshold();
    }

    @Override
    public void requestPUT(String endpoint, String content) {
        lastResponse = send(builder(endpoint).PUT(HttpRequest.BodyPublishers.ofString(content)).build());
        checkThreshold();
    }

    @Override
    public void requestPATCH(String endpoint, String content) {
        lastResponse = send(builder(endpoint).method("PATCH", HttpRequest.BodyPublishers.ofString(content)).build());
        checkThreshold();
    }

    @Override
    public void requestDELETE(String endpoint) {
        lastResponse = send(builder(endpoint).DELETE().build());
        checkThreshold();
    }

    @Override
    public Integer responseHttpCode() {
        return lastResponse != null ? lastResponse.statusCode() : null;
    }

    @Override
    public String responseBody() {
        return lastResponse != null ? lastResponse.body() : null;
    }

    @Override
    public String responseContentType() {
        if (lastResponse == null) return null;
        return lastResponse.headers().firstValue("Content-Type")
            .map(this::translateContentType)
            .orElse(null);
    }

    private HttpRequest.Builder builder(String endpoint) {
        String url = (baseUrl == null || baseUrl.isBlank())
            ? endpoint
            : (baseUrl.endsWith("/") || endpoint.startsWith("/"))
                ? baseUrl + endpoint
                : baseUrl + "/" + endpoint;
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(timeout);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenBBTException("HTTP request interrupted: {}",request.uri());
        } catch (IOException e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new OpenBBTException("HTTP request failed [{}]: {}", request.uri(), reason);
        }
    }

    private void checkThreshold() {
        if (httpCodeThreshold != null && lastResponse.statusCode() >= httpCodeThreshold) {
            throw new AssertionError(
                "HTTP response status " + lastResponse.statusCode() +
                " exceeds threshold " + httpCodeThreshold
            );
        }
    }

    private String translateContentType(String contentType) {
        if (contentType == null) return null;
        // Strip parameters like "; charset=UTF-8"
        String base = contentType.split(";")[0].trim();
        if (base.contains("application/json")) return "json";
        if (base.contains("application/yaml") ||
            base.contains("text/yaml") ||
            base.contains("text/x-yaml")) return "yaml";
        if (base.contains("application/xml") || base.contains("text/xml")) return "xml";
        if (base.contains("text/html")) return "html";
        return base;
    }
}
