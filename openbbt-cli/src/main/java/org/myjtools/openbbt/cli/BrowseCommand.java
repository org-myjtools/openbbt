package org.myjtools.openbbt.cli;

import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.persistence.TestPlanHierarchyFormatter;
import org.myjtools.openbbt.core.persistence.TestPlanJsonFormatter;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.testplan.TestPlanNode;
import org.myjtools.openbbt.core.util.Log;
import picocli.CommandLine;

import java.util.UUID;

@CommandLine.Command(
    name = "browse",
    description = "Browse the content of an existing test plan"
)
public final class BrowseCommand extends AbstractCommand {

    private static final Log log = Log.of();

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    Target target;

    static class Target {
        @CommandLine.Option(
            names = {"--plan"},
            description = "UUID of the test plan to browse from its root"
        )
        String planID;

        @CommandLine.Option(
            names = {"--node"},
            description = "UUID of a node to browse from (use with --detail and --depth to navigate large trees)"
        )
        String nodeID;
    }

    @CommandLine.Option(
        names = {"--detail"},
        description = "Show the node tree as hierarchical JSON",
        defaultValue = "false"
    )
    boolean detail;

    @CommandLine.Option(
        names = {"--json"},
        description = "Output the test plan as flat JSON (only with --plan)",
        defaultValue = "false"
    )
    boolean json;

    @CommandLine.Option(
        names = {"--depth"},
        description = "Maximum depth of the node tree (only with --detail). No limit by default.",
        defaultValue = "-1"
    )
    int depth;

    @Override
    protected void execute() {
        OpenBBTContext context = getContext();
        OpenBBTRuntime runtime = new OpenBBTRuntime(context.configuration());
        TestPlanRepository repository = runtime.getRepository(TestPlanRepository.class);
        try {
            if (target.planID != null) {
                executePlan(repository);
            } else {
                executeNode(repository);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void executePlan(TestPlanRepository repository) throws Exception {
        UUID uuid = UUID.fromString(target.planID);
        TestPlan testPlan = repository.getPlan(uuid)
            .orElseThrow(() -> new IllegalArgumentException("Test plan not found: " + target.planID));
        try {
            if (json) {
                new TestPlanJsonFormatter(repository).format(testPlan, System.out::print);
            } else if (detail) {
                new TestPlanHierarchyFormatter(repository, depth).format(testPlan, System.out::print);
            } else {
                System.out.println("Plan ID:    " + testPlan.planID());
                System.out.println("Project ID: " + testPlan.projectID());
                System.out.println("Created at: " + testPlan.createdAt());
            }
        } catch (Exception e) {
            log.warn("Error formatting test plan: {}", e.getMessage());
        }
    }

    private void executeNode(TestPlanRepository repository) throws Exception {
        UUID uuid = UUID.fromString(target.nodeID);
        TestPlanNode node = repository.getNodeData(uuid)
            .orElseThrow(() -> new IllegalArgumentException("Node not found: " + target.nodeID));
        try {
            if (detail) {
                new TestPlanHierarchyFormatter(repository, depth).formatFromNode(node, System.out::print);
            } else {
                System.out.println("Node ID:   " + node.nodeID());
                System.out.println("Node type: " + node.nodeType());
                System.out.println("Name:      " + node.name());
            }
        } catch (Exception e) {
            log.warn("Error formatting node: {}", e.getMessage());
        }
    }
}