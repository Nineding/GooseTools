package com.goosethings.tools.client.gomoku;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** PixelLab panels, sampled from the approved atlases; geometry remains owned by the screen. */
final class BoardGameSkin {
    static final int INK = 0xFFF5E8D0, MUTED = 0xFFB5CBC5, GOLD = 0xFFEAC582;
    private static final Identifier LOBBY = texture("lobby"), MATCH = texture("match");
    private record Region(Identifier texture, int x, int y, int w, int h, int edge) {}
    private static final Region FRAME = new Region(MATCH, 23, 18, 467, 300, 12);
    private static final Region HEADER = new Region(MATCH, 36, 30, 441, 26, 5);
    private static final Region BOARD = new Region(MATCH, 38, 63, 246, 242, 12);
    private static final Region PANEL = new Region(MATCH, 299, 64, 177, 241, 12);
    private static final Region RAIL = new Region(LOBBY, 38, 39, 439, 70, 9);
    private static final Region BUTTON = new Region(LOBBY, 383, 240, 73, 22, 5);
    private static final Region PRIMARY = new Region(LOBBY, 305, 240, 71, 22, 5);
    private BoardGameSkin() {}

    static void window(GuiGraphicsExtractor g, int w, int h) {
        g.fill(4, 6, w + 4, h + 6, 0x77000000);
        g.fill(5, 5, w - 5, h - 5, 0xFF352824);
        slices(g, FRAME, 0, 0, w, h, 12, false);
    }
    static void header(GuiGraphicsExtractor g, int x, int y, int w, int h) { slices(g, HEADER, x, y, w, h, 5, true); }
    static void board(GuiGraphicsExtractor g, int x, int y, int size) { slices(g, BOARD, x, y, size, size, 8, true); }
    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) { slices(g, PANEL, x, y, w, h, 8, true); }
    static void rail(GuiGraphicsExtractor g, int x, int y, int w, int h) { slices(g, RAIL, x, y, w, h, 8, true); }
    static void card(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean selected, boolean hover, boolean enabled) {
        blit(g, LOBBY, x, y, x + w, y + h, 38, 115, 178, 216);
        g.nextStratum();
        if (!enabled) g.fill(x + 5, y + 5, x + w - 5, y + h - 5, 0x55202B2C);
        if (selected || hover && enabled) outline(g, x + 3, y + 3, w - 6, h - 6, GOLD);
        if (selected) {
            g.fill(x + w - 23, y + 10, x + w - 11, y + 22, 0xFF214F4C);
            g.fill(x + w - 21, y + 15, x + w - 18, y + 18, GOLD);
            g.fill(x + w - 18, y + 17, x + w - 16, y + 20, GOLD);
            g.fill(x + w - 16, y + 13, x + w - 13, y + 18, GOLD);
        }
    }
    static void button(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean enabled, boolean selected, boolean hover) {
        slices(g, selected ? PRIMARY : BUTTON, x, y, w, h, 5, true);
        if (!enabled) g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0x991B292C);
        if (hover && enabled) outline(g, x + 1, y + 1, w - 2, h - 2, GOLD);
    }
    static void progress(GuiGraphicsExtractor g, int x, int y, int w, int remaining) {
        g.nextStratum();
        g.fill(x, y, x + w, y + 3, 0xFF20383C);
        int fill = (int) Math.ceil(w * Math.clamp(remaining / 60000.0, 0, 1));
        if (fill > 0) g.fill(x, y, x + fill, y + 3, remaining <= 10000 ? 0xFFE4947A : GOLD);
    }
    static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int ink) {
        g.fill(x, y, x + w, y + 1, ink); g.fill(x, y + h - 1, x + w, y + h, ink);
        g.fill(x, y, x + 1, y + h, ink); g.fill(x + w - 1, y, x + w, y + h, ink);
    }
    private static void slices(GuiGraphicsExtractor g, Region r, int x, int y, int w, int h, int edge, boolean center) {
        edge = Math.min(edge, Math.min(w, h) / 2);
        int[] dx = {x, x + edge, x + w - edge, x + w}, dy = {y, y + edge, y + h - edge, y + h};
        int[] sx = {r.x, r.x + r.edge, r.x + r.w - r.edge, r.x + r.w};
        int[] sy = {r.y, r.y + r.edge, r.y + r.h - r.edge, r.y + r.h};
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            if (!center && row == 1 && col == 1) continue;
            blit(g, r.texture, dx[col], dy[row], dx[col + 1], dy[row + 1], sx[col], sy[row], sx[col + 1], sy[row + 1]);
        }
        // Keep the atlas below dynamic controls in both Screen and HUD passes.
        g.nextStratum();
    }
    private static void blit(GuiGraphicsExtractor g, Identifier image, int x1, int y1, int x2, int y2, int u1, int v1, int u2, int v2) {
        g.blit(image, x1, y1, x2, y2, u1 / 512F, u2 / 512F, v1 / 336F, v2 / 336F);
    }
    private static Identifier texture(String name) { return Identifier.fromNamespaceAndPath("goosetools", "textures/gui/board_room/" + name + ".png"); }
}
