package com.example.examplemod.client;

import com.example.examplemod.network.TradePreviewEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 独立的交易预览界面。
 * 界面尺寸与原版交易界面一致（276 × 166），居中显示。
 * 竖排显示 5 个等级，支持鼠标滚轮滚动。
 * 支持悬停物品显示名字。
 * 关闭时返回原交易界面，避免容器被关闭。
 */
public class TradePreviewScreen extends Screen {

    private static final int GUI_WIDTH = 276;
    private static final int GUI_HEIGHT = 166;

    private static final int HEADER_Y = 8;
    private static final int CONTENT_TOP = 26;
    private static final int CONTENT_BOTTOM = 34;
    private static final int MARGIN_LEFT = 10;
    private static final int LINE_HEIGHT = 18;
    private static final int LEVEL_HEADER_HEIGHT = 14;

    private final MerchantScreen parent;
    private final int currentLevel;

    private int guiLeft;
    private int guiTop;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int contentHeight = 0;

    private ItemStack hoveredStack = null;

    public TradePreviewScreen(MerchantScreen parent, int currentLevel) {
        super(Component.literal("交易预览"));
        this.parent = parent;
        this.currentLevel = currentLevel;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        int buttonWidth = 60;
        int buttonHeight = 16;
        int buttonX = guiLeft + (GUI_WIDTH - buttonWidth) / 2;
        int buttonY = guiTop + GUI_HEIGHT - 22;

        Button backButton = Button.builder(
                Component.literal("返回"),
                btn -> this.onClose()
        ).pos(buttonX, buttonY).size(buttonWidth, buttonHeight).build();
        this.addRenderableWidget(backButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        // ---- 面板背景 ----
        guiGraphics.fill(guiLeft - 1, guiTop - 1,
                guiLeft + GUI_WIDTH + 1, guiTop + GUI_HEIGHT + 1,
                0xFF000000);
        guiGraphics.fill(guiLeft, guiTop,
                guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT,
                0xFFC6C6C6);

        // ---- 标题 ----
        guiGraphics.drawString(this.font,
                "§l交易预览 §0(等级: " + currentLevel + " - " + getLevelName(currentLevel) + ")",
                guiLeft + MARGIN_LEFT, guiTop + HEADER_Y, 0x404040, false);

        guiGraphics.fill(guiLeft + MARGIN_LEFT, guiTop + HEADER_Y + 12,
                guiLeft + GUI_WIDTH - MARGIN_LEFT, guiTop + HEADER_Y + 13,
                0xFF555555);

        // ---- 内容区 ----
        int viewTop = guiTop + CONTENT_TOP;
        int viewBottom = guiTop + GUI_HEIGHT - CONTENT_BOTTOM;
        int viewHeight = viewBottom - viewTop;

        contentHeight = 0;
        for (int level = 1; level <= 5; level++) {
            contentHeight += LEVEL_HEADER_HEIGHT;
            List<TradePreviewEntry> trades = TradePreviewCache.getTradesForLevel(level);
            contentHeight += Math.max(1, trades.size()) * LINE_HEIGHT;
            contentHeight += 4;
        }
        maxScroll = Math.max(0, contentHeight - viewHeight);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        boolean mouseInView = mouseX >= guiLeft && mouseX <= guiLeft + GUI_WIDTH
                && mouseY >= viewTop && mouseY <= viewBottom;

        hoveredStack = null;

        guiGraphics.enableScissor(guiLeft, viewTop, guiLeft + GUI_WIDTH, viewBottom);

        int y = viewTop - scrollOffset;

        for (int level = 1; level <= 5; level++) {
            List<TradePreviewEntry> trades = TradePreviewCache.getTradesForLevel(level);
            boolean unlocked = level <= currentLevel;

            String levelTitle = "等级 " + level + " - " + getLevelName(level)
                    + (unlocked ? "" : " (未解锁)");
            int headerColor = unlocked ? 0x006600 : 0x808080;
            guiGraphics.drawString(this.font, "§l" + levelTitle,
                    guiLeft + MARGIN_LEFT, y + 2, headerColor, false);
            y += LEVEL_HEADER_HEIGHT;

            if (trades.isEmpty()) {
                guiGraphics.drawString(this.font, "§7(无交易)",
                        guiLeft + MARGIN_LEFT + 8, y + 3, 0x808080, false);
                y += LINE_HEIGHT;
            } else {
                for (TradePreviewEntry entry : trades) {
                    drawTradeRow(guiGraphics, entry, guiLeft + MARGIN_LEFT, y, unlocked, mouseX, mouseY, mouseInView);
                    y += LINE_HEIGHT;
                }
            }
            y += 4;
        }

        guiGraphics.disableScissor();

        // ---- 滚动条 ----
        if (maxScroll > 0) {
            int barX = guiLeft + GUI_WIDTH - 6;
            int barTop = viewTop;
            int barHeight = viewHeight;
            int knobHeight = Math.max(16, barHeight * viewHeight / contentHeight);
            int knobTop = barTop + (barHeight - knobHeight) * scrollOffset / maxScroll;

            guiGraphics.fill(barX, barTop, barX + 4, barTop + barHeight, 0x40000000);
            guiGraphics.fill(barX, knobTop, barX + 4, knobTop + knobHeight, 0xFF666666);
        }

        // ---- 底部提示 ----
        guiGraphics.drawString(this.font, "§7滚轮滚动",
                guiLeft + MARGIN_LEFT, guiTop + GUI_HEIGHT - 14, 0x404040, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // ---- tooltip 最上层 ----
        if (hoveredStack != null && !hoveredStack.isEmpty()) {
            guiGraphics.renderTooltip(this.font, hoveredStack, mouseX, mouseY);
        }
    }

    private void drawTradeRow(GuiGraphics guiGraphics, TradePreviewEntry entry, int x, int y,
                              boolean unlocked, int mouseX, int mouseY, boolean mouseInView) {
        int textColor = unlocked ? 0x000000 : 0x707070;
        int bgColor = unlocked ? 0x40FFFFFF : 0x20000000;
        guiGraphics.fill(x, y, x + 230, y + 16, bgColor);

        // 成本 A
        ItemStack costA = entry.getCostA();
        int ax = x + 3;
        int ay = y;
        guiGraphics.renderItem(costA, ax, ay);
        guiGraphics.renderItemDecorations(this.font, costA, ax, ay);
        guiGraphics.drawString(this.font, "×" + costA.getCount(),
                x + 22, y + 4, textColor, false);
        if (mouseInView && isHovering(mouseX, mouseY, ax, ay)) {
            hoveredStack = costA;
        }

        int cursorX = x + 50;

        // 成本 B
        if (!entry.getCostB().isEmpty()) {
            guiGraphics.drawString(this.font, "+", cursorX, y + 4, textColor, false);
            cursorX += 8;

            ItemStack costB = entry.getCostB();
            int bx = cursorX;
            int by = y;
            guiGraphics.renderItem(costB, bx, by);
            guiGraphics.renderItemDecorations(this.font, costB, bx, by);
            guiGraphics.drawString(this.font, "×" + costB.getCount(),
                    cursorX + 19, y + 4, textColor, false);
            if (mouseInView && isHovering(mouseX, mouseY, bx, by)) {
                hoveredStack = costB;
            }
            cursorX += 45;
        }

        // 箭头
        guiGraphics.drawString(this.font, "→", cursorX + 4, y + 4, textColor, false);
        cursorX += 22;

        // 结果
        ItemStack result = entry.getResult();
        int rx = cursorX;
        int ry = y;
        guiGraphics.renderItem(result, rx, ry);
        guiGraphics.renderItemDecorations(this.font, result, rx, ry);
        guiGraphics.drawString(this.font, "×" + result.getCount(),
                cursorX + 19, y + 4, textColor, false);
        if (mouseInView && isHovering(mouseX, mouseY, rx, ry)) {
            hoveredStack = result;
        }
    }

    private static boolean isHovering(int mouseX, int mouseY, int itemX, int itemY) {
        return mouseX >= itemX && mouseX < itemX + 16
                && mouseY >= itemY && mouseY < itemY + 16;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= guiLeft && mouseX <= guiLeft + GUI_WIDTH
                && mouseY >= guiTop && mouseY <= guiTop + GUI_HEIGHT) {
            if (maxScroll > 0) {
                scrollOffset = Mth.clamp(scrollOffset - (int) (delta * 18), 0, maxScroll);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String getLevelName(int level) {
        return switch (level) {
            case 1 -> "新手";
            case 2 -> "学徒";
            case 3 -> "老手";
            case 4 -> "专家";
            case 5 -> "大师";
            default -> "未知";
        };
    }
}