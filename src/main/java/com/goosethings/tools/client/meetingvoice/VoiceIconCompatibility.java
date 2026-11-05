package com.goosethings.tools.client.meetingvoice;

import java.lang.reflect.Method;
import java.util.UUID;

/** The voice mod's convenience method is not part of its public plugin API. */
final class VoiceIconCompatibility {
    private interface Check { boolean allowed(UUID id) throws ReflectiveOperationException; }
    private static Check check;
    private static boolean initialized;
    private static boolean reported;
    static String mode = "pending";

    static boolean allowed(UUID id) {
        try {
            if (!initialized) {
                check = resolve();
                initialized = true;
                Diagnostics.info("Voice icon compatibility: " + mode);
            }
            return check.allowed(id);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            // A failed optional compatibility hook must not latch the entire renderer off.
            // Fail closed for this player: never bypass another plugin's visibility veto.
            mode = "unavailable";
            if (!reported) {
                reported = true;
                Diagnostics.fail("Voice icon visibility hook unavailable", error);
            }
            return false;
        }
    }

    private static Check resolve() throws ReflectiveOperationException {
        try {
            Class<?> manager = Class.forName("de.maxhenkel.voicechat.plugins.ClientPluginManager");
            Method method = manager.getMethod("shouldRenderPlayerIcons", UUID.class);
            Object instance = manager.getMethod("instance").invoke(null);
            mode = "direct";
            return id -> (Boolean) method.invoke(instance, id);
        } catch (NoSuchMethodException | ClassNotFoundException missing) {
            // Equivalent to the current convenience method: dispatch the cancellable
            // NameTagIconRenderEvent. Preserve the voice plugins' hide-icon decisions.
            Class<?> eventType = Class.forName("de.maxhenkel.voicechat.api.events.NameTagIconRenderEvent");
            Class<?> eventBase = Class.forName("de.maxhenkel.voicechat.api.events.Event");
            Class<?> impl = Class.forName("de.maxhenkel.voicechat.plugins.impl.events.NameTagIconRenderEventImpl");
            var constructor = impl.getConstructor();
            Method setId = impl.getMethod("setEntityId", UUID.class);
            Method setCancelled = impl.getMethod("setCancelled", boolean.class);
            Class<?> manager = Class.forName("de.maxhenkel.voicechat.plugins.PluginManager");
            Object instance = manager.getMethod("instance").invoke(null);
            Method dispatch = manager.getMethod("dispatchEvent", Class.class, eventBase);
            mode = "event-fallback";
            return id -> {
                Object event = constructor.newInstance();
                setId.invoke(event, id);
                setCancelled.invoke(event, false);
                return !((Boolean) dispatch.invoke(instance, eventType, event));
            };
        }
    }
}
