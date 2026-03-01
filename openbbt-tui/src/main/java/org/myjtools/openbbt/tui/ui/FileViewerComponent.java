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
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileViewerComponent extends AbstractInteractableComponent<FileViewerComponent> {

    private static final int MAX_LINES    = 2000;
    private static final int LINE_NUM_COL = 5; // "1234 "

    private Path   currentFile;
    private List<String> lines  = new ArrayList<>();
    private String errorMessage = null;
    private int    scrollOffset = 0;

    public void loadFile(Path path) {
        this.currentFile = path;
        this.scrollOffset = 0;
        this.errorMessage = null;
        this.lines = new ArrayList<>();
        try {
            var all = Files.readAllLines(path, StandardCharsets.UTF_8);
            lines = all.size() > MAX_LINES ? new ArrayList<>(all.subList(0, MAX_LINES)) : all;
        } catch (MalformedInputException e) {
            errorMessage = "Binary file — cannot display content.";
        } catch (IOException e) {
            errorMessage = "Error reading file: " + e.getMessage();
        }
        invalidate();
    }

    public void clear() {
        currentFile = null;
        lines = new ArrayList<>();
        errorMessage = null;
        scrollOffset = 0;
        invalidate();
    }

    @Override
    protected Result handleKeyStroke(KeyStroke key) {
        return switch (key.getKeyType()) {
            case ArrowUp -> {
                if (scrollOffset > 0) { scrollOffset--; invalidate(); }
                yield Result.HANDLED; // never trigger spatial lookup — use Tab to switch focus
            }
            case ArrowDown -> {
                int max = Math.max(0, lines.size() - 1);
                if (scrollOffset < max) { scrollOffset++; invalidate(); }
                yield Result.HANDLED;
            }
            case PageUp -> {
                scrollOffset = Math.max(0, scrollOffset - getSize().getRows());
                invalidate();
                yield Result.HANDLED;
            }
            case PageDown -> {
                int max = Math.max(0, lines.size() - getSize().getRows());
                scrollOffset = Math.min(max, scrollOffset + getSize().getRows());
                invalidate();
                yield Result.HANDLED;
            }
            default -> Result.UNHANDLED;
        };
    }

    @Override
    protected InteractableRenderer<FileViewerComponent> createDefaultRenderer() {
        return new Renderer();
    }

    private class Renderer implements InteractableRenderer<FileViewerComponent> {

        @Override
        public TerminalPosition getCursorLocation(FileViewerComponent c) { return null; }

        @Override
        public TerminalSize getPreferredSize(FileViewerComponent c) {
            return new TerminalSize(60, Math.max(lines.size(), 5));
        }

        @Override
        public void drawComponent(TextGUIGraphics g, FileViewerComponent c) {
            int rows = g.getSize().getRows();
            int cols = g.getSize().getColumns();

            g.setBackgroundColor(TextColor.ANSI.BLACK);
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.fill(' ');

            // No file selected
            if (currentFile == null) {
                String msg = "Select a file to view its contents.";
                int x = Math.max(0, (cols - msg.length()) / 2);
                int y = rows / 2;
                g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
                g.putString(x, y, msg);
                return;
            }

            // Error state
            if (errorMessage != null) {
                g.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
                g.putString(2, 1, errorMessage.length() > cols - 2
                    ? errorMessage.substring(0, cols - 2) : errorMessage);
                return;
            }

            // File name header
            String header = " " + currentFile.getFileName().toString();
            if (isFocused()) {
                g.setBackgroundColor(TextColor.ANSI.BLACK_BRIGHT);
                g.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT);
                g.putString(0, 0, " ".repeat(cols));
                g.putString(0, 0, header.length() > cols ? header.substring(0, cols) : header, SGR.BOLD);
                g.setBackgroundColor(TextColor.ANSI.BLACK);
            }

            // Content lines (start at row 0, header on same area managed outside via border title)
            int contentCols = Math.max(1, cols - LINE_NUM_COL);

            for (int row = 0; row < rows; row++) {
                int lineIdx = row + scrollOffset;
                if (lineIdx >= lines.size()) break;

                // Line number
                g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
                g.putString(0, row, String.format("%-4d ", lineIdx + 1));

                // Content
                String content = lines.get(lineIdx).replace("\t", "    ");
                if (content.length() > contentCols) content = content.substring(0, contentCols);
                g.setForegroundColor(TextColor.ANSI.WHITE);
                g.putString(LINE_NUM_COL, row, content);
            }

            // Scroll indicator (bottom right)
            if (lines.size() > rows) {
                int pct = scrollOffset * 100 / Math.max(1, lines.size() - rows);
                String indicator = " " + pct + "% ";
                g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
                g.setBackgroundColor(TextColor.ANSI.BLACK);
                int indX = Math.max(0, cols - indicator.length());
                g.putString(indX, rows - 1, indicator);
            }
        }
    }
}