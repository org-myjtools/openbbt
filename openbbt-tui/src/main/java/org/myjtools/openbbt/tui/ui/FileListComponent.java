package org.myjtools.openbbt.tui.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.AbstractInteractableComponent;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.input.KeyStroke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class FileListComponent extends AbstractInteractableComponent<FileListComponent> {

    private record Entry(Path path, boolean isDir, boolean isParent) {}

    private Path currentDir;
    private List<Entry> entries = new ArrayList<>();
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private Consumer<Path> onFileSelected;
    private Consumer<Path> onDirChanged;

    public FileListComponent(Path startDir) {
        this.currentDir = startDir.toAbsolutePath().normalize();
        reload();
    }

    public void setOnFileSelected(Consumer<Path> listener) { this.onFileSelected = listener; }
    public void setOnDirChanged(Consumer<Path> listener)   { this.onDirChanged = listener; }
    public Path getCurrentDir()                            { return currentDir; }

    private void reload() {
        entries.clear();
        var parent = currentDir.getParent();
        if (parent != null) entries.add(new Entry(parent, true, true));

        try (var stream = Files.list(currentDir)) {
            stream.sorted(Comparator
                    .<Path, Boolean>comparing(p -> !Files.isDirectory(p))
                    .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                .forEach(p -> entries.add(new Entry(p, Files.isDirectory(p), false)));
        } catch (IOException ignored) {}

        selectedIndex = 0;
        scrollOffset = 0;
        invalidate();
    }

    @Override
    protected Result handleKeyStroke(KeyStroke key) {
        return switch (key.getKeyType()) {
            case ArrowUp -> {
                if (selectedIndex > 0) {
                    selectedIndex--;
                    adjustScroll();
                    invalidate();
                    yield Result.HANDLED;
                }
                yield Result.MOVE_FOCUS_UP;
            }
            case ArrowDown -> {
                if (selectedIndex < entries.size() - 1) {
                    selectedIndex++;
                    adjustScroll();
                    invalidate();
                    yield Result.HANDLED;
                }
                yield Result.MOVE_FOCUS_DOWN;
            }
            case Enter -> {
                if (!entries.isEmpty()) {
                    var e = entries.get(selectedIndex);
                    if (e.isDir()) {
                        currentDir = e.path().toAbsolutePath().normalize();
                        reload();
                        if (onDirChanged != null) onDirChanged.accept(currentDir);
                    } else if (onFileSelected != null) {
                        onFileSelected.accept(e.path());
                    }
                }
                yield Result.HANDLED;
            }
            default -> Result.UNHANDLED;
        };
    }

    private void adjustScroll() {
        int h = getSize().getRows();
        if (h <= 0) return;
        if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
        else if (selectedIndex >= scrollOffset + h) scrollOffset = selectedIndex - h + 1;
    }

    @Override
    protected InteractableRenderer<FileListComponent> createDefaultRenderer() {
        return new Renderer();
    }

    private class Renderer implements InteractableRenderer<FileListComponent> {

        @Override
        public TerminalPosition getCursorLocation(FileListComponent c) { return null; }

        @Override
        public TerminalSize getPreferredSize(FileListComponent c) {
            int max = entries.stream()
                .mapToInt(e -> nameOf(e).length() + 4)
                .max().orElse(30);
            return new TerminalSize(Math.max(max, 30), entries.size());
        }

        @Override
        public void drawComponent(TextGUIGraphics g, FileListComponent c) {
            int rows = g.getSize().getRows();
            int cols = g.getSize().getColumns();

            g.setBackgroundColor(TextColor.ANSI.BLACK);
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.fill(' ');

            for (int row = 0; row < rows; row++) {
                int idx = row + scrollOffset;
                if (idx >= entries.size()) break;

                var entry  = entries.get(idx);
                boolean sel = (idx == selectedIndex) && isFocused();

                String icon = entry.isParent() ? "↑ " : entry.isDir() ? "▸ " : "  ";
                String name = nameOf(entry);
                TextColor fg = colorOf(entry, sel);
                TextColor bg = sel ? TextColor.ANSI.CYAN_BRIGHT : TextColor.ANSI.BLACK;

                g.setBackgroundColor(bg);
                g.setForegroundColor(fg);
                g.putString(0, row, " ".repeat(cols));

                String line = " " + icon + name;
                if (line.length() > cols) line = line.substring(0, cols);
                if (sel) g.putString(0, row, line, SGR.BOLD);
                else     g.putString(0, row, line);
            }
        }

        private String nameOf(Entry e) {
            if (e.isParent()) return "..";
            String n = e.path().getFileName().toString();
            return e.isDir() ? n + "/" : n;
        }

        private TextColor colorOf(Entry e, boolean selected) {
            if (selected) return TextColor.ANSI.BLACK;
            if (e.isParent()) return TextColor.ANSI.YELLOW_BRIGHT;
            if (e.isDir())    return TextColor.ANSI.CYAN_BRIGHT;
            return TextColor.ANSI.WHITE;
        }
    }
}