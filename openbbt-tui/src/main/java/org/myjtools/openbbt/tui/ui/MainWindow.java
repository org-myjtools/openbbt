package org.myjtools.openbbt.tui.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.myjtools.openbbt.tui.mock.MockData;
import org.myjtools.openbbt.tui.model.PlanNode;

import java.util.List;

public class MainWindow extends BasicWindow {

    private final PlanNode plan;
    private final PlanTreeComponent treeComponent;
    private final DetailPanel detailPanel;
    private final Label statusLabel;
    private volatile boolean running = false;

    public MainWindow() {
        super(" OpenBBT ");
        setHints(List.of(Hint.FULL_SCREEN));

        plan = MockData.createMockPlan();
        treeComponent = new PlanTreeComponent(plan);
        detailPanel = new DetailPanel();
        statusLabel = new Label(" Ready");

        treeComponent.setOnSelectionChange(detailPanel::showNode);
        detailPanel.showNode(plan);

        setComponent(buildLayout());
    }

    // ─── Layout ──────────────────────────────────────────────────────────────

    private Panel buildLayout() {
        var root = new Panel(new LinearLayout(Direction.VERTICAL));

        // Content area: tree | detail
        var content = new Panel(new GridLayout(2));

        content.addComponent(
            treeComponent.withBorder(Borders.singleLine(" Plan ")),
            GridLayout.createLayoutData(
                GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, false, true));

        content.addComponent(
            detailPanel.getPanel().withBorder(Borders.singleLine(" Detail ")),
            GridLayout.createLayoutData(
                GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, true));

        // Status bar
        var statusBar = new Panel(new LinearLayout(Direction.HORIZONTAL));
        var shortcuts = new Label(" [↑↓] Navigate  [Enter] Expand  [R] Run  [F] Filter  [Q] Quit ");
        statusBar.addComponent(shortcuts);
        statusBar.addComponent(statusLabel);

        root.addComponent(content, LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        root.addComponent(new Separator(Direction.HORIZONTAL));
        root.addComponent(statusBar);
        return root;
    }

    // ─── Input handling ──────────────────────────────────────────────────────

    @Override
    public boolean handleInput(KeyStroke key) {
        if (key.getKeyType() == KeyType.Character) {
            return switch (key.getCharacter()) {
                case 'q', 'Q' -> { close(); yield true; }
                case 'r', 'R' -> { startRun(); yield true; }
                case 'f', 'F' -> { openFilterDialog(); yield true; }
                default -> super.handleInput(key);
            };
        }
        if (key.getKeyType() == KeyType.Escape) {
            close();
            return true;
        }
        return super.handleInput(key);
    }

    // ─── Run simulation ──────────────────────────────────────────────────────

    private void startRun() {
        if (running) {
            setStatus(" Already running...");
            return;
        }
        running = true;
        plan.resetStatus();
        refreshUi(" Running...");

        Thread.ofVirtual().name("run-simulation").start(() -> {
            try {
                for (var feature : plan.getChildren()) {
                    for (var scenario : feature.getChildren()) {
                        runScenario(scenario);
                    }
                }
                // Propagate statuses up
                updateFeatureStatuses();
                refreshUi(" Done  ✓");
            } finally {
                running = false;
            }
        });
    }

    private void runScenario(PlanNode scenario) {
        scenario.setStatus(PlanNode.Status.RUNNING);
        refreshUi(null);

        boolean scenarioPassed = true;
        boolean failedEarly = false;

        for (var step : scenario.getChildren()) {
            if (failedEarly) break;

            step.setStatus(PlanNode.Status.RUNNING);
            refreshUi(null);
            sleep(120);

            // ~85% pass rate for prototype realism
            boolean pass = Math.random() > 0.15;
            step.setStatus(pass ? PlanNode.Status.PASS : PlanNode.Status.FAIL);
            if (!pass) { scenarioPassed = false; failedEarly = true; }
            refreshUi(null);
            sleep(60);
        }

        scenario.setStatus(scenarioPassed ? PlanNode.Status.PASS : PlanNode.Status.FAIL);
        refreshUi(null);
    }

    private void updateFeatureStatuses() {
        for (var feature : plan.getChildren()) {
            boolean anyFail    = feature.getChildren().stream().anyMatch(s -> s.getStatus() == PlanNode.Status.FAIL);
            boolean anyPending = feature.getChildren().stream().anyMatch(s -> s.getStatus() == PlanNode.Status.PENDING);
            if (anyFail)         feature.setStatus(PlanNode.Status.FAIL);
            else if (anyPending) feature.setStatus(PlanNode.Status.PENDING);
            else                 feature.setStatus(PlanNode.Status.PASS);
        }
        boolean anyFail    = plan.getChildren().stream().anyMatch(f -> f.getStatus() == PlanNode.Status.FAIL);
        boolean anyPending = plan.getChildren().stream().anyMatch(f -> f.getStatus() == PlanNode.Status.PENDING);
        if (anyFail)         plan.setStatus(PlanNode.Status.FAIL);
        else if (anyPending) plan.setStatus(PlanNode.Status.PENDING);
        else                 plan.setStatus(PlanNode.Status.PASS);
    }

    // ─── Filter dialog ───────────────────────────────────────────────────────

    private void openFilterDialog() {
        String tag = TextInputDialog.showDialog(getTextGUI(), "Filter by tag", "Enter tag expression:", "");
        if (tag != null && !tag.isBlank()) {
            setStatus(" Filter: " + tag + "  (not yet connected to backend)");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void refreshUi(String newStatus) {
        getTextGUI().getGUIThread().invokeLater(() -> {
            treeComponent.refresh();
            detailPanel.showNode(treeComponent.getSelectedNode());
            if (newStatus != null) statusLabel.setText(newStatus);
        });
    }

    private void setStatus(String text) {
        getTextGUI().getGUIThread().invokeLater(() -> statusLabel.setText(text));
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}