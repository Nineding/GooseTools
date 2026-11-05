package com.goosethings.tools.client.meetingvoice;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class MeetingVoice implements VoicechatPlugin {
    static volatile Config config = new Config();
    static volatile VoicechatClientApi api;
    static volatile UUID localId;
    private static boolean installed;
    private static Path configPath;
    private static Object world;
    private static String scope = "";



    public static boolean integrationEnabled() {
        var loader = net.fabricmc.loader.api.FabricLoader.getInstance();
        return loader.getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT
                && !loader.isModLoaded("meetingvoice");
    }

    public static void onClientInitialize() { bootstrap(); }
    public String getPluginId() { return "goosetools_meetingvoice"; }
    public void registerEvents(EventRegistration events) {
        if (!integrationEnabled()) return;
        events.registerEvent(ClientVoicechatInitializationEvent.class, event -> {
            api = event.getVoicechat();
            PlayerVoiceStateCache.clear();
            bootstrap();
        });
        events.registerEvent(ClientVoicechatConnectionEvent.class, event -> {
            api = event.getVoicechat();
            PlayerVoiceStateCache.clear();
            Diagnostics.info("Voice connected=" + event.isConnected());
        });
        // Group audio only: proximity/entity audio must never reveal isolated speakers.
        events.registerEvent(ClientReceiveSoundEvent.StaticSound.class, event -> {
            long token = PlayerVoiceStateCache.token();
            Diagnostics.incoming.incrementAndGet();
            UUID id = event.getId();
            if (id != null && !event.isCancelled() && allowed(event.getVoicechat())
                    && VoiceStatusProvider.audible(event.getVoicechat(), id)) {
                PlayerVoiceStateCache.feed(token, id, event.getRawAudio(), System.nanoTime(), config);
                Diagnostics.accepted.incrementAndGet();
            }
        }, Integer.MIN_VALUE);
        events.registerEvent(ClientSoundEvent.class, event -> {
            long token = PlayerVoiceStateCache.token();
            Diagnostics.microphone.incrementAndGet();
            VoicechatClientApi client = event.getVoicechat();
            if (config.showSelf && localId != null && !event.isCancelled() && allowed(client)
                    && !client.isMuted() && client.isTalking()) {
                PlayerVoiceStateCache.feed(token, localId, event.getRawAudio(), System.nanoTime(), config);
                Diagnostics.accepted.incrementAndGet();
            }
        }, Integer.MIN_VALUE);
    }

    private static synchronized void bootstrap() {
        if (installed || !integrationEnabled()) return;
        installed = true;
        try {
            Object loader = Reflect.call(Reflect.type("net.fabricmc.loader.api.FabricLoader"), "getInstance");
            Diagnostics.init((Path)Reflect.call(loader, "getGameDir"));
            configPath = ((Path)Reflect.call(loader, "getConfigDir")).resolve("meetingvoice.properties");
            reload();
            HeadRenderer.install();
            ClientControls.install();
            Diagnostics.info("MeetingVoice beta.9 D-plan: strict meeting phase + ggd_meeting group");
        } catch (Exception | LinkageError e) { fail("Initialization failed", e); }
    }

    // Called from client tick AND before rendering so transitions cannot leave an old bubble.
    static void refreshContext(Object mc) throws ReflectiveOperationException {
        Object level = Reflect.field(mc, "level");
        Object player = Reflect.field(mc, "player");
        UUID id = player == null ? null : (UUID)Reflect.call(player, "getUUID");
        String reason = blockReason(api);
        Group group = api == null ? null : api.getGroup();
        String next = reason + ":" + (group == null ? "none" : group.getId()) + ":" + id;
        if (level != world || !next.equals(scope)) {
            PlayerVoiceStateCache.clear();
            HeadRenderer.clearAnimations();
            ClientControls.previewUntil = 0;


            world = level;
            scope = next;
        }
        localId = id;
        if (level == null || id == null || !"ready".equals(reason)) {
            if (PlayerVoiceStateCache.size() > 0) PlayerVoiceStateCache.clear();
            return;
        }
        if (api.isMuted()) PlayerVoiceStateCache.remove(id);
    }

    static void reload() throws IOException {
        config = Config.load(configPath);
        PlayerVoiceStateCache.clear();
        HeadRenderer.clearAnimations();
        ClientControls.previewUntil = 0;

    }
    static void saveOption(String key, boolean value) throws IOException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(configPath)) { p.load(r); }
        p.setProperty(key, Boolean.toString(value));
        try (Writer w = Files.newBufferedWriter(configPath)) { p.store(w, "MeetingVoice D-plan; meetingOnly is always true"); }
        reload();
    }
    static boolean allowed(VoicechatClientApi client) { return "ready".equals(blockReason(client)); }
    static String blockReason(VoicechatClientApi client) { return VoiceStatusProvider.blockReason(client); }
    static String groupName() { Group group = api == null ? null : api.getGroup(); return group == null ? "none" : group.getName(); }
    static void fail(String message, Throwable error) { Diagnostics.fail(message, error); }
}
