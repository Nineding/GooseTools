package com.goosethings.tools.task.profession;

import com.goosethings.tools.task.TaskType;

/** Public question parameters and isolated professional rules. SI conversions are explicit. */
public abstract class ProfessionModel {
    public final TaskType type;
    protected ProfessionModel(TaskType type) { this.type = type; }
    public static ProfessionModel create(TaskType type, long seed) {
        return switch (type) {
            case TELECOM -> new TelecomModel(seed);
            case NUCLEAR -> new NuclearModel(seed);
            case FOODSAFETY -> new FoodSafetyModel(seed);
            case CIVIL -> new CivilModel(seed);
            default -> throw new IllegalArgumentException("Not a profession: " + type);
        };
    }
    public abstract boolean acceptsChoice(int stage, int slot, int value);
    public abstract boolean acceptsDial(int stage, int slot, double value);
    public abstract ProfessionFeedback check(int stage, int[] choices, double[] dials, double a, double b, double c);
    public double[] initialDials() { return new double[16]; }
    public static boolean near(double supplied, double expected, double absolute) {
        return Double.isFinite(supplied) && Math.abs(supplied - expected) <= Math.max(absolute, Math.abs(expected) * .01) + 1e-9;
    }
    protected static boolean selected(int[] choices, int first, int count) {
        for (int i = first; i < first + count; i++) if (choices[i] < 0) return false;
        return true;
    }
}
