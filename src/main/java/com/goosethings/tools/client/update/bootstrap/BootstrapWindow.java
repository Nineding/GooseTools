package com.goosethings.tools.client.update.bootstrap;

import com.goosethings.tools.client.update.UpdateMonitor;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.concurrent.*;
import javax.swing.*;

/** Independent launcher window; the Minecraft JVM has not started yet. */
final class BootstrapWindow {
    private final BootstrapText text;
    private final JFrame frame;
    private final JLabel status = new JLabel(" ");
    private final JLabel detail = new JLabel(" ");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JButton retry;
    private final JButton proceed;
    private final Timer timer;
    private volatile UpdateMonitor monitor;
    private volatile CompletableFuture<Boolean> decision;
    private volatile boolean blocked;
    private volatile boolean busy;

    BootstrapWindow(Path game, UpdateMonitor monitor) {
        this.monitor = monitor;
        text = new BootstrapText(game);
        frame = new JFrame(text.text("bootstrap_title", "GooseTools - update before launch"));
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        JPanel labels = new JPanel(new GridLayout(2, 1, 0, 10));
        labels.add(status); labels.add(detail);
        content.add(labels, BorderLayout.NORTH);
        progress.setStringPainted(true);
        content.add(progress, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        retry = new JButton(text.text("retry", "Retry"));
        retry.setEnabled(false);
        retry.addActionListener(event -> { if (decision != null) decision.complete(true); });
        proceed = new JButton(text.text("continue", "Continue to game"));
        proceed.addActionListener(event -> continueGame());
        buttons.add(retry); buttons.add(proceed); content.add(buttons, BorderLayout.SOUTH);
        frame.setContentPane(content);
        frame.setSize(560, 230); frame.setMinimumSize(new Dimension(480, 230));
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { continueGame(); }
        });
        timer = new Timer(100, event -> refresh());
        timer.start(); frame.setVisible(true);
    }
    private void continueGame() {
        if (blocked) return;
        if (monitor.snapshot().phase() == UpdateMonitor.Phase.INSTALLING) return;
        monitor.cancel();
        if (decision != null) decision.complete(false);
    }
    private void refresh() {
        var value = monitor.snapshot();
        boolean failure = value.phase() == UpdateMonitor.Phase.FAILED;
        retry.setEnabled(failure && decision != null);
        proceed.setEnabled(!blocked && value.phase() != UpdateMonitor.Phase.INSTALLING);
        status.setText(value.phase() == UpdateMonitor.Phase.READY
                ? text.text("bootstrap_finished", "GooseTools %s installed. Starting Minecraft...", value.version())
                : text.text("phase_" + value.phase().name().toLowerCase(java.util.Locale.ROOT), value.phase().name()));
        detail.setText(blocked ? busy ? text.text("bootstrap_busy", "Close the other running game, then retry; or cancel this launch in HMCL.")
                : text.text("bootstrap_repair", "Retry recovery, or cancel this launch in HMCL.")
                : value.total() > 0 ? text.text("bytes", "%s / %s MiB", mib(value.downloaded()), mib(value.total()))
                : text.text("bootstrap_hint", "Updates finish before Minecraft loads. Alpha releases are included by default."));
        proceed.setText(text.text("continue", "Continue to game"));
        progress.setIndeterminate(value.percent() < 0 && (value.phase() == UpdateMonitor.Phase.CHECKING));
        progress.setValue(Math.max(0, value.percent()));
        progress.setString(value.total() > 0 ? value.percent() + "%" : " ");
    }
    private static String mib(long bytes) { return String.format(java.util.Locale.ROOT, "%.2f", bytes / 1048576.0); }
    boolean retry() throws Exception {
        decision = new CompletableFuture<>();
        try { if (monitor.snapshot().phase() == UpdateMonitor.Phase.CANCELLED) return false; return decision.get(); }
        finally { decision = null; }
    }
    void monitor(UpdateMonitor value) { monitor = value; }
    void blocked(boolean value, boolean inUse) { busy = inUse; blocked = value; }
    void close() throws Exception { SwingUtilities.invokeAndWait(() -> { timer.stop(); frame.dispose(); }); }
}
