package com.goosethings.tools.camera;

/** The centre lies slightly outside the chosen wall face. Facing is its outward normal. */
public record ScreenDefinition(String id, String cameraId, String dimension, double x, double y,
                               double z, String facing, float width, float height) {
    public ScreenDefinition {
        CameraLimits.id(id);
        CameraLimits.id(cameraId);
        CameraLimits.dimension(dimension);
        CameraLimits.position(x, y, z);
        CameraLimits.size(width, height);
        if (!java.util.Set.of("north", "south", "east", "west").contains(facing))
            throw new IllegalArgumentException("A vertical wall is required");
    }

    public ScreenDefinition resize(float width, float height) {
        return new ScreenDefinition(id, cameraId, dimension, x, y, z, facing, width, height);
    }

    /** WorldEdit-style inclusive block selection, projected onto the outward wall face. */
    public static ScreenDefinition corners(String id, String cameraId, String dimension,
            double x1, double y1, double z1, double x2, double y2, double z2, String facing) {
        CameraLimits.position(x1,y1,z1); CameraLimits.position(x2,y2,z2);
        x1 = Math.floor(x1); y1 = Math.floor(y1); z1 = Math.floor(z1);
        x2 = Math.floor(x2); y2 = Math.floor(y2); z2 = Math.floor(z2);
        boolean alongX = facing.equals("north") || facing.equals("south");
        if (!java.util.Set.of("north","south","east","west").contains(facing)
                || Math.abs(alongX ? z2-z1 : x2-x1) > .000001)
            throw new IllegalArgumentException("Corners must share a vertical plane matching the facing");
        double centreX = alongX ? (x1+x2+1)/2 : x1 + (facing.equals("east") ? 1.003 : -.003);
        double centreZ = alongX ? z1 + (facing.equals("south") ? 1.003 : -.003) : (z1+z2+1)/2;
        return new ScreenDefinition(id,cameraId,dimension,centreX,(y1+y2+1)/2,centreZ,
                facing,(float)Math.abs(alongX ? x2-x1 : z2-z1)+1,(float)Math.abs(y2-y1)+1);
    }

    public ScreenDefinition bind(String cameraId) {
        return new ScreenDefinition(id, cameraId, dimension, x, y, z, facing, width, height);
    }

    public boolean withinReach(double px, double py, double pz) {
        // Distance to the rectangle, not its centre: large screens remain watchable at their edges.
        double horizontal = (facing.equals("north") || facing.equals("south")) ? px - x : pz - z;
        double normal = (facing.equals("north") || facing.equals("south")) ? pz - z : px - x;
        double dx = Math.max(0, Math.abs(horizontal) - width / 2);
        double dy = Math.max(0, Math.abs(py - y) - height / 2);
        return dx * dx + dy * dy + normal * normal <= CameraLimits.WATCH_DISTANCE * CameraLimits.WATCH_DISTANCE;
    }
}
