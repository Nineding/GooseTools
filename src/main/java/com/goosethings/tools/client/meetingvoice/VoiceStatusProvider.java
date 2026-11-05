package com.goosethings.tools.client.meetingvoice;

import java.util.UUID;
import de.maxhenkel.voicechat.api.*;

import com.goosethings.tools.client.presence.GamePresenceClient;

/** Reads server-synchronized game phase and the installed voice client's state. */
final class VoiceStatusProvider {
    static boolean inMeeting() {
        // The server's phase remains meeting while this client's camera is in a dream.
        if (com.goosethings.tools.client.dream.DreamAvatarClient.active()) return false;
        return MeetingVoice.config.enabled && "meeting".equals(phase());
    }

    // Unlike isDisconnected(UUID), getState does NOT turn unknown/fake players into
    // disconnected players based on the voice mod's showFakePlayersDisconnected setting.
    static boolean showDisconnected(UUID id, boolean self) {
        VoicechatClientApi api = MeetingVoice.api;
        if (id == null || !inMeeting() || api == null) return false;
        if (self) return api.isDisconnected();
        // A disconnected observer cannot reliably present other people's connection state.
        if (!"ready".equals(blockReason(api))) return false;
        Object states;
        try { states = Reflect.call(Reflect.type("de.maxhenkel.voicechat.voice.client.ClientManager"), "getPlayerStateManager"); }
        catch (ReflectiveOperationException | LinkageError e) { return false; }
        if (states == null) return false;
        Object state;
        try { state = Reflect.call(states, "getState", id); }
        catch (ReflectiveOperationException | LinkageError e) { return false; }
        if (state == null) return false;
        try { if (!Boolean.TRUE.equals(Reflect.call(state,"isDisconnected"))) return false; }
        catch (ReflectiveOperationException | LinkageError e) { return false; }
        Group own = api.getGroup();
        UUID peerGroup;
        try { peerGroup = (UUID) Reflect.call(state,"getGroup"); }
        catch (ReflectiveOperationException | LinkageError e) { return false; }
        // Losing the voice connection may also remove group membership. That absence
        // is allowed only with an explicit disconnected state; a different group is not.
        if (own == null || peerGroup != null && !own.getId().equals(peerGroup)) return false;
        return VoiceIconCompatibility.allowed(id);
    }
    /** Newer SVC volume API is optional; a failed check never exposes muted audio. */
    static boolean audible(VoicechatClientApi client, UUID id) {
        try {
            Object volumes = Reflect.call(client, "getVolumeConfig");
            return ((Number)Reflect.call(volumes, "getVolume", id)).doubleValue() > 0;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) { return false; }
    }
    static String phase() {
        try { return GamePresenceClient.phaseId(); }
        catch (LinkageError e) { return "unknown"; }
    }

    static String blockReason(VoicechatClientApi api) {
        // A changed reason makes refreshContext discard audio, animations and previews.
        if (com.goosethings.tools.client.dream.DreamAvatarClient.active()) return "outside-meeting-view-dream";
        if (!MeetingVoice.config.enabled) return "config-disabled";
        if (!"meeting".equals(phase())) return "outside-meeting-phase";
        if (api == null) return "waiting-for-voice-api";
        if (api.isDisconnected()) return "voice-disconnected";
        if (api.isDisabled()) return "voice-disabled";
        Group group = api.getGroup();
        return group != null && "ggd_meeting".equals(group.getName()) ? "ready" : "outside-meeting-group";
    }

    // Called on the render/client thread. Missing peer state is UNAVAILABLE, not muted.
    static boolean canPerceive(UUID id, boolean self) {
        VoicechatClientApi api = MeetingVoice.api;
        if (id == null || !"ready".equals(blockReason(api))) return false;
        if (self) return !api.isDisconnected() && !api.isDisabled();
        if (api.isDisconnected(id) || api.isDisabled(id) || !audible(api, id)) return false;
        Group own = api.getGroup();
        UUID peer;
        try {
            Object states = Reflect.call(Reflect.type("de.maxhenkel.voicechat.voice.client.ClientManager"), "getPlayerStateManager");
            if (states == null) return false;
            peer = (UUID) Reflect.call(states, "getGroup", id);
        } catch (ReflectiveOperationException | LinkageError e) { return false; }
        if (own == null || !own.getId().equals(peer)) return false;
        return VoiceIconCompatibility.allowed(id);
    }
}
