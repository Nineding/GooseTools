package com.goosethings.tools.client.gomoku;

import com.goosethings.tools.boardgame.BoardPackets;
import com.goosethings.tools.boardgame.BoardGameItems;
import net.minecraft.network.chat.ClickEvent;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
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
    private static BoardPackets.View view;
    private static long receivedAt, sequence, requests, pendingOpen, noticeUntil;
    private static int notice;
    public static int selectedKind;
    private static KeyMapping key;
    private static final Map<Screen, Button> launchers = new WeakHashMap<>();
    private GomokuClient() {}
    public static Component text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("gomoku.goosetools." + key, fallback, args);
    }
    public static long now() { return System.nanoTime() / 1_000_000; }
    public static BoardPackets.View view() { return view; }
    public static int remaining() { return view == null ? 0 : (int) Math.max(0, view.position().remaining() - (now() - receivedAt)); }
    public static int inviteRemaining() { return view == null ? 0 : (int) Math.max(0, view.inviteRemaining() - (now() - receivedAt)); }
    public static int notice() { return now() < noticeUntil ? notice : 0; }
    public static void register() {
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.goosetools.gomoku", InputConstants.Type.KEYBOARD,
                InputConstants.KEY_F9, KeyMapping.Category.register(Identifier.fromNamespaceAndPath("goosetools", "games"))));
        ClientPlayNetworking.registerGlobalReceiver(BoardPackets.View.TYPE, (packet, ctx) -> ctx.client().execute(() -> apply(packet)));
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
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("goosetools", "board_countdown"), (g, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (view == null || view.group() == 0 || view.position().id() == 0 || view.position().outcome() != 0 || mc.gui.screen() instanceof GomokuScreen || !presentationAllowed()) return;
            int w = Math.min(206, mc.getWindow().getGuiScaledWidth() - 16);
            int x = mc.getWindow().getGuiScaledWidth() - w - 8, y = mc.getWindow().getGuiScaledHeight() - 62;
            BoardGameSkin.panel(g, x, y, w, 50);
            g.text(mc.font, gameName(view.position().kind()), x + 10, y + 9, BoardGameSkin.GOLD, false);
            Component status = text(view.position().turn() == view.position().color() ? "hud_yours" : "hud_theirs", "%ss · /boardgames", (remaining() + 999) / 1000);
            float scale = Math.min(1, (w - 20F) / Math.max(1, mc.font.width(status)));
            g.pose().pushMatrix(); g.pose().translate(x + 10F, y + 25F); g.pose().scale(scale, scale);
            g.text(mc.font, status, 0, 0, remaining() <= 10000 ? 0xFFFF8D78 : BoardGameSkin.INK, false);
            g.pose().popMatrix();
            BoardGameSkin.progress(g, x + 10, y + 40, w - 20, remaining());
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
            button = Button.builder(text("home", "Board games"), b -> open())
                    .bounds(Math.max(8, screen.width - 112), 8, 104, 20).build();
            Screens.getWidgets(screen).add(button); launchers.put(screen, button);
        }
        button.visible = view != null && view.group() != 0 && presentationAllowed();
        button.active = button.visible;
    }
    public static void open() {
        if (!presentationAllowed() || !ClientPlayNetworking.canSend(BoardPackets.Action.TYPE)) return;
        pendingOpen = ++requests;
        send(new BoardPackets.Action(BoardPackets.OPEN, pendingOpen, 0, 0, sequence++, 0, 0, BoardPackets.NONE, selectedKind));
    }
    public static void action(int action, int value, int option, java.util.UUID target) {
        if (view == null) return;
        long id = action == BoardPackets.ACCEPT || action == BoardPackets.DECLINE || action == BoardPackets.CANCEL_INVITE ? view.invite() : view.position().id();
        send(new BoardPackets.Action(action, 0, id, view.position().revision(), sequence++, value, option, target, view.position().id() == 0 ? selectedKind : view.position().kind()));
    }
    public static void action(int action) { action(action, 0, 0, BoardPackets.NONE); }
    public static boolean matchesKey(net.minecraft.client.input.KeyEvent event) { return key != null && key.matches(event); }
    public static void hide() {
        pendingOpen = 0; action(BoardPackets.HIDE);
        if (view != null && view.position().id() != 0 && view.position().outcome() == 0 && now() - lastReturnMessage > 3000) returnMessage(false);
    }
    private static long lastReturnMessage;
    private static void returnMessage(boolean ended) {
        lastReturnMessage = now();
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(text(ended ? "board_finished" : "return_link", ended ? "Game ended. [View result]" : "Move timer continues. [Return to game]").copy()
                .withStyle(style -> style.withColor(0xEABB63).withUnderlined(true).withClickEvent(new ClickEvent.RunCommand("/boardgames"))));
    }
    public static Component gameName(int kind) { return kind == 1 ? text("xiangqi", "Xiangqi") : kind == 2 ? text("chess", "Chess") : text("title", "Gomoku"); }
    public static boolean spectatorHotbar(Minecraft mc) {
        if (mc.player == null || !mc.player.isSpectator() || view == null || view.group() != 2) return false;
        return BoardGameItems.isGamepad(mc.player.getInventory().getItem(1)) || BoardGameItems.isGamepad(mc.player.getInventory().getItem(2));
    }
    public static boolean useGamepad(Minecraft mc) {
        if (mc.player == null || view == null || view.group() == 0 || !BoardGameItems.isGamepad(mc.player.getMainHandItem())) return false;
        open(); return true;
    }
    private static void send(BoardPackets.Action action) { if (ClientPlayNetworking.canSend(BoardPackets.Action.TYPE)) ClientPlayNetworking.send(action); }
    private static void apply(BoardPackets.View next) {
        if (view != null && next.serial() <= view.serial()) return;
        Minecraft mc = Minecraft.getInstance();
        BoardPackets.View old = view; view = next; receivedAt = now();
        if (next.notice() != 0) { notice = next.notice(); noticeUntil = now() + 4000; }
        if (next.group() == 0 || next.notice() == 6) {
            pendingOpen = 0;
            if (next.notice() == 1 && mc.player != null)
                mc.player.sendSystemMessage(text("unavailable", "Available in the lobby or after elimination only."));
            if (mc.gui.screen() instanceof GomokuScreen) mc.setScreenAndShow(null);
            return;
        }
        if (next.inviteDirection() == 2 && (old == null || next.invite() != old.invite())) {
            var message = text("board_invited", "%s invites you to %s (30s). ", next.inviteName(), gameName(next.inviteKind())).copy();
            message.append(text("accept_link", "[Accept and enter]").copy().withStyle(style -> style.withColor(0x7ACB8B).withUnderlined(true).withClickEvent(new ClickEvent.RunCommand("/boardgames accept " + next.invite()))));
            message.append(Component.literal("  ")).append(text("decline_link", "[Decline]").copy().withStyle(style -> style.withColor(0xDFA285).withClickEvent(new ClickEvent.RunCommand("/boardgames decline " + next.invite()))));
            mc.gui.hud.getChat().addClientSystemMessage(message);
        }
        boolean started = next.position().id() != 0 && (old == null || old.position().id() != next.position().id());
        boolean ended = old != null && old.position().id() == next.position().id() && old.position().outcome() == 0 && next.position().outcome() != 0;
        if (started || ended) returnMessage(ended);
        if ((next.request() == -1 || next.request() > 0 && next.request() == pendingOpen) && presentationAllowed()) {
            pendingOpen = 0; mc.setScreenAndShow(new GomokuScreen());
        }
        if (mc.gui.screen() instanceof GomokuScreen screen) screen.updated(old, next);
    }
}
