package org.myjtools.openbbt.cli.serve;

import com.google.gson.*;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.testplan.TestPlanNode;
import org.myjtools.openbbt.core.util.Log;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * JSON-RPC 2.0 server over stdio using Content-Length framing (same as LSP).
 * Reads requests from an InputStream and writes responses to an OutputStream.
 */
public class JsonRpcServer {

    private static final Log log = Log.of();

    public interface RepositoryFactory {
        TestPlanRepository open();
    }

    private final InputStream in;
    private final OutputStream out;
    private final RepositoryFactory factory;
    private TestPlanRepository repository;
    private volatile boolean running = true;

    public JsonRpcServer(InputStream in, OutputStream out, RepositoryFactory factory) {
        this.in = in;
        this.out = out;
        this.factory = factory;
    }

    public void run() {
        repository = factory.open();
        log.info("OpenBBT serve: ready");
        while (running) {
            try {
                String message = readMessage();
                if (message == null) break;
                JsonObject request = JsonParser.parseString(message).getAsJsonObject();
                String response = dispatch(request);
                if (response != null) {
                    writeMessage(response);
                }
            } catch (EOFException | InterruptedIOException e) {
                break;
            } catch (IOException e) {
                break;
            } catch (Exception e) {
                log.error(e, "Error processing JSON-RPC request");
            }
        }
        closeRepository();
        log.info("OpenBBT serve: stopped");
    }

    // --- Protocol framing ---

    private String readMessage() throws IOException {
        int contentLength = -1;
        StringBuilder header = new StringBuilder();
        byte[] buf = new byte[1];
        while (true) {
            int read = in.read(buf);
            if (read == -1) return null;
            header.append((char) buf[0]);
            String h = header.toString();
            if (h.endsWith("\r\n\r\n")) {
                for (String line : h.split("\r\n")) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    }
                }
                break;
            }
        }
        if (contentLength < 0) throw new IOException("Missing Content-Length header");
        byte[] body = in.readNBytes(contentLength);
        return new String(body, StandardCharsets.UTF_8);
    }

    private void writeMessage(String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        byte[] header = ("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        synchronized (out) {
            out.write(header);
            out.write(body);
            out.flush();
        }
    }

    // --- Dispatcher ---

    private String dispatch(JsonObject req) {
        JsonElement idEl = req.get("id");
        String method = req.has("method") ? req.get("method").getAsString() : "";
        JsonObject params = req.has("params") && req.get("params").isJsonObject()
            ? req.getAsJsonObject("params")
            : new JsonObject();
        try {
            JsonElement result = switch (method) {
                case "browse/plans"    -> handlePlans();
                case "browse/node"     -> handleNode(params);
                case "browse/children" -> handleChildren(params);
                case "refresh"         -> { handleRefresh(); yield JsonNull.INSTANCE; }
                case "shutdown"        -> { running = false; yield JsonNull.INSTANCE; }
                default -> throw new IllegalArgumentException("Method not found: " + method);
            };
            return buildSuccess(idEl, result);
        } catch (IllegalArgumentException e) {
            return buildError(idEl, -32601, e.getMessage());
        } catch (Exception e) {
            log.error(e, "Handler error for method {}", method);
            return buildError(idEl, -32000, e.getMessage());
        }
    }

    // --- Handlers ---

    private JsonArray handlePlans() {
        JsonArray arr = new JsonArray();
        for (TestPlan plan : repository.listPlans()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("planId", plan.planID().toString());
            obj.addProperty("projectId", plan.projectID().toString());
            obj.addProperty("createdAt", plan.createdAt().toString());
            obj.addProperty("planNodeRoot", plan.planNodeRoot().toString());
            arr.add(obj);
        }
        return arr;
    }

    private JsonObject handleNode(JsonObject params) {
        UUID nodeId = UUID.fromString(params.get("nodeId").getAsString());
        TestPlanNode node = repository.getNodeData(nodeId)
            .orElseThrow(() -> new IllegalArgumentException("Node not found: " + nodeId));
        return nodeToJson(node);
    }

    private JsonArray handleChildren(JsonObject params) {
        UUID nodeId = UUID.fromString(params.get("nodeId").getAsString());
        JsonArray arr = new JsonArray();
        repository.getNodeChildren(nodeId).forEach(childId -> {
            TestPlanNode child = repository.getNodeData(childId).orElseThrow();
            arr.add(nodeToJson(child));
        });
        return arr;
    }

    private void handleRefresh() {
        closeRepository();
        repository = factory.open();
        log.info("OpenBBT serve: repository refreshed");
    }

    // --- Helpers ---

    private JsonObject nodeToJson(TestPlanNode node) {
        JsonObject obj = new JsonObject();
        obj.addProperty("nodeId", node.nodeID().toString());
        obj.add("nodeType", node.nodeType() != null ? new JsonPrimitive(node.nodeType().name()) : JsonNull.INSTANCE);
        obj.add("name", str(node.name()));
        obj.add("identifier", str(node.identifier()));
        obj.add("source", str(node.source()));
        obj.add("keyword", str(node.keyword()));
        obj.add("language", str(node.language()));
        obj.add("validationStatus", node.validationStatus() != null ? new JsonPrimitive(node.validationStatus().name()) : JsonNull.INSTANCE);
        obj.add("validationMessage", str(node.validationMessage()));
        obj.addProperty("hasIssues", node.hasIssues());

        JsonArray tags = new JsonArray();
        if (node.tags() != null) node.tags().stream().sorted().forEach(tags::add);
        obj.add("tags", tags);

        JsonObject props = new JsonObject();
        if (node.properties() != null) node.properties().forEach((k, v) -> props.addProperty(k, v));
        obj.add("properties", props);

        obj.addProperty("childCount", repository.countNodeChildren(node.nodeID()));
        return obj;
    }

    private static JsonElement str(String value) {
        return value != null ? new JsonPrimitive(value) : JsonNull.INSTANCE;
    }

    private String buildSuccess(JsonElement id, JsonElement result) {
        JsonObject resp = new JsonObject();
        resp.addProperty("jsonrpc", "2.0");
        if (id != null) resp.add("id", id);
        resp.add("result", result);
        return resp.toString();
    }

    private String buildError(JsonElement id, int code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message != null ? message : "Unknown error");
        JsonObject resp = new JsonObject();
        resp.addProperty("jsonrpc", "2.0");
        if (id != null) resp.add("id", id);
        resp.add("error", error);
        return resp.toString();
    }

    private void closeRepository() {
        if (repository instanceof AutoCloseable c) {
            try { c.close(); } catch (Exception ignored) {}
        }
    }
}