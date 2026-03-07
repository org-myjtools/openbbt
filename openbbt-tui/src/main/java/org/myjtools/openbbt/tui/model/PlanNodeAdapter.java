package org.myjtools.openbbt.tui.model;

import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.NodeType;
import org.myjtools.openbbt.core.testplan.TestPlanNode;
import org.myjtools.openbbt.core.testplan.ValidationStatus;

import java.util.UUID;

public class PlanNodeAdapter {

    /** Build a plan-view PlanNode tree from the repository, using validation status. */
    public static PlanNode adaptForPlanView(UUID rootId, TestPlanRepository repo) {
        return buildNode(rootId, repo);
    }

    /** Deep-copy a PlanNode tree, resetting all statuses to PENDING for execution. */
    public static PlanNode copyForExecution(PlanNode source) {
        var copy = new PlanNode(source.getId(), source.getLabel(), source.getType());
        copy.setStatus(PlanNode.Status.PENDING);
        for (var child : source.getChildren()) {
            copy.addChild(copyForExecution(child));
        }
        return copy;
    }

    private static PlanNode buildNode(UUID id, TestPlanRepository repo) {
        TestPlanNode coreNode = repo.getNodeData(id).orElseThrow();
        String label = coreNode.name() != null ? coreNode.name() : coreNode.nodeType().name();
        PlanNode.Type type = mapType(coreNode.nodeType());
        var node = new PlanNode(id.toString(), label, type);
        node.setStatus(mapValidationStatus(coreNode));
        node.setValidationMessage(coreNode.validationMessage());
        repo.getNodeChildren(id).forEach(childId -> node.addChild(buildNode(childId, repo)));
        return node;
    }

    private static PlanNode.Type mapType(NodeType nodeType) {
        return switch (nodeType) {
            case TEST_PLAN                 -> PlanNode.Type.PROJECT;
            case TEST_SUITE, TEST_FEATURE  -> PlanNode.Type.FEATURE;
            case TEST_CASE                 -> PlanNode.Type.SCENARIO;
            case STEP_AGGREGATOR           -> PlanNode.Type.STEP_GROUP;
            default                        -> PlanNode.Type.STEP;
        };
    }

    private static PlanNode.Status mapValidationStatus(TestPlanNode node) {
        ValidationStatus vs = node.validationStatus();
        if (vs == ValidationStatus.ERROR) return PlanNode.Status.INVALID;
        if (vs == ValidationStatus.OK)    return PlanNode.Status.VALIDATED;
        if (node.hasIssues())             return PlanNode.Status.HAS_ISSUES;
        return PlanNode.Status.NOT_VALIDATED;
    }
}