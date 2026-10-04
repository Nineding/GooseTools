package com.goosethings.tools.xaero;

import com.google.gson.JsonArray;
import com.goosethings.tools.client.ClientTaskMarkers;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Display;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.element.render.MinimapElementReader;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderer;
import xaero.lib.client.graphics.XaeroBufferProvider;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class GgdMapElementRenderer
        extends MinimapElementRenderer<GgdMapElementRenderer.Element, GgdMapElementRenderer.Context> {
    private static final Context WORLD_MAP_OVERLAY_CONTEXT = new Context();

    public GgdMapElementRenderer() {
        this(new Reader(), new Provider(), new Context());
    }

    private GgdMapElementRenderer(Reader reader, Provider provider, Context context) {
        super(reader, provider, context);
    }

    @Override
    public int getOrder() {
        return 150;
    }

    @Override
    public void preRender(MinimapElementRenderInfo info,
                          XaeroBufferProvider buffer,
                          MultiTextureRenderTypeRendererProvider multiTextureProvider) {
        buffer.endBatch();
    }

    @Override
    public boolean renderElement(Element element, boolean highlighted, boolean outOfBounds,
                                 double depth, float optionalScale, double partialX, double partialY,
                                 MinimapElementRenderInfo info, MinimapElementGraphics graphics,
                                 XaeroBufferProvider buffer) {
        if (outOfBounds) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        float textScale = info.location == MinimapElementRenderLocation.WORLD_MAP ? 2.5F : 2.0F;
        if (info.location != MinimapElementRenderLocation.IN_WORLD) {
            textScale *= GgdClientPreferences.mapMarkerScale();
        }
        graphics.pose().pushPose();
        graphics.pose().scale(textScale, textScale, 1.0F);
        try {
        if (element.kind() == ElementKind.TASK) {
            var config = ClientTaskMarkers.current();
            var task = config.task(element.textKey());
            Component taskName = Component.translatableWithFallback(task.translationKey(), task.fallback());
            int width = minecraft.font.width(taskName);
            int background = config.background(element.textKey(),
                    element.goldTask() ? "gold" : element.emergencyTask() ? "emergency" : "normal");

            graphics.fill(-width / 2 - 3, -6, width / 2 + 4, 6, 0xD0171717);
            graphics.fill(-width / 2 - 2, -5, width / 2 + 3, 5, background);
            graphics.drawCenteredString(minecraft.font, taskName, 0, -4, 0xFFFFFFFF);
            return true;
        }

        if (element.kind() == ElementKind.LAST_POSITION) {
            Component marker = Component.translatable("marker.ggd_xaero_map.last_position");
            int width = minecraft.font.width(marker);
            graphics.fill(-width / 2 - 3, -6, width / 2 + 4, 6, 0xD0171717);
            graphics.drawCenteredString(minecraft.font, marker, 0, -4, 0xFFFFD54A);
            return true;
        }

        if (element.kind() == ElementKind.REPORTED_BODY) {
            Component marker = Component.translatable("marker.ggd_xaero_map.reported_body");
            int width = minecraft.font.width(marker);
            graphics.fill(-width / 2 - 3, -6, width / 2 + 4, 6, 0xD0171717);
            graphics.drawCenteredString(minecraft.font, marker, 0, -4, 0xFFFF3030);
            return true;
        }

        if (element.kind() == ElementKind.BELL) {
            drawBellIcon(graphics);
            return true;
        }

        if (element.kind() == ElementKind.BROADCAST) {
            drawInterphoneIcon(graphics);
            return true;
        }

        Component name = Component.translatable(element.textKey());
        int width = minecraft.font.width(name);
        graphics.fill(-width / 2 - 2, -5, width / 2 + 3, 5, 0x8C111111);
        graphics.drawCenteredString(minecraft.font, name, 0, -4,
                element.activeRoom() ? 0xFFFFD54A : 0xFFFFFFFF);
        return true;
        } finally {
            graphics.pose().popPose();
        }
    }

    @Override
    public void postRender(MinimapElementRenderInfo info,
                           XaeroBufferProvider buffer,
                           MultiTextureRenderTypeRendererProvider multiTextureProvider) {
        buffer.endBatch();
    }

    @Override
    public boolean shouldRender(MinimapElementRenderLocation location) {
        return location == MinimapElementRenderLocation.OVER_MINIMAP
                || location == MinimapElementRenderLocation.IN_WORLD;
    }

    public static void renderWorldMapOverlay(
            GuiGraphicsExtractor graphics,
            double cameraX,
            double cameraZ,
            double mapScale,
            int clipLeft,
            int clipTop,
            int clipRight,
            int clipBottom) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow().getWidth() <= 0
                || minecraft.getWindow().getHeight() <= 0
                || mapScale <= 0.0D
                || clipLeft >= clipRight
                || clipTop >= clipBottom) {
            return;
        }

        WORLD_MAP_OVERLAY_CONTEXT.rebuild(MinimapElementRenderLocation.WORLD_MAP);
        if (WORLD_MAP_OVERLAY_CONTEXT.frameElements.isEmpty()) {
            return;
        }

        double toGuiX = (double) graphics.guiWidth() / minecraft.getWindow().getWidth();
        double toGuiY = (double) graphics.guiHeight() / minecraft.getWindow().getHeight();
        float markerScale = WorldMapOverlayProjection.markerScale(
                GgdClientPreferences.mapMarkerScale(), toGuiX, toGuiY);

        graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        try {
            for (Element element : WORLD_MAP_OVERLAY_CONTEXT.frameElements) {
                int screenX = WorldMapOverlayProjection.coordinate(
                        element.x(), cameraX, mapScale, toGuiX, graphics.guiWidth());
                int screenY = WorldMapOverlayProjection.coordinate(
                        element.z(), cameraZ, mapScale, toGuiY, graphics.guiHeight());

                graphics.pose().pushMatrix();
                graphics.pose().translate(screenX, screenY);
                graphics.pose().scale(markerScale, markerScale);
                try {
                    renderWorldMapElement(graphics, element);
                } finally {
                    graphics.pose().popMatrix();
                }
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private static void renderWorldMapElement(GuiGraphicsExtractor graphics, Element element) {
        Minecraft minecraft = Minecraft.getInstance();
        if (element.kind() == ElementKind.TASK) {
            var config = ClientTaskMarkers.current();
            var task = config.task(element.textKey());
            Component taskName = Component.translatableWithFallback(task.translationKey(), task.fallback());
            int width = minecraft.font.width(taskName);
            int background = config.background(element.textKey(),
                    element.goldTask() ? "gold" : element.emergencyTask() ? "emergency" : "normal");

            graphics.fill(-width / 2 - 3, -6, width / 2 + 4, 6, 0xD0171717);
            graphics.fill(-width / 2 - 2, -5, width / 2 + 3, 5, background);
            graphics.centeredText(minecraft.font, taskName, 0, -4, 0xFFFFFFFF);
            return;
        }

        if (element.kind() == ElementKind.LAST_POSITION) {
            Component marker = Component.translatable("marker.ggd_xaero_map.last_position");
            int width = minecraft.font.width(marker);
            graphics.fill(-width / 2 - 3, -6, width / 2 + 4, 6, 0xD0171717);
            graphics.centeredText(minecraft.font, marker, 0, -4, 0xFFFFD54A);
            return;
        }

        if (element.kind() == ElementKind.REPORTED_BODY) {
            Component marker = Component.translatable("marker.ggd_xaero_map.reported_body");
            int width = minecraft.font.width(marker);
            graphics.fill(-width / 2 - 3, -6, width / 2 + 4, 6, 0xD0171717);
            graphics.centeredText(minecraft.font, marker, 0, -4, 0xFFFF3030);
            return;
        }

        if (element.kind() == ElementKind.BELL) {
            drawAtlasIcon(graphics, "item/bell");
            return;
        }

        if (element.kind() == ElementKind.BROADCAST) {
            drawAtlasIcon(graphics, "item/interphone");
            return;
        }

        Component name = Component.translatable(element.textKey());
        int width = minecraft.font.width(name);
        graphics.fill(-width / 2 - 2, -5, width / 2 + 3, 5, 0x8C111111);
        graphics.centeredText(minecraft.font, name, 0, -4,
                element.activeRoom() ? 0xFFFFD54A : 0xFFFFFFFF);
    }

    private static boolean isEmergencyTask(String kind) {
        return "emergency".equalsIgnoreCase(kind);
    }


    enum ElementKind {
        ROOM,
        TASK,
        LAST_POSITION,
        REPORTED_BODY,
        BELL,
        BROADCAST
    }

    private static boolean isBellMarker(String id) {
        return id.equals("talker") || id.startsWith("talker_");
    }

    private static boolean isBroadcastMarker(String id) {
        return "broadcast_station".equals(id);
    }

    private static void drawInterphoneIcon(MinimapElementGraphics graphics) {
        drawAtlasIcon(graphics, "item/interphone");
    }

    private static void drawBellIcon(MinimapElementGraphics graphics) {
        drawAtlasIcon(graphics, "item/bell");
    }

    private static void drawAtlasIcon(MinimapElementGraphics graphics, String spritePath) {
        var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS);
        TextureAtlasSprite sprite = atlas.getSprite(Identifier.withDefaultNamespace(spritePath));
        graphics.blit(sprite, -8, -8, 16, 16, RenderPipelines.GUI_TEXTURED);
    }

    private static void drawAtlasIcon(GuiGraphicsExtractor graphics, String spritePath) {
        var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS);
        TextureAtlasSprite sprite = atlas.getSprite(Identifier.withDefaultNamespace(spritePath));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, -8, -8, 16, 16);
    }

    record Element(String textKey, double x, double y, double z,
                   ElementKind kind, boolean activeRoom, boolean goldTask,
                   boolean emergencyTask) {
        static Element room(Room room, boolean active) {
            return new Element(room.key(), room.x(), 64.0D, room.z(), ElementKind.ROOM, active, false, false);
        }

        static Element task(String id, double x, double y, double z, boolean gold, boolean emergency) {
            return new Element(id, x, y, z, ElementKind.TASK, false, gold, emergency);
        }

        static Element special(ElementKind kind, double x, double y, double z) {
            return new Element("", x, y, z, kind, false, false, false);
        }
    }


    record Room(String key, double x, double z, List<Area> areas, List<Area> excludedAreas,
                boolean advancedOnly, boolean basicOnly, String stackGroup) {
        double distanceSquared(double otherX, double otherZ) {
            double deltaX = x - otherX;
            double deltaZ = z - otherZ;
            return deltaX * deltaX + deltaZ * deltaZ;
        }

        boolean contains(double otherX, double otherY, double otherZ) {
            return areas.stream().anyMatch(area -> area.contains(otherX, otherY, otherZ))
                    && excludedAreas.stream().noneMatch(area -> area.contains(otherX, otherY, otherZ));
        }

        boolean containsY(double otherY) {
            return areas.stream().anyMatch(area -> otherY >= area.minY() && otherY <= area.maxY());
        }

        double minY() {
            double min = Double.POSITIVE_INFINITY;
            for (Area area : areas) {
                min = Math.min(min, area.minY());
            }
            return min;
        }

        double maxY() {
            double max = Double.NEGATIVE_INFINITY;
            for (Area area : areas) {
                max = Math.max(max, area.maxY());
            }
            return max;
        }

        boolean visibleFor(boolean advanced) {
            return (!advancedOnly || advanced) && (!basicOnly || !advanced);
        }

        boolean stacked() {
            return !stackGroup.isEmpty();
        }
    }

    record Area(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        boolean contains(double x, double y, double z) {
            return x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }
    }

    static final class Context {
        private final List<Layout> layouts = loadLayouts();
        private List<Element> frameElements = List.of();
        private Iterator<Element> iterator = List.<Element>of().iterator();

        private void rebuild(MinimapElementRenderLocation location) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null
                    || minecraft.level == null
                    || !GgdMapState.isGameActive(minecraft)) {
                frameElements = List.of();
                return;
            }

            boolean inWorld = location == MinimapElementRenderLocation.IN_WORLD;
            if (inWorld && (!GgdMapState.taskPathsVisible() || GgdMapState.isMeetingView(minecraft))) {
                frameElements = List.of();
                return;
            }

            var mapPosition = GgdMapState.effectiveMapPosition(minecraft);
            if (mapPosition == null) {
                frameElements = List.of();
                return;
            }
            Layout active = layouts.stream()
                    .filter(layout -> layout.contains(mapPosition.x, mapPosition.z))
                    .findFirst()
                    .orElse(null);
            if (active == null) {
                frameElements = List.of();
                return;
            }

            List<Element> result = new ArrayList<>();
            boolean advanced = !"poolcore".equals(active.id())
                    || GgdMapState.isPoolcoreAdvanced(minecraft);
            if (!inWorld) {
                List<Room> shownRooms = active.roomsForLayer(
                        mapPosition.x, mapPosition.y, mapPosition.z, advanced);
                Room currentRoom = active.currentRoom(
                        mapPosition.x, mapPosition.y, mapPosition.z, shownRooms);
                for (Room room : shownRooms) {
                    result.add(Element.room(room, room == currentRoom));
                }

                addSpecialMarker(result, active,
                        GgdMapState.findMarker(minecraft, GgdMapState.MEETING_LAST_POSITION),
                        ElementKind.LAST_POSITION);
                addSpecialMarker(result, active,
                        GgdMapState.findMarker(minecraft, GgdMapState.REPORTED_BODY),
                        ElementKind.REPORTED_BODY);
            }

            for (var entity : minecraft.level.entitiesForRendering()) {
                if (!(entity instanceof Display.BlockDisplay) || entity.getCustomName() == null) {
                    continue;
                }
                String encoded = entity.getCustomName().getString();
                if (!encoded.startsWith(GgdMapState.MARKER_PREFIX)) {
                    continue;
                }
                String[] parts = encoded.split(":", 3);
                if (parts.length < 2) {
                    continue;
                }
                String id = parts[1];
                if (GgdMapState.MEETING_LAST_POSITION.equals(id)
                        || GgdMapState.REPORTED_BODY.equals(id)
                        || GgdMapState.isControlMarker(id)) {
                    continue;
                }
                if (isBellMarker(id)) {
                    if (!inWorld && active.contains(entity.getX(), entity.getZ())) {
                        result.add(Element.special(ElementKind.BELL,
                                entity.getX(), entity.getY(), entity.getZ()));
                    }
                    continue;
                }
                if (isBroadcastMarker(id)) {
                    if (!inWorld && active.contains(entity.getX(), entity.getZ())) {
                        result.add(Element.special(ElementKind.BROADCAST,
                                entity.getX(), entity.getY(), entity.getZ()));
                    }
                    continue;
                }
                if (active.contains(entity.getX(), entity.getZ())) {
                    String kind = parts.length == 3 ? parts[2] : "normal";
                    result.add(Element.task(id, entity.getX(), entity.getY(), entity.getZ(),
                            "gold".equalsIgnoreCase(kind), isEmergencyTask(kind)));

                }
            }
            frameElements = result;
        }

        private static void addSpecialMarker(
                List<Element> result, Layout active, GgdMapState.Marker marker, ElementKind kind) {
            if (marker != null && active.contains(marker.position().x, marker.position().z)) {
                result.add(Element.special(kind,
                        marker.position().x, marker.position().y, marker.position().z));
            }
        }
    }

    record Layout(String id, double minX, double maxX, double minZ, double maxZ, List<Room> rooms) {
        boolean contains(double x, double z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        List<Room> roomsForLayer(double x, double y, double z, boolean advanced) {
            List<Room> visible = new ArrayList<>();
            for (Room room : rooms) {
                if (room.visibleFor(advanced)) {
                    visible.add(room);
                }
            }

            List<Room> shown = new ArrayList<>();
            for (Room room : visible) {
                if (!room.stacked()) {
                    shown.add(room);
                    continue;
                }
                if (hasStackGroup(shown, room.stackGroup())) {
                    continue;
                }
                shown.add(pickStackedRoom(visible, room.stackGroup(), x, y, z));
            }
            return shown;
        }

        Room currentRoom(double x, double y, double z, List<Room> candidates) {
            for (Room room : candidates) {
                if (room.contains(x, y, z)) {
                    return room;
                }
            }

            Room closest = null;
            double closestDistance = Double.MAX_VALUE;
            for (Room room : candidates) {
                double distance = room.distanceSquared(x, z);
                if (distance < closestDistance) {
                    closest = room;
                    closestDistance = distance;
                }
            }
            return closest;
        }

        private static boolean hasStackGroup(List<Room> rooms, String group) {
            for (Room room : rooms) {
                if (group.equals(room.stackGroup())) {
                    return true;
                }
            }
            return false;
        }

        private static Room pickStackedRoom(List<Room> visible, String group,
                                            double x, double y, double z) {
            List<Room> stacked = new ArrayList<>();
            for (Room room : visible) {
                if (group.equals(room.stackGroup())) {
                    stacked.add(room);
                }
            }
            if (stacked.size() == 1) {
                return stacked.get(0);
            }

            for (Room room : stacked) {
                if (room.contains(x, y, z)) {
                    return room;
                }
            }
            for (Room room : stacked) {
                if (room.containsY(y)) {
                    return room;
                }
            }

            Room chosen = stacked.get(0);
            if (y >= 144.0D) {
                for (Room room : stacked) {
                    if (room.minY() > chosen.minY()) {
                        chosen = room;
                    }
                }
                return chosen;
            }
            for (Room room : stacked) {
                if (room.maxY() < chosen.maxY()) {
                    chosen = room;
                }
            }
            return chosen;
        }
    }

    private static List<Layout> loadLayouts() {
        try (var stream = GgdMapElementRenderer.class.getResourceAsStream(
                "/assets/goosetools/layouts.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing layouts.json");
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Layout> result = new ArrayList<>();
            for (JsonElement layoutElement : root.getAsJsonArray("layouts")) {
                JsonObject layout = layoutElement.getAsJsonObject();
                List<Room> rooms = new ArrayList<>();
                JsonArray roomArray = layout.getAsJsonArray("rooms");
                for (JsonElement roomElement : roomArray) {
                    JsonObject room = roomElement.getAsJsonObject();
                    List<Area> areas = new ArrayList<>();
                    if (room.has("areas")) {
                        for (JsonElement areaElement : room.getAsJsonArray("areas")) {
                            JsonObject area = areaElement.getAsJsonObject();
                            areas.add(new Area(
                                    area.get("minX").getAsDouble(), area.get("maxX").getAsDouble(),
                                    area.has("minY") ? area.get("minY").getAsDouble() : Double.NEGATIVE_INFINITY,
                                    area.has("maxY") ? area.get("maxY").getAsDouble() : Double.POSITIVE_INFINITY,
                                    area.get("minZ").getAsDouble(), area.get("maxZ").getAsDouble()));
                        }
                    }
                    List<Area> excludedAreas = new ArrayList<>();
                    if (room.has("excludeAreas")) {
                        for (JsonElement areaElement : room.getAsJsonArray("excludeAreas")) {
                            JsonObject area = areaElement.getAsJsonObject();
                            excludedAreas.add(new Area(
                                    area.get("minX").getAsDouble(), area.get("maxX").getAsDouble(),
                                    area.has("minY") ? area.get("minY").getAsDouble() : Double.NEGATIVE_INFINITY,
                                    area.has("maxY") ? area.get("maxY").getAsDouble() : Double.POSITIVE_INFINITY,
                                    area.get("minZ").getAsDouble(), area.get("maxZ").getAsDouble()));
                        }
                    }
                    rooms.add(new Room(
                            room.get("key").getAsString(),
                            room.get("x").getAsDouble(),
                            room.get("z").getAsDouble(),
                            List.copyOf(areas),
                            List.copyOf(excludedAreas),
                            room.has("advancedOnly") && room.get("advancedOnly").getAsBoolean(),
                            room.has("basicOnly") && room.get("basicOnly").getAsBoolean(),
                            room.has("stackGroup") ? room.get("stackGroup").getAsString() : ""));
                }
                result.add(new Layout(
                        layout.get("id").getAsString(),
                        layout.get("minX").getAsDouble(),
                        layout.get("maxX").getAsDouble(),
                        layout.get("minZ").getAsDouble(),
                        layout.get("maxZ").getAsDouble(),
                        List.copyOf(rooms)));
            }
            return List.copyOf(result);
        } catch (Exception exception) {
            GgdXaeroMapClient.LOGGER.error("Failed to load Goose Goose Duck minimap layouts", exception);
            return List.of();
        }
    }

    private static final class Provider extends MinimapElementRenderProvider<Element, Context> {
        @Override
        public void begin(MinimapElementRenderLocation location, Context context) {
            context.rebuild(location);
            context.iterator = context.frameElements.iterator();
        }

        @Override
        public boolean hasNext(MinimapElementRenderLocation location, Context context) {
            return context.iterator.hasNext();
        }

        @Override
        public Element getNext(MinimapElementRenderLocation location, Context context) {
            return context.iterator.next();
        }

        @Override
        public void end(MinimapElementRenderLocation location, Context context) {
            context.iterator = List.<Element>of().iterator();
        }
    }

    private static final class Reader extends MinimapElementReader<Element, Context> {
        @Override public boolean isHidden(Element element, Context context) { return false; }
        @Override public double getRenderX(Element element, Context context, float partialTicks) { return element.x(); }
        @Override public double getRenderY(Element element, Context context, float partialTicks) { return element.y(); }
        @Override public double getRenderZ(Element element, Context context, float partialTicks) { return element.z(); }
        @Override public int getInteractionBoxLeft(Element element, Context context, float partialTicks) { return -32; }
        @Override public int getInteractionBoxRight(Element element, Context context, float partialTicks) { return 32; }
        @Override public int getInteractionBoxTop(Element element, Context context, float partialTicks) { return -8; }
        @Override public int getInteractionBoxBottom(Element element, Context context, float partialTicks) { return 12; }
        @Override public int getRenderBoxLeft(Element element, Context context, float partialTicks) { return -160; }
        @Override public int getRenderBoxRight(Element element, Context context, float partialTicks) { return 160; }
        @Override public int getRenderBoxTop(Element element, Context context, float partialTicks) { return -32; }
        @Override public int getRenderBoxBottom(Element element, Context context, float partialTicks) { return 48; }
        @Override public int getLeftSideLength(Element element, Minecraft minecraft) { return 0; }
        @Override public String getMenuName(Element element) { return ""; }
        @Override public String getFilterName(Element element) { return ""; }
        @Override public int getMenuTextFillLeftPadding(Element element) { return 0; }
        @Override public int getRightClickTitleBackgroundColor(Element element) { return 0; }
        @Override public boolean shouldScaleBoxWithOptionalScale() { return false; }
        @Override public boolean isInteractable(MinimapElementRenderLocation location, Element element) { return false; }
    }
}
