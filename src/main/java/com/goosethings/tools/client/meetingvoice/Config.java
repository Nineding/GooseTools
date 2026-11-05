package com.goosethings.tools.client.meetingvoice;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public final class Config {
    public boolean enabled = true, showSelf = false, animations = true;
    public final boolean meetingOnly = true;
    public final String meetingGroup = "ggd_meeting";
    public double range = 24, bubbleScale = 1, bubbleGap = 4;
    public double gateDb = -48, sensitivity = 1;
    public double attackMs = 55, releaseMs = 140, holdMs = 250;

    public static Config load(Path path) throws IOException {
        Config c = new Config();
        Properties p = new Properties();
        if (Files.exists(path)) try (Reader r = Files.newBufferedReader(path)) { p.load(r); }
        c.enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
        c.showSelf = Boolean.parseBoolean(p.getProperty("showSelf", "false"));
        c.animations = Boolean.parseBoolean(p.getProperty("bubbleAnimations", "true"));
        c.range = number(p, "range", 24, 2, 48);
        c.bubbleScale = number(p, "bubbleScale", 1, 0.7, 1.3);
        c.bubbleGap = number(p, "bubbleGap", 4, 3, 8);
        c.gateDb = number(p, "gateDb", -48, -80, -10);
        c.sensitivity = number(p, "sensitivity", 1, 0.25, 4);
        // D-plan timings intentionally do not inherit the old equalizer timings.
        c.attackMs = number(p, "bubbleAttackMs", 55, 40, 80);
        c.releaseMs = number(p, "bubbleReleaseMs", 140, 100, 180);
        c.holdMs = number(p, "bubbleHoldMs", 250, 200, 300);
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, "# MeetingVoice beta.9 / D-plan / meetings only\n"
                + "enabled=true\nshowSelf=false\nrange=24\ngateDb=-48\nsensitivity=1\n"
                + "bubbleScale=1\nbubbleGap=4\nbubbleAnimations=true\n"
                + "bubbleAttackMs=55\nbubbleReleaseMs=140\nbubbleHoldMs=250\n");
        }
        return c;
    }

    private static double number(Properties p, String key, double fallback, double min, double max) {
        try { double v = Double.parseDouble(p.getProperty(key, "")); return Double.isFinite(v) ? Math.max(min, Math.min(max, v)) : fallback; }
        catch (NumberFormatException e) { return fallback; }
    }
}
