package com.goosethings.tools.client.voice;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.client.dream.DreamAvatarClient;
import com.goosethings.tools.client.presence.GamePresenceClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Reads audible speech for the managed plate, including bodies aliased to a player UUID. */
public final class GgdSpeakingIcon {
    private static final Identifier SPEAKER = Identifier.fromNamespaceAndPath(
            "voicechat", "textures/icons/speaker.png");
    private static final Identifier WHISPER = Identifier.fromNamespaceAndPath(
            "voicechat", "textures/icons/speaker_whisper.png");
    private static final AtomicBoolean LOGGED_FAILURE = new AtomicBoolean();
    private static volatile SvcAccess access;

    private GgdSpeakingIcon() {
    }

    public static Identifier texture(UUID sourcePlayerId) {
        // Meeting audio stays audible, but must not identify the frozen dream bodies.
        if (DreamAvatarClient.active()) {
            return null;
        }
        String phase = GamePresenceClient.phaseId();
        if (sourcePlayerId == null || !("lobby".equals(phase) || "meeting".equals(phase))
                || !FabricLoader.getInstance().isModLoaded("voicechat")) {
            return null;
        }
        SvcAccess svc = access();
        if (svc == null) {
            return null;
        }
        try {
            Object config = svc.clientConfig.get(null);
            if (Boolean.TRUE.equals(svc.configValue.invoke(svc.hideIcons.get(config)))
                    || !Boolean.TRUE.equals(svc.configValue.invoke(svc.showNametagIcons.get(config)))) {
                return null;
            }
            Object client = svc.getClient.invoke(null);
            if (client == null) {
                return null;
            }
            Object connection = svc.getConnection.invoke(client);
            if (connection == null || !Boolean.TRUE.equals(svc.isInitialized.invoke(connection))) {
                return null;
            }
            Object plugins = svc.pluginManager.invoke(null);
            if (!Boolean.TRUE.equals(svc.shouldRenderPlayerIcons.invoke(plugins, sourcePlayerId))) {
                return null;
            }
            Object cache = svc.getTalkCache.invoke(client);
            if (Boolean.TRUE.equals(svc.isWhispering.invoke(cache, sourcePlayerId))) {
                return WHISPER;
            }
            return Boolean.TRUE.equals(svc.isTalking.invoke(cache, sourcePlayerId)) ? SPEAKER : null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            logFailure(exception);
            return null;
        }
    }

    private static SvcAccess access() {
        if (LOGGED_FAILURE.get()) {
            return null;
        }
        SvcAccess existing = access;
        if (existing != null) {
            return existing;
        }
        synchronized (GgdSpeakingIcon.class) {
            if (access == null && !LOGGED_FAILURE.get()) {
                try {
                    access = SvcAccess.resolve();
                } catch (ReflectiveOperationException | LinkageError exception) {
                    logFailure(exception);
                }
            }
            return access;
        }
    }

    private static void logFailure(Throwable exception) {
        if (LOGGED_FAILURE.compareAndSet(false, true)) {
            GooseTools.LOGGER.warn("Could not read Simple Voice Chat speaking state", exception);
        }
    }

    private record SvcAccess(
            Field clientConfig, Field hideIcons, Field showNametagIcons, Method configValue,
            Method getClient, Method getConnection, Method isInitialized, Method getTalkCache,
            Method isTalking, Method isWhispering, Method pluginManager, Method shouldRenderPlayerIcons) {
        static SvcAccess resolve() throws ReflectiveOperationException {
            Class<?> voicechat = Class.forName("de.maxhenkel.voicechat.VoicechatClient");
            Class<?> config = Class.forName("de.maxhenkel.voicechat.config.ClientConfig");
            Class<?> entry = Class.forName("de.maxhenkel.voicechat.configbuilder.entry.ConfigEntry");
            Class<?> manager = Class.forName("de.maxhenkel.voicechat.voice.client.ClientManager");
            Class<?> client = Class.forName("de.maxhenkel.voicechat.voice.client.ClientVoicechat");
            Class<?> connection = Class.forName("de.maxhenkel.voicechat.voice.client.ClientVoicechatConnection");
            Class<?> cache = Class.forName("de.maxhenkel.voicechat.voice.client.TalkCache");
            Class<?> plugins = Class.forName("de.maxhenkel.voicechat.plugins.ClientPluginManager");
            return new SvcAccess(
                    voicechat.getField("CLIENT_CONFIG"), config.getField("hideIcons"),
                    config.getField("showNametagIcons"), entry.getMethod("get"),
                    manager.getMethod("getClient"), client.getMethod("getConnection"),
                    connection.getMethod("isInitialized"), client.getMethod("getTalkCache"),
                    cache.getMethod("isTalking", UUID.class), cache.getMethod("isWhispering", UUID.class),
                    plugins.getMethod("instance"), plugins.getMethod("shouldRenderPlayerIcons", UUID.class));
        }
    }
}
