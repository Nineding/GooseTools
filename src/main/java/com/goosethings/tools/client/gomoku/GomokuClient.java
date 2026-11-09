package com.goosethings.tools.client.gomoku;

import com.goosethings.tools.gomoku.GomokuPackets;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.Map;
import java.util.WeakHashMap;

/** A cached view survives screen removal, but never survives a connection. */
public final class GomokuClient {
    private static GomokuPackets.View view;
    private static long receivedAt, sequence, requests, pendingOpen, noticeUntil;
    private static int notice;
    private static KeyMapping key;
    private static final Map<Screen, Button> launchers = new WeakHashMap<>();
    private GomokuClient() {}
    public static Component text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("gomoku.goosetools." + key, fallback, args);
    }
    public static long now() { return System.nanoTime() / 1_000_000; }
    public static GomokuPackets.View view() { return view; }
    public static int remaining() { return view == null ? 0 : (int) Math.max(0, view.position().remaining() - (now() - receivedAt)); }
    public static int inviteRemaining() { return view == null ? 0 : (int) Math.max(0, view.inviteRemaining() - (now() - receivedAt)); }
    public static int notice() { return now() < noticeUntil ? notice : 0; }
    public static void register() {
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.goosetools.gomoku", InputConstants.Type.KEYBOARD,
                InputConstants.KEY_F9, KeyMapping.Category.register(Identifier.fromNamespaceAndPath("goosetools", "games"))));
        ClientPlayNetworking.registerGlobalReceiver(GomokuPackets.View.TYPE, (packet, ctx) -> ctx.client().execute(() -> apply(packet)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            view = null; receivedAt = sequence = requests = pendingOpen = noticeUntil = 0; launchers.clear();
            if (client.gui.screen() instanceof GomokuScreen) client.setScreenAndShow(null);
        }));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            launchers.remove(screen); launcher(screen);
            if (screen instanceof InventoryScreen || screen instanceof PauseScreen)
                ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
                    if (matchesKey(event)) { open(); return false; } return true;
                });
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.consumeClick()) {
                Screen screen = client.gui.screen();
                if (screen instanceof GomokuScreen g) g.onClose();
                else if (screen == null || screen instanceof InventoryScreen || screen instanceof PauseScreen) open();
            }
            if (!presentationAllowed()) {
                pendingOpen = 0;
                if (client.gui.screen() instanceof GomokuScreen) client.setScreenAndShow(null);
            }
            launcher(client.gui.screen());
        });
    }
    private static boolean presentationAllowed() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        Screen screen = mc.gui.screen();
        if (screen instanceof com.goosethings.tools.client.hud.RoleIntroScreen
                || screen instanceof com.goosethings.tools.client.hud.MatchResultScreen
                || screen instanceof com.goosethings.tools.client.hud.PostgameSummaryScreen
                || screen instanceof com.goosethings.tools.client.hud.PresentationBlackoutScreen) return false;
        String phase = com.goosethings.tools.client.presence.GamePresenceClient.phaseId();
        return !phase.equals("preparing") && !phase.equals("results") && !phase.equals("tutorial") && !phase.equals("playing");
    }
    private static void launcher(Screen screen) {
        if (!(screen instanceof InventoryScreen) && !(screen instanceof PauseScreen)) return;
        Button button = launchers.get(screen);
        if (button == null) {
            button = Button.builder(text("title", "Gomoku"), b -> open())
                    .bounds(Math.max(8, screen.width - 112), 8, 104, 20).build();
            Screens.getWidgets(screen).add(button); launchers.put(screen, button);
        }
        button.visible = view != null && view.group() != 0 && presentationAllowed();
        button.active = button.visible;
    }
    public static void open() {
        if (!presentationAllowed() || !ClientPlayNetworking.canSend(GomokuPackets.Action.TYPE)) return;
        pendingOpen = ++requests;
        send(new GomokuPackets.Action(GomokuPackets.OPEN, pendingOpen, 0, 0, sequence++, 0, 0, GomokuPackets.NONE));
    }
    public static void action(int action, int value, int option, java.util.UUID target) {
        if (view == null) return;
        long id = action == GomokuPackets.ACCEPT || action == GomokuPackets.DECLINE || action == GomokuPackets.CANCEL_INVITE ? view.invite() : view.position().id();
        send(new GomokuPackets.Action(action, 0, id, view.position().revision(), sequence++, value, option, target));
    }
    public static void action(int action) { action(action, 0, 0, GomokuPackets.NONE); }
    public static boolean matchesKey(net.minecraft.client.input.KeyEvent event) { return key != null && key.matches(event); }
    public static void hide() { pendingOpen = 0; action(GomokuPackets.HIDE); }
    private static void send(GomokuPackets.Action action) { if (ClientPlayNetworking.canSend(GomokuPackets.Action.TYPE)) ClientPlayNetworking.send(action); }
    private static void apply(GomokuPackets.View next) {
        if (view != null && next.serial() <= view.serial()) return;
        Minecraft mc = Minecraft.getInstance();
        GomokuPackets.View old = view; view = next; receivedAt = now();
        if (next.notice() != 0) { notice = next.notice(); noticeUntil = now() + 4000; }
        if (next.group() == 0 || next.notice() == 6) {
            pendingOpen = 0;
            if (next.notice() == 1 && mc.player != null)
                mc.player.sendSystemMessage(text("unavailable", "Available in the lobby or after elimination only."));
            if (mc.gui.screen() instanceof GomokuScreen) mc.setScreenAndShow(null);
            return;
        }
        if (next.inviteDirection() == 2 && (old == null || next.invite() != old.invite()))
            mc.gui.hud.getChat().addClientSystemMessage(text("invited", "%s invited you to Gomoku. Open /gomoku to respond (30 seconds).", next.inviteName()));
        if (old != null && old.position().id() == next.position().id() && old.position().outcome() == 0 && next.position().outcome() != 0)
            mc.gui.hud.getChat().addClientSystemMessage(text("finished_notice", "Your Gomoku game has ended. Open /gomoku to see the result."));
        if (old != null && next.position().id() != 0 && next.position().id() != old.position().id() && !(mc.gui.screen() instanceof GomokuScreen))
            mc.gui.hud.getChat().addClientSystemMessage(text("started_notice", "Your Gomoku game has started. Each move has 60 seconds; open /gomoku to play."));
        if ((next.request() == -1 || next.request() > 0 && next.request() == pendingOpen) && presentationAllowed()) {
            pendingOpen = 0; mc.setScreenAndShow(new GomokuScreen());
        }
        if (mc.gui.screen() instanceof GomokuScreen screen) screen.updated(old, next);
    }
}
