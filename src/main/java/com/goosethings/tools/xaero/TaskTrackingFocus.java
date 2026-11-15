package com.goosethings.tools.xaero;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Client-only gaze selection and interruptible opacity, independent of task availability. */
final class TaskTrackingFocus {
    static final long FADE_NANOS = 180_000_000L;
    private static final double MIN_ALIGNMENT = Math.cos(Math.toRadians(10));
    private final Map<String, Fade> fades = new HashMap<>();
    private long frameTime;
    private String selected;

    void update(List<GgdMapElementRenderer.Element> elements, Vec3 eye, Vec3 forward, long now) {
        frameTime = now;
        selected = select(elements, eye, forward);
        var available = new HashSet<String>();
        for (var element : elements) {
            if (element.kind() != GgdMapElementRenderer.ElementKind.TASK) continue;
            String id = element.textKey();
            available.add(id);
            boolean focused = id.equals(selected);
            Fade previous = fades.get(id);
            if (previous == null) {
                if (focused) fades.put(id, new Fade(0, true, now));
            } else if (previous.visible() != focused) {
                fades.put(id, new Fade(previous.sample(now), focused, now));
            }
        }
        // Completed tasks, map switches and expired exits never retain label state.
        fades.entrySet().removeIf(entry -> !available.contains(entry.getKey())
                || (!entry.getValue().visible() && entry.getValue().sample(now) <= 0));
    }

    float alpha(String id) {
        Fade fade = fades.get(id);
        return fade == null ? 0 : fade.sample(frameTime);
    }

    String selected() {
        return selected;
    }

    void clear() {
        selected = null;
        fades.clear();
    }

    static String select(List<GgdMapElementRenderer.Element> elements, Vec3 eye, Vec3 forward) {
        Vec3 direction = forward.normalize();
        double bestAlignment = MIN_ALIGNMENT;
        double nearestDistance = Double.POSITIVE_INFINITY;
        String result = null;
        for (var element : elements) {
            if (element.kind() != GgdMapElementRenderer.ElementKind.TASK) continue;
            Vec3 offset = new Vec3(element.x(), element.y(), element.z()).subtract(eye);
            double distance = offset.length();
            if (distance < 0.01) continue;
            double alignment = offset.dot(direction) / distance;
            if (alignment > bestAlignment || (alignment == bestAlignment && distance < nearestDistance)) {
                bestAlignment = alignment;
                nearestDistance = distance;
                result = element.textKey();
            }
        }
        return result;
    }

    static double hitDistance(double mouseX, double mouseY, int x, int y, float markerScale) {
        double radius = Math.max(5, 6 * markerScale + 2);
        double dx = mouseX - x;
        double dy = mouseY - y;
        return Math.abs(dx) <= radius && Math.abs(dy) <= radius
                ? dx * dx + dy * dy : Double.POSITIVE_INFINITY;
    }

    private record Fade(float start, boolean visible, long since) {
        float sample(long now) {
            double progress = Math.clamp((double) (now - since) / FADE_NANOS, 0, 1);
            if (progress <= 0) return start;
            if (progress >= 1) return visible ? 1 : 0;
            // Match the preview's ease-out cubic-bezier(.23, 1, .32, 1).
            double low = 0;
            double high = 1;
            for (int i = 0; i < 14; i++) {
                double t = (low + high) * 0.5;
                double x = 3 * (1 - t) * (1 - t) * t * 0.23
                        + 3 * (1 - t) * t * t * 0.32 + t * t * t;
                if (x < progress) low = t;
                else high = t;
            }
            double t = (low + high) * 0.5;
            double eased = 1 - Math.pow(1 - t, 3);
            return (float) (start + ((visible ? 1 : 0) - start) * eased);
        }
    }
}
