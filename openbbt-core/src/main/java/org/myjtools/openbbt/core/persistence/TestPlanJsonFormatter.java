package org.myjtools.openbbt.core.persistence;

import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.testplan.TestPlanNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Formats a {@link TestPlan} as a flat JSON representation.
 * All nodes are listed in a flat array (not nested), each with their fields.
 */
public class TestPlanJsonFormatter {

    public interface Appender {
        void append(String string) throws IOException;
    }

    private final TestPlanRepository repository;

    public TestPlanJsonFormatter(TestPlanRepository repository) {
        this.repository = repository;
    }

    public void format(TestPlan testPlan, Appender appender) throws IOException {
        List<UUID> allNodes = Stream.concat(
            Stream.of(testPlan.planNodeRoot()),
            repository.getNodeDescendants(testPlan.planNodeRoot())
        ).toList();

        appender.append("{\n");
        appender.append("  \"planID\": " + quoted(testPlan.planID()) + ",\n");
        appender.append("  \"projectID\": " + quoted(testPlan.projectID()) + ",\n");
        appender.append("  \"createdAt\": " + quoted(testPlan.createdAt()) + ",\n");
        appender.append("  \"nodes\": [\n");

        for (int i = 0; i < allNodes.size(); i++) {
            TestPlanNode node = repository.getNodeData(allNodes.get(i)).orElseThrow();
            appender.append("    {\n");
            appendField(appender, "nodeID", node.nodeID(), false);
            appendField(appender, "nodeType", node.nodeType(), false);
            appendField(appender, "name", node.name(), false);
            appendField(appender, "identifier", node.identifier(), false);
            appendField(appender, "source", node.source(), false);
            appendField(appender, "keyword", node.keyword(), false);
            appendField(appender, "language", node.language(), false);
            appendField(appender, "validationStatus", node.validationStatus(), false);
            appendField(appender, "validationMessage", node.validationMessage(), false);
            appendField(appender, "hasIssues", node.hasIssues(), false);
            appendTags(appender, node);
            appendProperties(appender, node);
            appender.append("    }");
            if (i < allNodes.size() - 1) appender.append(",");
            appender.append("\n");
        }

        appender.append("  ]\n");
        appender.append("}\n");
    }

    private void appendField(Appender appender, String key, Object value, boolean last) throws IOException {
        appender.append("      " + quoted(key) + ": ");
        if (value == null) {
            appender.append("null");
        } else if (value instanceof Boolean || value instanceof Number) {
            appender.append(String.valueOf(value));
        } else {
            appender.append(quoted(value));
        }
        appender.append(",\n");
    }

    private void appendTags(Appender appender, TestPlanNode node) throws IOException {
        appender.append("      \"tags\": [");
        List<String> tags = node.tags() != null ? node.tags().stream().sorted().toList() : List.of();
        for (int i = 0; i < tags.size(); i++) {
            appender.append(quoted(tags.get(i)));
            if (i < tags.size() - 1) appender.append(", ");
        }
        appender.append("],\n");
    }

    private void appendProperties(Appender appender, TestPlanNode node) throws IOException {
        appender.append("      \"properties\": {");
        Map<String, String> props = node.properties() != null ? node.properties() : Map.of();
        List<Map.Entry<String, String>> entries = props.entrySet().stream().toList();
        for (int i = 0; i < entries.size(); i++) {
            appender.append(quoted(entries.get(i).getKey()) + ": " + quoted(entries.get(i).getValue()));
            if (i < entries.size() - 1) appender.append(", ");
        }
        appender.append("}\n");
    }

    private String quoted(Object value) {
        if (value == null) return "null";
        return "\"" + value.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}