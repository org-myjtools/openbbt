package org.myjtools.openbbt.cli.serve;

import com.google.gson.*;
import org.myjtools.openbbt.core.execution.TestExecution;
import org.myjtools.openbbt.core.execution.TestExecutionNode;
import org.myjtools.openbbt.core.persistence.AttachmentRepository;
import org.myjtools.openbbt.core.persistence.TestExecutionRepository;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.testplan.TestPlanNode;
import org.myjtools.openbbt.core.util.Log;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * JSON-RPC 2.0 server over stdio using Content-Length framing (same as LSP).
 * Reads requests from an InputStream and writes responses to an OutputStream.
 */
public class JsonRpcServer {

    private static final Log log = Log.of();

    public interface RepositoryFactory {
        TestPlanRepository open();
        default TestExecutionRepository openExecution() { return null; }
        default AttachmentRepository openAttachment() { return null; }
    }

    @FunctionalInterface
    public interface ExecHandler {
        /**
         * Execute the current plan synchronously.
         * {@code onExecutionCreated} is called with (executionID, planID) as soon as the
         * execution record exists, before any test steps run.
         */
        TestExecution exec(BiConsumer<UUID, UUID> onExecutionCreated);
    }

    private final InputStream in;
    private final OutputStream out;
    private final RepositoryFactory factory;
    private final ExecHandler execHandler;
    private TestPlanRepository repository;
    private TestExecutionRepository executionRepository;
    private AttachmentRepository attachmentRepository;
    private volatile boolean running = true;

    public JsonRpcServer(InputStream in, OutputStream out, RepositoryFactory factory) {
        this(in, out, factory, null);
    }

    public JsonRpcServer(InputStream in, OutputStream out, RepositoryFactory factory, ExecHandler execHandler) {
        this.in = in;
        this.out = out;
        this.factory = factory;
        this.execHandler = execHandler;
    }

    public void run() {
        repository = factory.open();
        executionRepository = factory.openExecution();
        attachmentRepository = factory.openAttachment();
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
                case "plans/list"      -> handleListPlans(params);
                case "plans/get"       -> handleGetPlan(params);
                case "plans/delete"              -> { handleDeletePlan(params); yield JsonNull.INSTANCE; }
                case "plans/deleteUnexecuted"    -> { handleDeleteUnexecutedPlans(); yield JsonNull.INSTANCE; }
                case "executions/list"   -> handleListExecutions(params);
                case "executions/node"        -> handleExecutionNode(params);
                case "executions/attachments" -> handleListAttachments(params);
                case "executions/attachment"  -> handleGetAttachment(params);
                case "executions/delete" -> { handleDeleteExecution(params); yield JsonNull.INSTANCE; }
                case "exec"                   -> handleExec(params);
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

    private JsonArray handleListPlans(JsonObject params) {
        String organization = params.get("organization").getAsString();
        String project = params.get("project").getAsString();
        int offset = params.has("offset") ? params.get("offset").getAsInt() : 0;
        int max = params.has("max") ? params.get("max").getAsInt() : 0;
        JsonArray arr = new JsonArray();
        for (TestPlan plan : repository.listPlans(organization, project, offset, max)) {
            JsonObject obj = new JsonObject();
            obj.addProperty("planId", plan.planID().toString());
            obj.addProperty("createdAt", plan.createdAt().toString());
            boolean hasIssues = repository.getNodeData(plan.planNodeRoot())
                .map(n -> n.hasIssues()).orElse(false);
            obj.addProperty("hasIssues", hasIssues);
            arr.add(obj);
        }
        return arr;
    }

    private JsonObject handleGetPlan(JsonObject params) {
        UUID planId = UUID.fromString(params.get("planId").getAsString());
        TestPlan plan = repository.getPlan(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));
        JsonObject obj = new JsonObject();
        obj.addProperty("planId",      plan.planID().toString());
        obj.addProperty("createdAt",   plan.createdAt().toString());
        obj.addProperty("planNodeRoot", plan.planNodeRoot().toString());
        repository.getProject(plan.projectID()).ifPresent(p -> {
            obj.addProperty("organization", p.organization());
            obj.addProperty("project",      p.name());
            if (p.description() != null) obj.addProperty("description", p.description());
        });
        return obj;
    }

    private JsonArray handleListExecutions(JsonObject params) {
        if (executionRepository == null)
            throw new IllegalStateException("Execution repository not available");
        UUID planId = UUID.fromString(params.get("planId").getAsString());
        int offset = params.has("offset") ? params.get("offset").getAsInt() : 0;
        int max = params.has("max") ? params.get("max").getAsInt() : 0;
        UUID planNodeRoot = repository.getPlan(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId))
            .planNodeRoot();
        JsonArray arr = new JsonArray();
        for (TestExecution ex : executionRepository.listExecutions(planId, planNodeRoot, offset, max)) {
            JsonObject obj = new JsonObject();
            obj.addProperty("executionId", ex.executionID().toString());
            obj.addProperty("planId", ex.planID().toString());
            obj.addProperty("planNodeRoot", planNodeRoot.toString());
            obj.add("executionRootNodeId", ex.executionRootNodeID() != null
                ? new JsonPrimitive(ex.executionRootNodeID().toString()) : JsonNull.INSTANCE);
            obj.addProperty("executedAt", ex.executedAt().toString());
            executionRepository.getExecutionNodeResult(ex.executionRootNodeID() != null
                    ? ex.executionRootNodeID() : UUID.randomUUID())
                .ifPresent(r -> obj.addProperty("result", r.name()));
            arr.add(obj);
        }
        return arr;
    }

    private JsonObject handleExecutionNode(JsonObject params) {
        if (executionRepository == null)
            throw new IllegalStateException("Execution repository not available");
        UUID executionId = UUID.fromString(params.get("executionId").getAsString());
        UUID planNodeId  = UUID.fromString(params.get("planNodeId").getAsString());
        TestExecutionNode node = executionRepository.getExecutionNode(executionId, planNodeId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Execution node not found for executionId=" + executionId + " planNodeId=" + planNodeId));
        JsonObject obj = new JsonObject();
        obj.addProperty("executionNodeId", node.executionNodeID().toString());
        obj.addProperty("executionId",     node.executionID().toString());
        obj.addProperty("planNodeId",      node.planNodeID().toString());
        obj.add("result",     node.result()    != null ? new JsonPrimitive(node.result().name())         : JsonNull.INSTANCE);
        obj.add("startedAt",  node.startTime() != null ? new JsonPrimitive(node.startTime().toString())  : JsonNull.INSTANCE);
        obj.add("finishedAt", node.endTime()   != null ? new JsonPrimitive(node.endTime().toString())    : JsonNull.INSTANCE);
        obj.add("durationMs", node.startTime() != null && node.endTime() != null ? new JsonPrimitive(node.duration()) : JsonNull.INSTANCE);
        obj.add("message",    node.message()   != null ? new JsonPrimitive(node.message())               : JsonNull.INSTANCE);
        int attachmentCount = executionRepository.listAttachmentIds(node.executionNodeID()).size();
        obj.addProperty("attachmentCount", attachmentCount);
        return obj;
    }

    private JsonArray handleListAttachments(JsonObject params) {
        if (executionRepository == null || attachmentRepository == null)
            throw new IllegalStateException("Execution or attachment repository not available");
        UUID executionId = UUID.fromString(params.get("executionId").getAsString());
        UUID planNodeId  = UUID.fromString(params.get("planNodeId").getAsString());
        UUID executionNodeId = executionRepository.getExecutionNodeByPlanNode(executionId, planNodeId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Execution node not found for executionId=" + executionId + " planNodeId=" + planNodeId));
        List<UUID> ids = executionRepository.listAttachmentIds(executionNodeId);
        JsonArray arr = new JsonArray();
        for (UUID attachmentId : ids) {
            attachmentRepository.retrieveAttachment(executionId, executionNodeId, attachmentId).ifPresent(a -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("attachmentId",    a.attachmentID().toString());
                obj.addProperty("executionId",     executionId.toString());
                obj.addProperty("executionNodeId", executionNodeId.toString());
                obj.addProperty("contentType",     a.contentType());
                arr.add(obj);
            });
        }
        return arr;
    }

    private JsonObject handleGetAttachment(JsonObject params) {
        if (executionRepository == null || attachmentRepository == null)
            throw new IllegalStateException("Execution or attachment repository not available");
        UUID executionId     = UUID.fromString(params.get("executionId").getAsString());
        UUID executionNodeId = UUID.fromString(params.get("executionNodeId").getAsString());
        UUID attachmentId    = UUID.fromString(params.get("attachmentId").getAsString());
        AttachmentRepository.Attachment a = attachmentRepository
            .retrieveAttachment(executionId, executionNodeId, attachmentId)
            .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));
        JsonObject obj = new JsonObject();
        obj.addProperty("attachmentId",    a.attachmentID().toString());
        obj.addProperty("contentType",     a.contentType());
        obj.addProperty("data",            Base64.getEncoder().encodeToString(a.bytes()));
        return obj;
    }

    private JsonObject handleExec(JsonObject params) {
        if (execHandler == null) {
            throw new IllegalStateException("exec handler not configured");
        }
        boolean detach = params.has("detach") && params.get("detach").getAsBoolean();

        if (!detach) {
            TestExecution ex = execHandler.exec(null);
            JsonObject result = new JsonObject();
            result.addProperty("executionId", ex.executionID().toString());
            result.addProperty("planId", ex.planID().toString());
            if (ex.executionRootNodeID() != null && executionRepository != null) {
                executionRepository.getExecutionNodeResult(ex.executionRootNodeID())
                    .ifPresent(r -> result.addProperty("result", r.name()));
            }
            return result;
        }

        // Detach mode: return executionId and planId as soon as the record is created
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<UUID> idRef = new AtomicReference<>();
        AtomicReference<UUID> planIdRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            try {
                execHandler.exec((id, planId) -> {
                    idRef.set(id);
                    planIdRef.set(planId);
                    latch.countDown();
                });
            } catch (Throwable t) {
                errorRef.set(t);
                latch.countDown();
            }
        }, "openbbt-exec");
        thread.setDaemon(false);
        thread.start();

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted waiting for execution to start");
        }
        if (errorRef.get() != null) {
            throw new RuntimeException("Execution failed to start", errorRef.get());
        }

        JsonObject result = new JsonObject();
        result.addProperty("executionId", idRef.get().toString());
        result.addProperty("planId", planIdRef.get().toString());
        return result;
    }

    private void handleDeleteExecution(JsonObject params) {
        if (executionRepository == null)
            throw new IllegalStateException("Execution repository not available");
        UUID executionId = UUID.fromString(params.get("executionId").getAsString());
        if (attachmentRepository != null) {
            attachmentRepository.deleteAttachments(executionId);
        }
        executionRepository.deleteExecution(executionId);
    }

    private void handleDeletePlan(JsonObject params) {
        UUID planId = UUID.fromString(params.get("planId").getAsString());
        // Delete file-system attachments before the DB cascades remove the execution records
        if (executionRepository != null && attachmentRepository != null) {
            UUID planNodeRoot = repository.getPlan(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId))
                .planNodeRoot();
            executionRepository.listExecutions(planId, planNodeRoot, 0, 0)
                .forEach(ex -> attachmentRepository.deleteAttachments(ex.executionID()));
        }
        // Deleting the plan cascades: executions, execution_nodes, attachment records, plan_nodes
        repository.deletePlan(planId);
    }

    private void handleDeleteUnexecutedPlans() {
        for (TestPlan plan : repository.listPlans()) {
            boolean hasExecutions = executionRepository != null &&
                !executionRepository.listExecutions(plan.planID(), plan.planNodeRoot(), 0, 1).isEmpty();
            if (!hasExecutions) {
                if (attachmentRepository != null) {
                    // no executions means no attachments, but call for safety
                    attachmentRepository.deleteAttachments(plan.planID());
                }
                repository.deletePlan(plan.planID());
            }
        }
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
        obj.addProperty("display", node.toString());
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

        if (node.document() != null) {
            JsonObject doc = new JsonObject();
            doc.addProperty("mimeType", node.document().mimeType());
            doc.addProperty("content",  node.document().content());
            obj.add("document", doc);
        } else {
            obj.add("document", JsonNull.INSTANCE);
        }

        if (node.dataTable() != null) {
            JsonArray rows = new JsonArray();
            for (var row : node.dataTable().values()) {
                JsonArray rowArr = new JsonArray();
                row.forEach(rowArr::add);
                rows.add(rowArr);
            }
            obj.add("dataTable", rows);
        } else {
            obj.add("dataTable", JsonNull.INSTANCE);
        }

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