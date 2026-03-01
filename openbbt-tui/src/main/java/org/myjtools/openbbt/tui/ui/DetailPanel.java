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

public class DetailPanel {

    private final Panel panel;

    public DetailPanel() {
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
            case PROJECT  -> showProject(node);
            case FEATURE  -> showFeature(node);
            case SCENARIO -> showScenario(node);
            case STEP     -> showStep(node);
        }
    }

    // ─── Project ─────────────────────────────────────────────────────────────

    private void showProject(PlanNode node) {
        addTitle("Project: " + node.getLabel(), node.getStatus());
        addSeparator();

        int features  = node.getChildren().size();
        int scenarios = node.getChildren().stream().mapToInt(f -> f.getChildren().size()).sum();
        long pass     = countDeep(node, Status.PASS,    PlanNode.Type.SCENARIO);
        long fail     = countDeep(node, Status.FAIL,    PlanNode.Type.SCENARIO);
        long pending  = countDeep(node, Status.PENDING, PlanNode.Type.SCENARIO);

        panel.addComponent(new EmptySpace());
        addInfo("Features:  " + features);
        addInfo("Scenarios: " + scenarios);
        panel.addComponent(new EmptySpace());
        addStat("✓ Passed ", pass,   TextColor.ANSI.GREEN_BRIGHT);
        addStat("✗ Failed ", fail,   TextColor.ANSI.RED_BRIGHT);
        addStat("○ Pending", pending, TextColor.ANSI.DEFAULT);
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
    }

    // ─── Scenario ────────────────────────────────────────────────────────────

    private void showScenario(PlanNode node) {
        addTitle("Scenario: " + node.getLabel(), node.getStatus());
        addSeparator();
        panel.addComponent(new EmptySpace());

        if (node.getChildren().isEmpty()) {
            panel.addComponent(new Label("  (no steps)"));
            return;
        }
        for (var step : node.getChildren()) {
            var lbl = new Label("  " + iconFor(step.getStatus()) + "  " + step.getLabel());
            lbl.setForegroundColor(colorFor(step.getStatus()));
            panel.addComponent(lbl);
        }
    }

    // ─── Step ────────────────────────────────────────────────────────────────

    private void showStep(PlanNode node) {
        addTitle("Step", node.getStatus());
        addSeparator();
        panel.addComponent(new EmptySpace());
        addInfo(node.getLabel());
        panel.addComponent(new EmptySpace());
        addInfo("Status: " + iconFor(node.getStatus()) + " " + node.getStatus());
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
            case PENDING -> "○";
            case RUNNING -> "►";
            case PASS    -> "✓";
            case FAIL    -> "✗";
        };
    }

    private static TextColor colorFor(Status s) {
        return switch (s) {
            case PENDING -> TextColor.ANSI.DEFAULT;
            case RUNNING -> TextColor.ANSI.YELLOW_BRIGHT;
            case PASS    -> TextColor.ANSI.GREEN_BRIGHT;
            case FAIL    -> TextColor.ANSI.RED_BRIGHT;
        };
    }

    private static long countDeep(PlanNode node, Status status, PlanNode.Type type) {
        long count = 0;
        if (node.getType() == type && node.getStatus() == status) count++;
        for (var child : node.getChildren()) count += countDeep(child, status, type);
        return count;
    }
}