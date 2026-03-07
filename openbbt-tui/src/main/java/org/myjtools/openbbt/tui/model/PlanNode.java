package org.myjtools.openbbt.tui.model;

import java.util.ArrayList;
import java.util.List;

public class PlanNode {

    public enum Type { PROJECT, FEATURE, SCENARIO, STEP_GROUP, STEP }

    public enum Status {
        // Validation states (plan view)
        NOT_VALIDATED,
        VALIDATED,
        INVALID,
        HAS_ISSUES,
        // Execution states (execution view)
        PENDING,
        RUNNING,
        PASS,
        FAIL,
        SKIPPED,
        UNDEFINED
    }

    private final String id;
    private final String label;
    private final Type type;
    private volatile Status status;
    private volatile String validationMessage;
    private final List<PlanNode> children;
    private boolean expanded;

    public PlanNode(String id, String label, Type type) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.status = Status.NOT_VALIDATED;
        this.children = new ArrayList<>();
        this.expanded = true;
    }

    public void addChild(PlanNode child) {
        children.add(child);
    }

    public String getId()                          { return id; }
    public String getLabel()                       { return label; }
    public Type getType()                          { return type; }
    public Status getStatus()                      { return status; }
    public void setStatus(Status s)                { this.status = s; }
    public String getValidationMessage()           { return validationMessage; }
    public void setValidationMessage(String msg)   { this.validationMessage = msg; }
    public List<PlanNode> getChildren()            { return children; }
    public boolean isExpanded()                    { return expanded; }
    public boolean hasChildren()                   { return !children.isEmpty(); }

    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }

    public void resetStatus() {
        this.status = Status.PENDING;
        this.validationMessage = null;
        for (var child : children) child.resetStatus();
    }

    /** Collapse all nodes below {@code maxVisibleDepth} levels from root. */
    public static void collapseBelow(PlanNode root, int maxVisibleDepth) {
        applyDepth(root, 0, maxVisibleDepth);
    }

    private static void applyDepth(PlanNode node, int depth, int maxDepth) {
        node.expanded = depth < maxDepth;
        for (var child : node.getChildren()) applyDepth(child, depth + 1, maxDepth);
    }
}