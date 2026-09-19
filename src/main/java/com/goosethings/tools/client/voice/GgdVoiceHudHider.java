package com.goosethings.tools.client.voice;

import com.goosethings.tools.GooseTools;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Detects GooseThings meeting voice group membership through Simple Voice Chat
 * client state. Entity tags are not synced to dedicated-server clients.
 */
public final class GgdVoiceHudHider {
    public static final String MEETING_GROUP_NAME = "ggd_meeting";
    static final Identifier GROUP_ICON = Identifier.fromNamespaceAndPath("voicechat", "icons/group");

    private static final AtomicBoolean LOGGED_FAILURE = new AtomicBoolean();
    private static volatile SvcAccess access;

    private GgdVoiceHudHider() {
    }

    public static boolean shouldHideMeetingGroupHud() {
        return MEETING_GROUP_NAME.equals(localGroupName());
    }

    public static boolean isMeetingGroupNametagIcon(Identifier texture, UUID playerId) {
        return GROUP_ICON.equals(texture) && MEETING_GROUP_NAME.equals(groupNameOf(playerId));
    }

    static String localGroupName() {
        SvcAccess svc = access();
        if (svc == null) {
            return null;
        }
        try {
            Object playerStates = svc.getPlayerStateManager.invoke(null);
            if (playerStates == null) {
                return null;
            }
            Object groupId = svc.getGroupID.invoke(playerStates);
            return groupName(svc, groupId);
        } catch (ReflectiveOperationException exception) {
            logFailure(exception);
            return null;
        }
    }

    static String groupNameOf(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        SvcAccess svc = access();
        if (svc == null) {
            return null;
        }
        try {
            Object playerStates = svc.getPlayerStateManager.invoke(null);
            if (playerStates == null) {
                return null;
            }
            Object groupId = null;
            if (svc.getGroupOfPlayer != null) {
                groupId = svc.getGroupOfPlayer.invoke(playerStates, playerId);
            }
            if (!(groupId instanceof UUID) && svc.getState != null && svc.playerStateGetGroup != null) {
                Object state = svc.getState.invoke(playerStates, playerId);
                groupId = state == null ? null : svc.playerStateGetGroup.invoke(state);
            }
            return groupName(svc, groupId);
        } catch (ReflectiveOperationException exception) {
            logFailure(exception);
            return null;
        }
    }

    private static String groupName(SvcAccess svc, Object groupId) throws ReflectiveOperationException {
        if (!(groupId instanceof UUID uuid)) {
            return null;
        }
        Object groupManager = svc.getGroupManager.invoke(null);
        if (groupManager == null) {
            return null;
        }
        Object group = svc.getClientGroup.invoke(groupManager, uuid);
        if (group == null) {
            return null;
        }
        Object name = svc.getName.invoke(group);
        return name instanceof String text ? text : null;
    }

    private static SvcAccess access() {
        SvcAccess existing = access;
        if (existing != null || LOGGED_FAILURE.get()) {
            return existing;
        }
        synchronized (GgdVoiceHudHider.class) {
            if (access != null || LOGGED_FAILURE.get()) {
                return access;
            }
            try {
                access = SvcAccess.resolve();
            } catch (ReflectiveOperationException exception) {
                logFailure(exception);
            }
            return access;
        }
    }

    private static void logFailure(Exception exception) {
        if (LOGGED_FAILURE.compareAndSet(false, true)) {
            GooseTools.LOGGER.warn("Could not read Simple Voice Chat group state", exception);
        }
    }

    private record SvcAccess(
            Method getPlayerStateManager,
            Method getGroupManager,
            Method getGroupID,
            Method getGroupOfPlayer,
            Method getState,
            Method playerStateGetGroup,
            Method getClientGroup,
            Method getName) {
        static SvcAccess resolve() throws ReflectiveOperationException {
            Class<?> clientManager = Class.forName("de.maxhenkel.voicechat.voice.client.ClientManager");
            Class<?> playerStates = Class.forName(
                    "de.maxhenkel.voicechat.voice.client.ClientPlayerStateManager");
            Class<?> groupManager = Class.forName(
                    "de.maxhenkel.voicechat.voice.client.ClientGroupManager");
            Class<?> clientGroup = Class.forName("de.maxhenkel.voicechat.voice.common.ClientGroup");
            Method getGroupOfPlayer = findMethod(playerStates, "getGroup", UUID.class);
            Method getState = findMethod(playerStates, "getState", UUID.class);
            Method playerStateGetGroup = null;
            if (getState != null) {
                playerStateGetGroup = Class.forName("de.maxhenkel.voicechat.voice.common.PlayerState")
                        .getMethod("getGroup");
            }
            if (getGroupOfPlayer == null && getState == null) {
                throw new NoSuchMethodException("ClientPlayerStateManager has no group lookup");
            }
            return new SvcAccess(
                    clientManager.getMethod("getPlayerStateManager"),
                    clientManager.getMethod("getGroupManager"),
                    playerStates.getMethod("getGroupID"),
                    getGroupOfPlayer,
                    getState,
                    playerStateGetGroup,
                    groupManager.getMethod("getGroup", UUID.class),
                    clientGroup.getMethod("getName"));
        }

        private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
            try {
                return type.getMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                return null;
            }
        }
    }
}
