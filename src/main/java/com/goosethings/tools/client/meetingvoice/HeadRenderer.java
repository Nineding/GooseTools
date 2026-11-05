package com.goosethings.tools.client.meetingvoice;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class HeadRenderer {
    private static boolean failed, goose, registered;
    private static Object normal;
    private static Class<?> mcType, playerType, gooseNames, gooseVision;
    private static final Map<UUID, VoiceBubbleAnimator> animators = new LinkedHashMap<>();
    private static long generation = -1;
    static void clearAnimations() { animators.clear(); }

    static void install() throws ReflectiveOperationException {
        if (registered) return;
        mcType = Reflect.type("net.minecraft.client.Minecraft");
        playerType = Reflect.type("net.minecraft.world.entity.player.Player");
        normal = Reflect.field(Reflect.type("net.minecraft.client.gui.Font$DisplayMode"), "NORMAL");
        Object loader = Reflect.call(Reflect.type("net.fabricmc.loader.api.FabricLoader"), "getInstance");
        goose = (Boolean)Reflect.call(loader, "isModLoaded", "goosetools");
        if (goose) {
            gooseNames = Reflect.type("com.goosethings.tools.client.nametag.NameTagClientState");
            gooseVision = Reflect.type("com.goosethings.tools.client.vision.VisionFogState");
        }
        String events = "net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents";
        Class<?> callback = Reflect.type(events + "$CollectSubmits");
        Object listener = Proxy.newProxyInstance(callback.getClassLoader(), new Class<?>[]{callback}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (method.getName().equals("equals")) return proxy == args[0];
                return "MeetingVoiceRenderer";
            }
            Diagnostics.renders++;
            Diagnostics.lastRender = System.nanoTime();
            if (!failed) {
                try { render(args[0]); }
                catch (Exception | LinkageError error) {
                    failed = true;
                    Diagnostics.renderState = "render-error";
                    PlayerVoiceStateCache.clear();
                    MeetingVoice.fail("Render compatibility error; use /meetingvoice reload after checking logs", error);
                }
            }
            return null;
        });
        Reflect.call(Reflect.field(Reflect.type(events), "COLLECT_SUBMITS"), "register", listener);
        registered = true;
        Diagnostics.renderState = "registered";
    }

    static void retry() throws ReflectiveOperationException {
        install();
        failed = false;
        Diagnostics.error = "none";
    }

    private static void render(Object context) throws ReflectiveOperationException {
        Object mc = Reflect.call(mcType, "getInstance");
        MeetingVoice.refreshContext(mc);
        long epoch = PlayerVoiceStateCache.token();
        if (epoch != generation) { animators.clear(); generation = epoch; }
        Object level = Reflect.field(mc, "level"), player = Reflect.field(mc, "player");
        if (level == null || player == null) { Diagnostics.renderState = "no-world"; return; }
        // Intentionally before preview: no command or legacy setting can bypass the meeting gate.
        if (!VoiceStatusProvider.inMeeting() || MeetingVoice.api == null) {
            animators.clear();
            Diagnostics.renderState = MeetingVoice.blockReason(MeetingVoice.api);
            return;
        }
        long now = System.nanoTime();
        Config config = MeetingVoice.config;
        boolean preview = MeetingVoice.allowed(MeetingVoice.api) && now < ClientControls.previewUntil;
        PlayerVoiceStateCache.prune(now);
        var ids = PlayerVoiceStateCache.ids();
        if (preview) ids.add(MeetingVoice.localId);
        ids.addAll(animators.keySet());
        Object options = Reflect.field(mc, "options");
        if ((Boolean)Reflect.call(Reflect.field(Reflect.field(mc, "gui"), "hud"), "isHidden")) {
            animators.clear(); Diagnostics.renderState = "hud-hidden-F1"; return;
        }
        Object camera = Reflect.call(Reflect.field(mc, "gameRenderer"), "mainCamera");
        Object cameraPosition = Reflect.call(camera, "position"), cameraEntity = Reflect.call(camera, "entity");
        boolean firstPerson = (Boolean)Reflect.call(Reflect.call(options, "getCameraType"), "isFirstPerson");
        float partialTick = ((Number)Reflect.call(Reflect.call(mc, "getDeltaTracker"), "getGameTimeDeltaPartialTick", true)).floatValue();
        Map<UUID, Object> bodies = new LinkedHashMap<>();
        Diagnostics.renderState = "no-audio-or-visible-body";
        for (Object entity : (Iterable<?>)Reflect.call(level, "entitiesForRendering")) {
            boolean proxy = goose && isMeetingProxy(entity);
            if (!playerType.isInstance(entity) && !proxy) continue;
            UUID id = (UUID)(goose ? Reflect.call(gooseNames, "sourcePlayerId", entity) : Reflect.call(entity, "getUUID"));
            if (id != null && (ids.contains(id) || VoiceStatusProvider.showDisconnected(id, id.equals(MeetingVoice.localId))) && (!goose || !suppressed(entity))
                    && (proxy || !bodies.containsKey(id))) bodies.put(id, entity);
        }
        animators.keySet().retainAll(bodies.keySet());
        Object collector = Reflect.call(context, "submitNodeCollector"), poses = Reflect.call(context, "poseStack");
        Object rotation = Reflect.call(Reflect.type("org.joml.Matrix4f").getConstructor().newInstance(), "rotate", Reflect.call(camera, "rotation"));
        for (Map.Entry<UUID, Object> body : bodies.entrySet()) {
            UUID id = body.getKey(); Object entity = body.getValue();
            boolean self = id.equals(MeetingVoice.localId);
            boolean disconnected = VoiceStatusProvider.showDisconnected(id, self);
            if (!disconnected && !VoiceStatusProvider.canPerceive(id, self)) {
                PlayerVoiceStateCache.remove(id); animators.remove(id); continue;
            }
            if ((Boolean)Reflect.call(entity, "isRemoved") || (Boolean)Reflect.call(entity, "isInvisibleTo", player)
                    || playerType.isInstance(entity) && (Boolean)Reflect.call(entity, "isSpectator")) {
                animators.remove(id); continue;
            }
            if (self && !config.showSelf && !preview) { animators.remove(id); Diagnostics.renderState = "self-hidden-config"; continue; }
            if (firstPerson && (entity == cameraEntity || self)) { animators.remove(id); Diagnostics.renderState = "self-first-person-use-F5"; continue; }
            boolean localMuted = self && MeetingVoice.api.isMuted();
            if (disconnected) PlayerVoiceStateCache.remove(id);
            Meter.Sample sample = disconnected || localMuted ? new Meter.Sample(0, 0)
                : preview && self ? new Meter.Sample(0.5 + 0.45 * Math.sin(now / 2.0E8), now) : PlayerVoiceStateCache.sample(id, now);
            // Mute has no icon. A confirmed disconnection takes precedence over mute.
            if (localMuted && !disconnected) { animators.remove(id); continue; }
            double visibility = goose ? ((Number)Reflect.call(gooseVision, "nameTagVisibilityAlpha", entity)).doubleValue() : 1;
            if (visibility <= 0.01) { animators.remove(id); Diagnostics.renderState = "goose-visibility"; continue; }
            Object position = Reflect.call(entity, "getPosition", partialTick);
            double x = Reflect.coord(position, "x") - Reflect.coord(cameraPosition, "x");
            double y = Reflect.coord(position, "y") - Reflect.coord(cameraPosition, "y");
            double z = Reflect.coord(position, "z") - Reflect.coord(cameraPosition, "z");
            double distance = Math.sqrt(x*x + y*y + z*z);
            if (distance >= config.range) { animators.remove(id); Diagnostics.renderState = "out-of-range"; continue; }
            double height = ((Number)Reflect.call(entity, "getBbHeight")).doubleValue();
            if (!clearRay(level, player, cameraPosition, Reflect.vec(Reflect.coord(position, "x"),
                    Reflect.coord(position, "y") + height * 0.9, Reflect.coord(position, "z")))) {
                animators.remove(id); Diagnostics.renderState = "wall-occluded"; continue;
            }
            VoiceBubbleAnimator.Frame frame = animators.computeIfAbsent(id, ignored -> new VoiceBubbleAnimator())
                .update(sample, disconnected, now, config);
            if (frame.opacity() <= 0.005) {
                if (frame.state() == VoiceBubbleAnimator.State.IDLE) animators.remove(id);
                continue;
            }
            double fade = Math.min(1, (config.range - distance) / (config.range * 0.2));
            int alpha = (int)(255 * Math.min(1, visibility) * frame.opacity() * fade);
            float scale = (float)(0.025 * config.bubbleScale * frame.scale());
            Reflect.call(poses, "pushPose");
            try {
                Reflect.call(poses, "translate", x, y + height + 0.5, z);
                Reflect.call(poses, "mulPose", rotation);
                // Fixed tail anchor above the highest visible nameplate attachment.
                Reflect.call(poses, "translate", 0.0, 0.025 * (BubbleAnchor.top(entity, goose) + config.bubbleGap), 0.0);
                Reflect.call(poses, "scale", scale, -scale, scale);
                BubbleRenderer.submit(collector, poses, frame, alpha);
            } finally { Reflect.call(poses, "popPose"); }
            Diagnostics.renderState = disconnected ? "disconnected-drawing" : preview && self ? "meeting-test-drawing" : "bubble-drawing";
        }
    }
    private static boolean isMeetingProxy(Object entity) throws ReflectiveOperationException {
        return (Boolean)Reflect.call(Reflect.type("com.goosethings.tools.client.dream.DreamStandInClient"), "isMeetingProxy", entity)
            || (Boolean)Reflect.call(Reflect.type("com.goosethings.tools.client.projection.ProjectionBodyClient"), "isDreamMeetingBody", entity);
    }

    private static boolean suppressed(Object entity) throws ReflectiveOperationException {
        // Dream map bodies carry real source UUIDs even when their appearance is anonymous.
        // Only the stand-in seated in the meeting may inherit a speaker's bubble.
        if ("com.goosethings.tools.client.dream.DreamRemotePlayer".equals(entity.getClass().getName())
                && !isMeetingProxy(entity)) return true;
        Class<?> dream = Reflect.type("com.goosethings.tools.client.dream.DreamStandInClient");
        Class<?> projection = Reflect.type("com.goosethings.tools.client.projection.ProjectionBodyClient");
        if ((Boolean)Reflect.call(gooseVision, "shouldHideEntityVisual", entity)
            || (Boolean)Reflect.call(dream, "shouldSuppressStandIn", entity)
            || (Boolean)Reflect.call(dream, "shouldSuppressMeetingSource", entity)
            || (Boolean)Reflect.call(projection, "shouldSuppressBody", entity)
            || (Boolean)Reflect.call(projection, "shouldSuppressSource", entity)
            || (Boolean)Reflect.call(Reflect.type("com.goosethings.tools.client.animation.PlayerArmAnimationContinuity"), "shouldSuppressRender", entity)) return true;
        Class<?> mime = Reflect.type("com.goosethings.tools.client.mime.MimeControlClient");
        return (Boolean)Reflect.call(mime, "isControlled") && Objects.equals(Reflect.call(mime, "controllerId"), Reflect.call(entity, "getUUID"));
    }

    private static boolean clearRay(Object level, Object player, Object from, Object to) throws ReflectiveOperationException {
        Class<?> vec = Reflect.type("net.minecraft.world.phys.Vec3");
        Class<?> block = Reflect.type("net.minecraft.world.level.ClipContext$Block");
        Class<?> fluid = Reflect.type("net.minecraft.world.level.ClipContext$Fluid");
        Object clip = Reflect.type("net.minecraft.world.level.ClipContext")
            .getConstructor(vec, vec, block, fluid, Reflect.type("net.minecraft.world.entity.Entity"))
            .newInstance(from, to, Reflect.field(block, "COLLIDER"), Reflect.field(fluid, "NONE"), player);
        return Reflect.call(Reflect.call(level, "clip", clip), "getType") == Reflect.field(Reflect.type("net.minecraft.world.phys.HitResult$Type"), "MISS");
    }
}
