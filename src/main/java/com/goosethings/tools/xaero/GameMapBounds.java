package com.goosethings.tools.xaero;

public record GameMapBounds(String id, double minX, double maxX, double minZ, double maxZ) {
    public static final GameMapBounds ANTIQUE =
            new GameMapBounds("antique", -352.0D, -144.0D, -192.0D, 0.0D);
    public static final GameMapBounds POOLCORE =
            new GameMapBounds("poolcore", 527.0D, 640.0D, 575.0D, 703.0D);
    public static final GameMapBounds GOOSECHAPEL =
            new GameMapBounds("goosechapel", -1026.0D, -825.0D, -751.0D, -597.0D);
    public static final GameMapBounds GOOSESHIP =
            new GameMapBounds("gooseship", -2485.0D, -2229.0D, -4.0D, 187.0D);
    public static final GameMapBounds POLUS =
            new GameMapBounds("polus", -905.0D, -821.0D, 779.0D, 909.0D);

    public static GameMapBounds at(double x, double z) {
        if (ANTIQUE.contains(x, z)) {
            return ANTIQUE;
        }
        if (POOLCORE.contains(x, z)) {
            return POOLCORE;
        }
        if (GOOSECHAPEL.contains(x, z)) {
            return GOOSECHAPEL;
        }
        if (GOOSESHIP.contains(x, z)) {
            return GOOSESHIP;
        }
        if (POLUS.contains(x, z)) {
            return POLUS;
        }
        return null;
    }

    public boolean contains(double x, double z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean usesCaveMode() {
        return "goosechapel".equals(id) || "gooseship".equals(id) || "polus".equals(id);
    }

    public double width() {
        return maxX - minX;
    }

    public double height() {
        return maxZ - minZ;
    }
}
