package org.myjtools.openbbt.tui.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.AbstractInteractableComponent;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import org.myjtools.openbbt.tui.model.PlanNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PlanTreeComponent extends AbstractInteractableComponent<PlanTreeComponent> {

    private record FlatItem(PlanNode node, int depth) {}

    private PlanNode root;
    private final List<FlatItem> flatItems = new ArrayList<>();
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private Consumer<PlanNode> onSelectionChange;

    public PlanTreeComponent(PlanNode root) {
        this.root = root;
        rebuildFlatList();
    }

    public void setOnSelectionChange(Consumer<PlanNode> listener) {
        this.onSelectionChange = listener;
    }

    public PlanNode getSelectedNode() {
        if (flatItems.isEmpty()) return null;
        return flatItems.get(selectedIndex).node();
    }

    public void refresh() {
        rebuildFlatList();
        if (selectedIndex >= flatItems.size()) {
            selectedIndex = Math.max(0, flatItems.size() - 1);
        }
        invalidate();
    }

    public void reload(PlanNode newRoot) {
        this.root = newRoot;
        selectedIndex = 0;
        scrollOffset = 0;
        rebuildFlatList();
        fireSelectionChange();
        invalidate();
    }

    private void rebuildFlatList() {
        flatItems.clear();
        if (root != null) addToFlat(root, 0);
    }

    private void addToFlat(PlanNode node, int depth) {
        flatItems.add(new FlatItem(node, depth));
        if (node.hasChildren() && node.isExpanded()) {
            for (var child : node.getChildren()) addToFlat(child, depth + 1);
        }
    }

    @Override
    protected Result handleKeyStroke(KeyStroke key) {
        return switch (key.getKeyType()) {
            case ArrowUp -> {
                if (selectedIndex > 0) {
                    selectedIndex--;
                    adjustScroll();
                    fireSelectionChange();
                    invalidate();
                    yield Result.HANDLED;
                }
                yield Result.MOVE_FOCUS_UP;
            }
            case ArrowDown -> {
                if (selectedIndex < flatItems.size() - 1) {
                    selectedIndex++;
                    adjustScroll();
                    fireSelectionChange();
                    invalidate();
                    yield Result.HANDLED;
                }
                yield Result.MOVE_FOCUS_DOWN;
            }
            case Enter -> {
                if (!flatItems.isEmpty()) {
                    var node = flatItems.get(selectedIndex).node();
                    if (node.hasChildren()) {
                        node.toggleExpanded();
                        rebuildFlatList();
                        adjustScroll();
                        invalidate();
                    }
                }
                yield Result.HANDLED;
            }
            default -> Result.UNHANDLED;
        };
    }

    private void adjustScroll() {
        int visibleHeight = getSize().getRows();
        if (visibleHeight <= 0) return;
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleHeight) {
            scrollOffset = selectedIndex - visibleHeight + 1;
        }
    }

    private void fireSelectionChange() {
        if (onSelectionChange != null && !flatItems.isEmpty()) {
            onSelectionChange.accept(flatItems.get(selectedIndex).node());
        }
    }

    @Override
    protected InteractableRenderer<PlanTreeComponent> createDefaultRenderer() {
        return new Renderer();
    }

    // ─── Renderer ────────────────────────────────────────────────────────────

    private class Renderer implements InteractableRenderer<PlanTreeComponent> {

        @Override
        public TerminalPosition getCursorLocation(PlanTreeComponent c) {
            return null; // no text cursor
        }

        @Override
        public TerminalSize getPreferredSize(PlanTreeComponent c) {
            int maxWidth = flatItems.stream()
                .mapToInt(item -> item.depth() * 2 + 6 + item.node().getLabel().length())
                .max().orElse(30);
            return new TerminalSize(Math.max(maxWidth, 30), flatItems.size());
        }

        @Override
        public void drawComponent(TextGUIGraphics g, PlanTreeComponent c) {
            int rows = g.getSize().getRows();
            int cols = g.getSize().getColumns();

            // Clear background
            g.setBackgroundColor(TextColor.ANSI.BLACK);
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.fill(' ');

            for (int row = 0; row < rows; row++) {
                int idx = row + scrollOffset;
                if (idx >= flatItems.size()) break;

                var item = flatItems.get(idx);
                var node = item.node();
                boolean selected = (idx == selectedIndex) && isFocused();

                TextColor fg = selected ? TextColor.ANSI.BLACK : colorForStatus(node.getStatus());
                TextColor bg = selected ? TextColor.ANSI.CYAN_BRIGHT : TextColor.ANSI.BLACK;

                // Fill line background
                g.setBackgroundColor(bg);
                g.setForegroundColor(fg);
                g.putString(0, row, " ".repeat(cols));

                // Build line text
                String indent      = "  ".repeat(item.depth());
                String expandIcon  = node.hasChildren() ? (node.isExpanded() ? "▼ " : "▶ ") : "  ";
                String statusIcon  = iconForStatus(node.getStatus());
                String text        = indent + expandIcon + statusIcon + " " + node.getLabel();

                if (text.length() > cols) text = text.substring(0, cols);

                if (selected) {
                    g.putString(0, row, text, SGR.BOLD);
                } else {
                    g.putString(0, row, text);
                }
            }
        }

        private String iconForStatus(PlanNode.Status s) {
            return switch (s) {
                case NOT_VALIDATED -> "○";
                case VALIDATED     -> "✓";
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

        private TextColor colorForStatus(PlanNode.Status s) {
            return switch (s) {
                case NOT_VALIDATED -> TextColor.ANSI.DEFAULT;
                case VALIDATED     -> TextColor.ANSI.DEFAULT;
                case INVALID       -> TextColor.ANSI.RED_BRIGHT;
                case HAS_ISSUES    -> TextColor.ANSI.YELLOW_BRIGHT;
                case PENDING       -> TextColor.ANSI.DEFAULT;
                case RUNNING       -> TextColor.ANSI.YELLOW_BRIGHT;
                case PASS          -> TextColor.ANSI.GREEN_BRIGHT;
                case FAIL          -> TextColor.ANSI.RED_BRIGHT;
                case SKIPPED       -> TextColor.ANSI.BLACK_BRIGHT;
                case UNDEFINED     -> TextColor.ANSI.MAGENTA_BRIGHT;
            };
        }
    }
}