package com.goosethings.tools.client.ai;

import com.goosethings.tools.network.GooseToolsPayloads;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.UUID;

/** End-of-match, non-pausing confirmation vote for one uncertain spoken player name. */
public final class AiNameVoteScreen extends Screen {
    private static final int GAP = 6;
    private final GooseToolsPayloads.AiNameVoteOpenS2C vote;
    private final long openedAtMillis = System.currentTimeMillis();
    private int page;
    private boolean submitted;

    AiNameVoteScreen(GooseToolsPayloads.AiNameVoteOpenS2C vote) {
        super(Component.translatableWithFallback(
                "screen.goosetools.ai_name_vote.title", "Confirm an uncertain player name"));
        this.vote = vote;
    }

    long voteId() {
        return vote.voteId();
    }

    void closeFromServer() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() == this) minecraft.setScreenAndShow(null);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xF212171D);
        int panelLeft = Math.max(12, width / 2 - 250);
        int panelRight = Math.min(width - 12, width / 2 + 250);
        graphics.fill(panelLeft, 12, panelRight, height - 12, 0xFA0A1118);
        graphics.outline(panelLeft, 12, panelRight - panelLeft, height - 24, 0xFF456A82);

        Component heading = Component.translatableWithFallback(
                "screen.goosetools.ai_name_vote.heading",
                "AI heard “%s”. Which player was meant?", vote.alias());
        graphics.text(font, heading, width / 2 - font.width(heading) / 2, 22, 0xFFEAF7FF, false);
        Component count = Component.translatableWithFallback(
                "screen.goosetools.ai_name_vote.question_count", "Question %s/%s",
                vote.questionIndex(), vote.questionCount());
        graphics.text(font, count, width / 2 - font.width(count) / 2, 37, 0xFF8EABB9, false);

        List<FormattedCharSequence> context = font.split(Component.literal(vote.context()), panelRight - panelLeft - 32);
        int contextY = 52;
        for (int index = 0; index < Math.min(2, context.size()); index++) {
            FormattedCharSequence line = context.get(index);
            graphics.text(font, line, width / 2 - font.width(line) / 2, contextY + index * 11,
                    0xFFBFD0D9, false);
        }

        Grid grid = grid();
        int start = page * grid.pageSize();
        int end = Math.min(vote.candidates().size(), start + grid.pageSize());
        for (int index = start; index < end; index++) {
            int local = index - start;
            Rect rect = grid.candidate(local);
            boolean hover = rect.contains(mouseX, mouseY) && !submitted;
            graphics.fill(rect.left(), rect.top(), rect.right(), rect.bottom(),
                    submitted ? 0xFF17232B : hover ? 0xFF356783 : 0xFF213C4C);
            graphics.outline(rect.left(), rect.top(), rect.width(), rect.height(),
                    hover ? 0xFF9CE1FF : 0xFF456A82);
            String name = vote.candidates().get(index).playerName();
            graphics.text(font, Component.literal(name), rect.left() + 8,
                    rect.top() + (rect.height() - font.lineHeight) / 2, 0xFFF2FAFF, false);
        }

        Rect unknown = unknownButton();
        boolean unknownHover = unknown.contains(mouseX, mouseY) && !submitted;
        graphics.fill(unknown.left(), unknown.top(), unknown.right(), unknown.bottom(),
                submitted ? 0xFF2A2F32 : unknownHover ? 0xFF735A37 : 0xFF4B3E2E);
        graphics.outline(unknown.left(), unknown.top(), unknown.width(), unknown.height(),
                unknownHover ? 0xFFFFD394 : 0xFF80694A);
        Component unknownLabel = submitted
                ? Component.translatableWithFallback("screen.goosetools.ai_name_vote.submitted", "Vote submitted")
                : Component.translatableWithFallback(
                        "screen.goosetools.ai_name_vote.unknown", "Cannot tell / not a player");
        graphics.text(font, unknownLabel, width / 2 - font.width(unknownLabel) / 2,
                unknown.top() + (unknown.height() - font.lineHeight) / 2, 0xFFFFE8C5, false);

        int pages = pages(grid.pageSize());
        if (pages > 1) {
            drawPageButton(graphics, previousButton(), mouseX, mouseY, page > 0, "‹");
            drawPageButton(graphics, nextButton(), mouseX, mouseY, page + 1 < pages, "›");
        }
        int leftSeconds = Math.max(0, vote.timeoutSeconds()
                - (int) ((System.currentTimeMillis() - openedAtMillis) / 1000L));
        Component timer = Component.translatableWithFallback(
                "screen.goosetools.ai_name_vote.timer", "%s seconds remaining", leftSeconds);
        graphics.text(font, timer, width / 2 - font.width(timer) / 2, height - 26, 0xFF91A9B5, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || submitted) {
            return super.mouseClicked(event, doubleClick);
        }
        Grid grid = grid();
        int start = page * grid.pageSize();
        int end = Math.min(vote.candidates().size(), start + grid.pageSize());
        for (int index = start; index < end; index++) {
            if (grid.candidate(index - start).contains(event.x(), event.y())) {
                submit(vote.candidates().get(index).playerId());
                return true;
            }
        }
        if (unknownButton().contains(event.x(), event.y())) {
            submit(AiReviewFlowClient.UNKNOWN);
            return true;
        }
        int pages = pages(grid.pageSize());
        if (pages > 1 && page > 0 && previousButton().contains(event.x(), event.y())) {
            page--;
            return true;
        }
        if (pages > 1 && page + 1 < pages && nextButton().contains(event.x(), event.y())) {
            page++;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void submit(UUID playerId) {
        if (submitted || !ClientPlayNetworking.canSend(GooseToolsPayloads.AiNameVoteC2S.TYPE)) return;
        submitted = true;
        ClientPlayNetworking.send(new GooseToolsPayloads.AiNameVoteC2S(vote.voteId(), playerId));
    }

    private Grid grid() {
        int columns = width >= 460 ? 2 : 1;
        int top = 82;
        int bottom = height - 70;
        int rows = Math.max(1, (bottom - top) / 24);
        int panelWidth = Math.min(460, width - 48);
        return new Grid(width / 2 - panelWidth / 2, top, panelWidth, rows, columns);
    }

    private int pages(int pageSize) {
        return Math.max(1, (vote.candidates().size() + pageSize - 1) / pageSize);
    }

    private Rect unknownButton() {
        return new Rect(width / 2 - 110, height - 58, width / 2 + 110, height - 38);
    }

    private Rect previousButton() {
        return new Rect(width / 2 - 152, height - 58, width / 2 - 124, height - 38);
    }

    private Rect nextButton() {
        return new Rect(width / 2 + 124, height - 58, width / 2 + 152, height - 38);
    }

    private void drawPageButton(GuiGraphicsExtractor graphics, Rect rect, int mouseX, int mouseY,
                                boolean active, String label) {
        boolean hover = active && rect.contains(mouseX, mouseY);
        graphics.fill(rect.left(), rect.top(), rect.right(), rect.bottom(),
                active ? hover ? 0xFF356783 : 0xFF213C4C : 0xFF17232B);
        graphics.outline(rect.left(), rect.top(), rect.width(), rect.height(), 0xFF456A82);
        graphics.text(font, Component.literal(label), rect.left() + rect.width() / 2 - font.width(label) / 2,
                rect.top() + 5, active ? 0xFFF2FAFF : 0xFF5D707A, false);
    }

    private record Grid(int left, int top, int width, int rows, int columns) {
        int pageSize() { return rows * columns; }

        Rect candidate(int index) {
            int column = index / rows;
            int row = index % rows;
            int columnWidth = (width - (columns - 1) * GAP) / columns;
            int x = left + column * (columnWidth + GAP);
            int y = top + row * 24;
            return new Rect(x, y, x + columnWidth, y + 20);
        }
    }

    private record Rect(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
        boolean contains(double x, double y) { return x >= left && x < right && y >= top && y < bottom; }
    }
}
