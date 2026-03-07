package org.myjtools.openbbt.tui.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Direction;
import org.myjtools.openbbt.tui.model.PlanNode;
import org.myjtools.openbbt.tui.model.PlanNode.Status;

import java.util.ArrayList;
import java.util.List;

public class DetailPanel {

    public enum Mode { PLAN, EXECUTION }

    private final Panel panel;
    private final Mode mode;

    public DetailPanel(Mode mode) {
        this.mode = mode;
        panel = new Panel(new LinearLayout(Direction.VERTICAL));
        showEmpty();
    }

    public Panel getPanel() {
        return panel;
    }

    public void showNode(PlanNode node) {
        panel.removeAllComponents();
        if (node == null) { showEmpty(); return; }
        switch (node.getType()) {
            case PROJECT    -> showProject(node);
            case FEATURE    -> showFeature(node);
            case SCENARIO   -> showScenario(node);
            case STEP_GROUP -> showStepGroup(node);
            case STEP       -> showStep(node);
        }
    }

    // ─── Project ─────────────────────────────────────────────────────────────

    private void showProject(PlanNode node) {
        addTitle("Project: " + node.getLabel(), node.getStatus());
        addSeparator();

        int features  = node.getChildren().size();
        int scenarios = node.getChildren().stream().mapToInt(f -> f.getChildren().size()).sum();
        panel.addComponent(new EmptySpace());
        addInfo("Features:  " + features);
        addInfo("Scenarios: " + scenarios);
        panel.addComponent(new EmptySpace());

        if (mode == Mode.PLAN) {
            long invalid   = countDeep(node, n -> n.getStatus() == Status.INVALID,    PlanNode.Type.STEP);
            long hasIssues = countDeep(node, n -> n.getStatus() == Status.HAS_ISSUES, PlanNode.Type.SCENARIO);
            long validated = countDeep(node, n -> n.getStatus() == Status.VALIDATED,  PlanNode.Type.SCENARIO);
            addStat("  OK      ", validated, TextColor.ANSI.DEFAULT);
            addStat("! Issues  ", hasIssues, TextColor.ANSI.YELLOW_BRIGHT);
            addStat("✗ Errors  ", invalid,   TextColor.ANSI.RED_BRIGHT);
            showValidationIssues(node);
        } else {
            long pass    = countDeep(node, n -> n.getStatus() == Status.PASS,      PlanNode.Type.SCENARIO);
            long fail    = countDeep(node, n -> n.getStatus() == Status.FAIL,      PlanNode.Type.SCENARIO);
            long skipped = countDeep(node, n -> n.getStatus() == Status.SKIPPED,   PlanNode.Type.SCENARIO);
            long undef   = countDeep(node, n -> n.getStatus() == Status.UNDEFINED, PlanNode.Type.SCENARIO);
            addStat("✓ Passed  ", pass,    TextColor.ANSI.GREEN_BRIGHT);
            addStat("✗ Failed  ", fail,    TextColor.ANSI.RED_BRIGHT);
            addStat("- Skipped ", skipped, TextColor.ANSI.BLACK_BRIGHT);
            addStat("? Undefined", undef,  TextColor.ANSI.MAGENTA_BRIGHT);
        }
    }

    // ─── Feature ─────────────────────────────────────────────────────────────

    private void showFeature(PlanNode node) {
        addTitle("Feature: " + node.getLabel(), node.getStatus());
        addSeparator();
        panel.addComponent(new EmptySpace());

        for (var scenario : node.getChildren()) {
            var lbl = new Label("  " + iconFor(scenario.getStatus()) + "  " + scenario.getLabel());
            lbl.setForegroundColor(colorFor(scenario.getStatus()));
            panel.addComponent(lbl);
        }

        if (mode == Mode.PLAN) {
            showValidationIssues(node);
        }
    }

    // ─── Scenario ────────────────────────────────────────────────────────────

    private void showScenario(PlanNode node) {
        addTitle("Scenario: " + node.getLabel(), node.getStatus());
        addSeparator();
        panel.addComponent(new EmptySpace());

        if (node.getChildren().isEmpty()) {
            panel.addComponent(new Label("  (no steps)"));
        } else {
            for (var child : node.getChildren()) {
                var lbl = new Label("  " + iconFor(child.getStatus()) + "  " + child.getLabel());
                lbl.setForegroundColor(colorFor(child.getStatus()));
                panel.addComponent(lbl);
                if (mode == Mode.PLAN && child.getStatus() == Status.INVALID
                        && child.getValidationMessage() != null) {
                    var msg = new Label("      ↳ " + child.getValidationMessage());
                    msg.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
                    panel.addComponent(msg);
                }
            }
        }
    }

    // ─── Step Group ──────────────────────────────────────────────────────────

    private void showStepGroup(PlanNode node) {
        addTitle("Steps: " + node.getLabel(), node.getStatus());
        addSeparator();
        panel.addComponent(new EmptySpace());

        for (var step : node.getChildren()) {
            var lbl = new Label("  " + iconFor(step.getStatus()) + "  " + step.getLabel());
            lbl.setForegroundColor(colorFor(step.getStatus()));
            panel.addComponent(lbl);
            if (mode == Mode.PLAN && step.getStatus() == Status.INVALID
                    && step.getValidationMessage() != null) {
                var msg = new Label("      ↳ " + step.getValidationMessage());
                msg.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
                panel.addComponent(msg);
            }
        }
    }

    // ─── Step ────────────────────────────────────────────────────────────────

    private void showStep(PlanNode node) {
        addTitle("Step", node.getStatus());
        addSeparator();
        panel.addComponent(new EmptySpace());
        addInfo(node.getLabel());

        if (mode == Mode.PLAN) {
            if (node.getStatus() == Status.INVALID && node.getValidationMessage() != null) {
                panel.addComponent(new EmptySpace());
                var msg = new Label("  " + node.getValidationMessage());
                msg.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
                panel.addComponent(msg);
            }
        } else {
            if (node.getStatus() != Status.PENDING && node.getStatus() != Status.NOT_VALIDATED) {
                panel.addComponent(new EmptySpace());
                addInfo("Status: " + iconFor(node.getStatus()) + " " + node.getStatus());
                if ((node.getStatus() == Status.FAIL || node.getStatus() == Status.UNDEFINED)
                        && node.getValidationMessage() != null) {
                    var msg = new Label("  " + node.getValidationMessage());
                    msg.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
                    panel.addComponent(msg);
                }
            }
        }
    }

    // ─── Validation issues list ───────────────────────────────────────────────

    private void showValidationIssues(PlanNode node) {
        List<PlanNode> issues = new ArrayList<>();
        collectInvalid(node, issues);
        if (issues.isEmpty()) return;

        panel.addComponent(new EmptySpace());
        var header = new Label("  Validation errors:");
        header.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
        header.addStyle(SGR.BOLD);
        panel.addComponent(header);

        for (var issue : issues) {
            String msg = issue.getValidationMessage() != null ? issue.getValidationMessage() : "validation error";
            var lbl = new Label("  ✗ " + issue.getLabel());
            lbl.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
            panel.addComponent(lbl);
            var detail = new Label("      " + msg);
            detail.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
            panel.addComponent(detail);
        }
    }

    private static void collectInvalid(PlanNode node, List<PlanNode> result) {
        if (node.getStatus() == Status.INVALID) result.add(node);
        for (var child : node.getChildren()) collectInvalid(child, result);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void showEmpty() {
        panel.addComponent(new EmptySpace());
        panel.addComponent(new Label("  Select a node in the plan tree."));
    }

    private void addTitle(String text, Status status) {
        panel.addComponent(new EmptySpace());
        var lbl = new Label("  " + iconFor(status) + "  " + text);
        lbl.setForegroundColor(colorFor(status));
        lbl.addStyle(SGR.BOLD);
        panel.addComponent(lbl);
    }

    private void addSeparator() {
        panel.addComponent(new Label("  " + "─".repeat(48)));
    }

    private void addInfo(String text) {
        panel.addComponent(new Label("  " + text));
    }

    private void addStat(String label, long count, TextColor color) {
        var lbl = new Label("  " + label + ":  " + count);
        lbl.setForegroundColor(color);
        panel.addComponent(lbl);
    }

    private static String iconFor(Status s) {
        return switch (s) {
            case NOT_VALIDATED -> " ";
            case VALIDATED     -> " ";
            case INVALID       -> "✗";
            case HAS_ISSUES    -> "!";
            case PENDING       -> "○";
            case RUNNING       -> "►";
            case PASS          -> "✓";
            case FAIL          -> "✗";
            case SKIPPED       -> "-";
            case UNDEFINED     -> "?";
        };
    }

    private TextColor colorFor(Status s) {
        if (mode == Mode.PLAN) {
            return switch (s) {
                case INVALID    -> TextColor.ANSI.RED_BRIGHT;
                case HAS_ISSUES -> TextColor.ANSI.YELLOW_BRIGHT;
                default         -> TextColor.ANSI.DEFAULT;
            };
        } else {
            return switch (s) {
                case PASS      -> TextColor.ANSI.GREEN_BRIGHT;
                case FAIL      -> TextColor.ANSI.RED_BRIGHT;
                case RUNNING   -> TextColor.ANSI.YELLOW_BRIGHT;
                case SKIPPED   -> TextColor.ANSI.BLACK_BRIGHT;
                case UNDEFINED -> TextColor.ANSI.MAGENTA_BRIGHT;
                default        -> TextColor.ANSI.DEFAULT;
            };
        }
    }

    private static long countDeep(PlanNode node, java.util.function.Predicate<PlanNode> pred, PlanNode.Type type) {
        long count = 0;
        if (node.getType() == type && pred.test(node)) count++;
        for (var child : node.getChildren()) count += countDeep(child, pred, type);
        return count;
    }
}
