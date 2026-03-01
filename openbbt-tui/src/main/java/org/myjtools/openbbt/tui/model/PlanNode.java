package org.myjtools.openbbt.tui.model;

import java.util.ArrayList;
import java.util.List;

public class PlanNode {

    public enum Type { PROJECT, FEATURE, SCENARIO, STEP }

    public enum Status { PENDING, RUNNING, PASS, FAIL }

    private final String id;
    private final String label;
    private final Type type;
    private volatile Status status;
    private final List<PlanNode> children;
    private boolean expanded;

    public PlanNode(String id, String label, Type type) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.status = Status.PENDING;
        this.children = new ArrayList<>();
        this.expanded = true;
    }

    public void addChild(PlanNode child) {
        children.add(child);
    }

    public String getId()              { return id; }
    public String getLabel()           { return label; }
    public Type getType()              { return type; }
    public Status getStatus()          { return status; }
    public void setStatus(Status s)    { this.status = s; }
    public List<PlanNode> getChildren(){ return children; }
    public boolean isExpanded()        { return expanded; }
    public boolean hasChildren()       { return !children.isEmpty(); }

    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }

    public void resetStatus() {
        this.status = Status.PENDING;
        for (var child : children) child.resetStatus();
    }
}