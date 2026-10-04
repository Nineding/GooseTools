package com.goosethings.tools.client.nametag;

import com.goosethings.tools.marker.PlayerMarkerCatalog;
import com.goosethings.tools.client.vision.VisionFogState;
import com.goosethings.tools.client.vision.WitchDoctorTargetClient;
import com.goosethings.tools.client.vision.BirdwatcherClientState;
import com.goosethings.tools.client.vision.BirdwatcherWallTransparency;
import com.goosethings.tools.nametag.NameTagSync;
import com.goosethings.tools.network.GooseToolsPayloads;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Renders names and image attachments as one depth-tested entity-relative plate. */
public final class NameTagRenderer {
    private static final Identifier CURSE_EYE = texture("textures/item/curse_eye.png");
    private static final Identifier GRAVY = texture("textures/item/ggd/gravy.png");
    private static final Identifier BALLOON_ONE = texture("textures/item/single_balloon.png");
    private static final Identifier BALLOON_TWO = texture("textures/item/double_balloon.png");
    private static final Identifier PIGEON = texture("textures/item/ggd/pigeon.png");
    private static final Identifier LOVER = texture("textures/item/ggd/lover.png");
    private static final Identifier GUARD_SHIELD = texture("textures/item/guard_shield.png");
    private static final int FULL_BRIGHT = 0x00f000f0;
    private static final double MAX_DISTANCE_SQUARED = 96.0D * 96.0D;
    private static final float SCALE = 0.025F;
    private static final float GAP = 1.5F;
    private static final float ICON_HEIGHT = 10.0F;
    private static final float BADGE_SIZE = SerialBadgeStyle.SIZE;
    private static final float ICON_DEPTH = 0.01F;
    private static final float BADGE_NUMBER_DEPTH = 0.03F;
    private static final float MARK_BACKGROUND_DEPTH = -0.02F;
    private static final float SERIAL_WHITE_U = 8.5F / 32.0F;
    private static final float SERIAL_WHITE_V = 3.5F / 32.0F;
    private NameTagRenderer() {
    }

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(NameTagRenderer::renderWorld);
    }

    public static void renderInCamera(SubmitNodeCollector destination,
                                      PoseStack poses,
                                      Entity entity,
                                      double relativeX,
                                      double relativeY,
                                      double relativeZ,
                                      float boundingBoxHeight,
                                      Quaternionfc cameraRotation) {
        UUID sourceId = NameTagClientState.sourcePlayerId(entity);
        GooseToolsPayloads.NameTagEntry entry = NameTagClientState.entry(sourceId);
        if (entry == null || entry.name().isEmpty() || entity instanceof Mannequin mannequin
                && mannequin.getPose() == Pose.SLEEPING) {
            return;
        }
        emitPlate(destination, poses,
                new Vec3(relativeX, relativeY + boundingBoxHeight + 0.5D, relativeZ),
                cameraRotation, entity, sourceId, entry, 1.0F);
    }

    private static void renderWorld(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }
        Camera camera = client.gameRenderer.mainCamera();
        Vec3 cameraPosition = camera.position();
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        for (Entity entity : client.level.entitiesForRendering()) {
            UUID sourceId = NameTagClientState.sourcePlayerId(entity);
            GooseToolsPayloads.NameTagEntry entry = NameTagClientState.entry(sourceId);
            boolean localPlayer = entity == client.player
                    || (sourceId != null && sourceId.equals(client.player.getUUID()));
            if (entry == null || entry.name().isEmpty()) {
                continue;
            }
            if (entity instanceof Mannequin mannequin && mannequin.getPose() == Pose.SLEEPING) {
                continue;
            }
            if (entity.isRemoved() || NameTagRenderPolicy.shouldSkip(
                    client.options.getCameraType().isFirstPerson(),
                    entity == camera.entity(),
                    entity.isInvisibleTo(client.player),
                    client.player.isSpectator(),
                    localPlayer)) {
                continue;
            }
            Vec3 position = entity.getPosition(partialTick);
            if (position.distanceToSqr(cameraPosition) > MAX_DISTANCE_SQUARED) {
                continue;
            }
            float visibility = VisionFogState.nameTagVisibilityAlpha(entity);
            if (visibility <= 0.01F || !hasLineOfSight(client, cameraPosition, entity, position)) {
                continue;
            }
            emitPlate(context.submitNodeCollector(), context.poseStack(),
                    new Vec3(position.x - cameraPosition.x,
                            position.y - cameraPosition.y + entity.getBbHeight() + 0.5D,
                            position.z - cameraPosition.z),
                    camera.rotation(), entity, sourceId, entry, visibility);
        }
    }

    private static boolean hasLineOfSight(Minecraft client, Vec3 from,
                                          Entity entity, Vec3 position) {
        double height = entity.getBbHeight();
        return clearRay(client, from, new Vec3(position.x, position.y + height * 0.92D, position.z))
                || clearRay(client, from, new Vec3(position.x, position.y + height * 0.62D, position.z));
    }

    private static boolean clearRay(Minecraft client, Vec3 from, Vec3 to) {
        Vec3 direction = to.subtract(from);
        double length = direction.length();
        if (length <= 1.0E-6D) {
            return true;
        }
        Vec3 step = direction.scale(0.05D / length);
        Vec3 cursor = from;
        for (int ignoredBlocks = 0; ignoredBlocks < 64; ignoredBlocks++) {
            HitResult result = client.level.clip(new ClipContext(
                    cursor, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
            if (result.getType() == HitResult.Type.MISS) {
                return true;
            }
            if (!BirdwatcherClientState.isActive()
                    || !(result instanceof BlockHitResult blockHit)
                    || !BirdwatcherWallTransparency.isTransparent(blockHit.getBlockPos())) {
                return false;
            }

            // Continue the ray immediately beyond this render-hidden wall block. This
            // preserves collision in the real client world while matching the wall mesh
            // the Birdwatcher can actually see through.
            BlockPos ignored = blockHit.getBlockPos();
            cursor = blockHit.getLocation().add(step);
            int withinBlockSteps = 0;
            while (BlockPos.containing(cursor).equals(ignored) && withinBlockSteps++ < 32) {
                cursor = cursor.add(step);
            }
            if (cursor.distanceToSqr(to) <= step.lengthSqr()) {
                return true;
            }
        }
        return false;
    }

    private static void emitPlate(SubmitNodeCollector destination,
                                  PoseStack poses,
                                  Vec3 centre,
                                  Quaternionfc cameraRotation,
                                  Entity renderedEntity,
                                  UUID sourceId,
                                  GooseToolsPayloads.NameTagEntry entry,
                                  float alpha) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        int flags = entry.attachmentFlags();
        boolean lover = (flags & NameTagSync.LOVER) != 0;
        int nameRgb = lover ? 0xff55ff : entry.rgb();
        int alphaByte = Math.clamp(Math.round(alpha * 255.0F), 0, 255);
        int nameColour = alphaByte << 24 | nameRgb;
        int background = Math.clamp(Math.round(alpha * 64.0F), 0, 255) << 24;
        PlayerMarkerCatalog.Definition marker = entry.markerNameTagVisible()
                ? PlayerMarkerCatalog.byCode(entry.markerCode()) : null;
        String markerText = marker == null ? "" : Component.translatableWithFallback(
                marker.translationKey(), marker.fallback()).getString();
        Identifier markerTexture = marker == null ? null : Identifier.tryParse(marker.texture());

        List<Icon> icons = new ArrayList<>(6 + entry.attachments().size());
        if (NameTagRenderIconPolicy.showWitchDoctorCurse(
                WitchDoctorTargetClient.isTarget(entry.identityPlayerId()), flags)) {
            icons.add(new Icon(CURSE_EYE, ICON_HEIGHT, ICON_HEIGHT, 0xffffff));
        }
        if ((flags & NameTagSync.GRAVY) != 0) {
            icons.add(new Icon(GRAVY, ICON_HEIGHT, ICON_HEIGHT, 0xffffff));
        }
        if ((flags & NameTagSync.CLOWN_BALLOON_TWO) != 0) {
            icons.add(new Icon(BALLOON_TWO, 17.5F, ICON_HEIGHT, 0xffffff));
        } else if ((flags & NameTagSync.CLOWN_BALLOON_ONE) != 0) {
            icons.add(new Icon(BALLOON_ONE, ICON_HEIGHT, ICON_HEIGHT, 0xffffff));
        }
        if ((flags & NameTagSync.PIGEON_INFECTED) != 0) {
            icons.add(new Icon(PIGEON, ICON_HEIGHT, ICON_HEIGHT, 0xffffff));
        }
        if (lover) {
            icons.add(new Icon(LOVER, ICON_HEIGHT, ICON_HEIGHT, 0xffffff));
        }
        if ((flags & NameTagSync.GUARD_SHIELD) != 0) {
            icons.add(new Icon(GUARD_SHIELD, ICON_HEIGHT, ICON_HEIGHT, 0xffffff));
        }
        for (GooseToolsPayloads.NameTagIcon attachment : entry.attachments()) {
            Identifier texture = Identifier.tryParse(attachment.texture());
            if (texture != null) {
                icons.add(new Icon(
                        texture, attachment.width(), attachment.height(), attachment.rgb()));
            }
        }
        if (entry.serialNumber() > 0) {
            icons.add(new Icon(SerialBadgeStyle.TEXTURE, BADGE_SIZE, BADGE_SIZE, entry.rgb()));
        }

        float nameWidth = font.width(entry.name());
        float totalWidth = nameWidth;
        for (Icon icon : icons) {
            totalWidth += GAP + icon.width();
        }
        float markerTextWidth = marker == null ? 0.0F : font.width(markerText);
        float markerWidth = marker == null ? 0.0F
                : markerTextWidth + 4.0F
                + (markerTexture == null ? 0.0F : GAP + ICON_HEIGHT);
        if (marker != null) {
            totalWidth += GAP + markerWidth;
        }
        float cursor = -totalWidth * 0.5F;

        poses.pushPose();
        poses.translate(centre.x, centre.y, centre.z);
        poses.mulPose(new Matrix4f().rotate(cameraRotation));
        poses.scale(SCALE, -SCALE, SCALE);
        // Attachments are left of the name. SERIAL is appended last above, so it is
        // always the attachment nearest the nametag as requested.
        for (Icon icon : icons) {
            float top = -icon.height() * 0.5F;
            emitIcon(destination, poses, icon.texture(), cursor, top,
                    icon.width(), icon.height(), alphaByte << 24 | icon.rgb());
            if (icon.texture().equals(SerialBadgeStyle.TEXTURE)) {
                int contrast = alphaByte << 24 | SerialBadgeStyle.contrastColour(icon.rgb());
                emitBadgeNumber(destination, poses, cursor, entry.serialNumber(), contrast);
            }
            cursor += icon.width() + GAP;
        }
        destination.submitText(poses, cursor, -4.5F,
                Component.literal(entry.name()).getVisualOrderText(), false,
                Font.DisplayMode.NORMAL, FULL_BRIGHT, nameColour, background, 0);
        cursor += nameWidth;
        if (marker != null) {
            cursor += GAP;
            int markerAlpha = Math.clamp(Math.round(alpha * 190.0F), 0, 255);
            emitSolidRect(destination, poses, cursor, -6.0F, markerWidth, 12.0F,
                    markerAlpha << 24 | marker.style().cardRgb());
            int markerTextColour = alphaByte << 24
                    | SerialBadgeStyle.contrastColour(marker.style().cardRgb());
            float markerContentX = cursor + 2.0F;
            if (markerTexture != null) {
                emitIcon(destination, poses, markerTexture,
                        markerContentX, -ICON_HEIGHT * 0.5F,
                        ICON_HEIGHT, ICON_HEIGHT, alphaByte << 24 | 0xffffff);
                markerContentX += ICON_HEIGHT + GAP;
            }
            destination.submitText(poses, markerContentX, -4.5F,
                    Component.literal(markerText).getVisualOrderText(), false,
                    Font.DisplayMode.NORMAL, FULL_BRIGHT, markerTextColour, 0, 0);
        }
        poses.popPose();
    }

    private static void emitSolidRect(SubmitNodeCollector destination,
                                      PoseStack poses,
                                      float x,
                                      float y,
                                      float width,
                                      float height,
                                      int colour) {
        float right = x + width;
        float bottom = y + height;
        destination.submitCustomGeometry(poses, RenderTypes.text(SerialBadgeStyle.TEXTURE), (pose, out) -> {
            Matrix4fc matrix = pose.pose();
            out.addVertex(matrix, x, bottom, MARK_BACKGROUND_DEPTH).setColor(colour)
                    .setUv(SERIAL_WHITE_U, SERIAL_WHITE_V).setLight(FULL_BRIGHT);
            out.addVertex(matrix, right, bottom, MARK_BACKGROUND_DEPTH).setColor(colour)
                    .setUv(SERIAL_WHITE_U, SERIAL_WHITE_V).setLight(FULL_BRIGHT);
            out.addVertex(matrix, right, y, MARK_BACKGROUND_DEPTH).setColor(colour)
                    .setUv(SERIAL_WHITE_U, SERIAL_WHITE_V).setLight(FULL_BRIGHT);
            out.addVertex(matrix, x, y, MARK_BACKGROUND_DEPTH).setColor(colour)
                    .setUv(SERIAL_WHITE_U, SERIAL_WHITE_V).setLight(FULL_BRIGHT);
        });
    }

    /** Draws a compact 3x5 pixel number in the same render batch as the badge. */
    private static void emitBadgeNumber(SubmitNodeCollector destination,
                                        PoseStack poses,
                                        float badgeX,
                                        int number,
                                        int colour) {
        SerialBadgeStyle.forEachDigitPixel(number, badgeX, -BADGE_SIZE * 0.5F,
                (x, y, size) -> emitSolidPixel(destination, poses, x, y, size, colour));
    }

    private static void emitSolidPixel(SubmitNodeCollector destination,
                                       PoseStack poses,
                                       float x,
                                       float y,
                                       float size,
                                       int colour) {
        float right = x + size;
        float bottom = y + size;
        destination.submitCustomGeometry(poses, RenderTypes.text(SerialBadgeStyle.TEXTURE), (pose, out) -> {
            Matrix4fc matrix = pose.pose();
            out.addVertex(matrix, x, bottom, BADGE_NUMBER_DEPTH).setColor(colour)
                    .setUv(SERIAL_WHITE_U, SERIAL_WHITE_V).setLight(FULL_BRIGHT);
            out.addVertex(matrix, right, bottom, BADGE_NUMBER_DEPTH).setColor(colour)
                    .setUv(SERIAL_WHITE_U, SERIAL_WHITE_V).setLight(FULL_BRIGHT);
            out.addVertex(matrix, right, y, BADGE_NUMBER_DEPTH).setColor(colour)
                    .setUv(SERIAL_WHITE_U, SERIAL_WHITE_V).setLight(FULL_BRIGHT);
            out.addVertex(matrix, x, y, BADGE_NUMBER_DEPTH).setColor(colour)
                    .setUv(SERIAL_WHITE_U, SERIAL_WHITE_V).setLight(FULL_BRIGHT);
        });
    }

    private static void emitIcon(SubmitNodeCollector destination,
                                 PoseStack poses,
                                 Identifier texture,
                                 float x,
                                 float y,
                                 float width,
                                 float height,
                                 int colour) {
        float right = x + width;
        float bottom = y + height;
        destination.submitCustomGeometry(poses, RenderTypes.text(texture), (pose, out) -> {
            Matrix4fc matrix = pose.pose();
            out.addVertex(matrix, x, bottom, ICON_DEPTH).setColor(colour).setUv(0, 1).setLight(FULL_BRIGHT);
            out.addVertex(matrix, right, bottom, ICON_DEPTH).setColor(colour).setUv(1, 1).setLight(FULL_BRIGHT);
            out.addVertex(matrix, right, y, ICON_DEPTH).setColor(colour).setUv(1, 0).setLight(FULL_BRIGHT);
            out.addVertex(matrix, x, y, ICON_DEPTH).setColor(colour).setUv(0, 0).setLight(FULL_BRIGHT);
        });
    }

    private static Identifier texture(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    private record Icon(Identifier texture, float width, float height, int rgb) {
    }
}
