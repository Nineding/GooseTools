package com.goosethings.tools.client.vision;

import com.goosethings.tools.GooseTools;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Highlights only the first collision boundary reachable on the player's current Y layer. */
public final class BlackoutWallGuide {
    private static final int RADIUS = 12;
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(
            GooseTools.MOD_ID, "blackout_wall_guide");
    private static List<GuideSegment> cached = List.of();
    private static int ticks;

    private BlackoutWallGuide() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!BlackoutAssistClient.isWallGuideActive() || client.level == null || client.player == null) {
                clear();
                return;
            }
            if ((ticks++ & 1) == 0) {
                rebuild(client);
            }
        });
        // Draw immediately before the crosshair as a GUI overlay. World points are projected
        // with the current camera every frame, avoiding near-cylinder vertex rescaling and
        // avoiding Iris world-line shader paths.
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CROSSHAIR,
                HUD_ID,
                (graphics, delta) -> render(graphics));
    }

    public static void clear() {
        cached = List.of();
        ticks = 0;
    }

    private static void rebuild(Minecraft client) {
        BlockPos origin = client.player.blockPosition();
        int y = origin.getY();
        var walls = BlackoutWallGuideMath.boundary(
                new BlackoutWallGuideMath.Cell(origin.getX(), origin.getZ()), RADIUS,
                cell -> isBlocked(client, new BlockPos(cell.x(), y, cell.z())));

        List<GuideSegment> segments = new ArrayList<>(walls.size() * 24);
        for (BlackoutWallGuideMath.Cell cell : walls) {
            BlockPos wall = new BlockPos(cell.x(), y, cell.z());
            addSegments(client, segments, wall, origin);
            addSegments(client, segments, wall.above(), origin);
        }
        cached = List.copyOf(segments);
    }

    private static boolean isBlocked(Minecraft client, BlockPos foot) {
        return !client.level.getBlockState(foot).getCollisionShape(client.level, foot).isEmpty()
                || !client.level.getBlockState(foot.above()).getCollisionShape(client.level, foot.above()).isEmpty();
    }

    private static void addSegments(
            Minecraft client, List<GuideSegment> output, BlockPos pos, BlockPos origin) {
        VoxelShape shape = client.level.getBlockState(pos).getCollisionShape(client.level, pos);
        if (shape.isEmpty()) {
            return;
        }
        double distance = Math.sqrt(origin.distSqr(pos));
        shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> output.add(new GuideSegment(
                pos.getX() + minX,
                pos.getY() + minY,
                pos.getZ() + minZ,
                pos.getX() + maxX,
                pos.getY() + maxY,
                pos.getZ() + maxZ,
                distance)));
    }

    private static void render(GuiGraphicsExtractor graphics) {
        if (!BlackoutAssistClient.isWallGuideActive() || cached.isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Camera camera = client.gameRenderer.mainCamera();
        if (!camera.isInitialized() || client.player == null || client.level == null) {
            return;
        }

        Matrix4f viewProjection = camera.getViewRotationProjectionMatrix(new Matrix4f());
        double cameraX = camera.position().x();
        double cameraY = camera.position().y();
        double cameraZ = camera.position().z();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        for (GuideSegment guide : cached) {
            ScreenSpaceProjection.Point start = ScreenSpaceProjection.project(
                    viewProjection,
                    guide.startX() - cameraX,
                    guide.startY() - cameraY,
                    guide.startZ() - cameraZ,
                    width,
                    height);
            ScreenSpaceProjection.Point end = ScreenSpaceProjection.project(
                    viewProjection,
                    guide.endX() - cameraX,
                    guide.endY() - cameraY,
                    guide.endZ() - cameraZ,
                    width,
                    height);
            if (start == null || end == null) {
                continue;
            }
            ScreenSpaceProjection.Line clipped = ScreenSpaceProjection.clip(
                    start.x(), start.y(), end.x(), end.y(), 0.0D, 0.0D, width, height);
            if (clipped == null) {
                continue;
            }
            int alpha = Math.clamp((int) Math.round(235.0D - guide.distance() * 16.0D), 80, 235);
            drawLine(graphics, clipped, (alpha << 24) | 0x42D9FF);
        }
    }

    private static void drawLine(
            GuiGraphicsExtractor graphics, ScreenSpaceProjection.Line line, int color) {
        float deltaX = (float) (line.endX() - line.startX());
        float deltaY = (float) (line.endY() - line.startY());
        float length = (float) Math.hypot(deltaX, deltaY);
        if (length < 0.5F) {
            return;
        }
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float) line.startX(), (float) line.startY());
        pose.rotate((float) Math.atan2(deltaY, deltaX));
        graphics.fill(0, -1, Math.max(1, Math.round(length)), 1, color);
        pose.popMatrix();
    }

    private record GuideSegment(
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ,
            double distance) {
    }
}
