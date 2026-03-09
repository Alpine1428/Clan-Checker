package com.clanchecker;

import com.clanchecker.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.List;

public class ClanCheckerHud {

    private static int panelX = 0;
    private static int panelY = 0;
    private static int panelWidth = 180;
    private static int entryHeight = 12;

    public static void renderOverlay(DrawContext drawContext, GenericContainerScreen containerScreen,
                                      int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        ClanScanManager manager = ClanScanManager.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        HandledScreenAccessor accessor = (HandledScreenAccessor) containerScreen;
        int guiLeft = accessor.getX();
        int guiTop = accessor.getY();
        int guiWidth = accessor.getBackgroundWidth();

        panelX = guiLeft + guiWidth + 6;
        panelY = guiTop;

        if (!manager.isScanComplete()) {
            renderHintPanel(drawContext, textRenderer);
            return;
        }

        renderResultsPanel(drawContext, textRenderer, manager, containerScreen);
    }

    private static void renderHintPanel(DrawContext drawContext, TextRenderer textRenderer) {
        int totalHeight = 50;

        drawContext.fill(panelX - 2, panelY - 2,
                panelX + panelWidth + 2, panelY + totalHeight + 2, 0xCC000000);

        drawBorder(drawContext, panelX - 2, panelY - 2,
                panelX + panelWidth + 2, panelY + totalHeight + 2, 0xFF555555);

        drawContext.drawText(textRenderer, "\u00a76\u00a7lClan Checker",
                panelX + 4, panelY + 4, 0xFFAA00, true);

        drawContext.drawText(textRenderer, "\u00a77Press \u00a7eR \u00a77to scan clans",
                panelX + 4, panelY + 20, 0x777777, true);
        drawContext.drawText(textRenderer, "\u00a77LMB on slot = copy creator",
                panelX + 4, panelY + 32, 0x777777, true);
    }

    private static void renderResultsPanel(DrawContext drawContext, TextRenderer textRenderer,
                                             ClanScanManager manager,
                                             GenericContainerScreen containerScreen) {
        List<ViolationDatabase.ViolationResult> violations = manager.getViolations();
        List<ClanScanManager.ScannedClan> allClans = manager.getAllClans();

        int headerHeight = 22;
        int statsHeight = 14;
        int violationsHeight = violations.isEmpty() ? 16 : (violations.size() * entryHeight + 6);
        int helpHeight = 26;
        int totalHeight = headerHeight + statsHeight + violationsHeight + helpHeight + 4;

        drawContext.fill(panelX - 2, panelY - 2,
                panelX + panelWidth + 2, panelY + totalHeight + 2, 0xDD000000);

        int borderColor = violations.isEmpty() ? 0xFF00AA00 : 0xFFAA0000;
        drawBorder(drawContext, panelX - 2, panelY - 2,
                panelX + panelWidth + 2, panelY + totalHeight + 2, borderColor);

        String titlePrefix = violations.isEmpty() ? "\u00a7a" : "\u00a7c";
        drawContext.drawText(textRenderer, titlePrefix + "\u00a7lClan Checker",
                panelX + 4, panelY + 4, 0xFFFFFF, true);

        drawContext.fill(panelX, panelY + headerHeight - 2,
                panelX + panelWidth, panelY + headerHeight - 1, borderColor);

        int statY = panelY + headerHeight + 2;
        String statText = "\u00a77Clans: \u00a7f" + allClans.size() + " \u00a77| Bad: " +
                (violations.isEmpty() ? "\u00a7a0" : "\u00a7c" + violations.size());
        drawContext.drawText(textRenderer, statText, panelX + 4, statY, 0xAAAAAA, true);

        int listStartY = statY + statsHeight + 2;

        if (violations.isEmpty()) {
            drawContext.drawText(textRenderer, "\u00a7a  \u2714 All clean!",
                    panelX + 4, listStartY, 0x55FF55, true);
        } else {
            int selectedIndex = manager.getSelectedViolationIndex();

            for (int i = 0; i < violations.size(); i++) {
                ViolationDatabase.ViolationResult v = violations.get(i);
                int yPos = listStartY + i * entryHeight;

                if (i == selectedIndex) {
                    drawContext.fill(panelX, yPos - 1,
                            panelX + panelWidth, yPos + entryHeight - 1, 0x44FFAA00);
                }

                String displayName = v.clanName;
                if (displayName.length() > 10) {
                    displayName = displayName.substring(0, 9) + "..";
                }

                String prefix = (i == selectedIndex) ? "\u00a7e\u25b6 " : "\u00a7c- ";
                String catShort = shortenCategory(v.category);

                drawContext.drawText(textRenderer,
                        prefix + "\u00a7f" + displayName + " \u00a78| \u00a7c" + catShort,
                        panelX + 4, yPos, 0xFFAA00, true);
            }
        }

        int helpY = listStartY + (violations.isEmpty() ? 16 : violations.size() * entryHeight) + 4;
        drawContext.drawText(textRenderer, "\u00a78\u2191\u2193 navigate | R rescan",
                panelX + 4, helpY, 0x555555, true);
        drawContext.drawText(textRenderer, "\u00a78LMB on slot = copy nick",
                panelX + 4, helpY + 12, 0x555555, true);

        if (manager.getHoverSlot() >= 0) {
            highlightSlot(drawContext, containerScreen, manager.getHoverSlot());
        }
    }

    private static void highlightSlot(DrawContext drawContext,
                                       GenericContainerScreen containerScreen, int slotIndex) {
        try {
            var handler = containerScreen.getScreenHandler();
            if (slotIndex >= handler.slots.size()) return;

            HandledScreenAccessor accessor = (HandledScreenAccessor) containerScreen;
            Slot slot = handler.getSlot(slotIndex);

            int x = accessor.getX() + slot.x;
            int y = accessor.getY() + slot.y;

            drawContext.fill(x, y, x + 16, y + 16, 0x55FF0000);

            int c = 0xFFFF0000;
            drawContext.fill(x - 1, y - 1, x + 17, y, c);
            drawContext.fill(x - 1, y + 16, x + 17, y + 17, c);
            drawContext.fill(x - 1, y, x, y + 16, c);
            drawContext.fill(x + 16, y, x + 17, y + 16, c);
        } catch (Exception ignored) {}
    }

    private static String shortenCategory(String category) {
        if (category == null) return "?";
        switch (category) {
            case "Cheats": return "Cheat";
            case "Profanity": return "Prof.";
            case "Insults": return "Insult";
            case "Politics": return "Polit.";
            case "NSFW": return "18+";
            case "Server Rules": return "Rules";
            default: return category.length() > 6 ? category.substring(0, 6) : category;
        }
    }

    private static void drawBorder(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        ctx.fill(x1, y1, x2, y1 + 1, color);
        ctx.fill(x1, y2 - 1, x2, y2, color);
        ctx.fill(x1, y1, x1 + 1, y2, color);
        ctx.fill(x2 - 1, y1, x2, y2, color);
    }

    public static boolean handlePanelClick(double mouseX, double mouseY) {
        ClanScanManager manager = ClanScanManager.getInstance();
        if (!manager.isScanComplete()) return false;

        List<ViolationDatabase.ViolationResult> violations = manager.getViolations();
        if (violations.isEmpty()) return false;

        int headerHeight = 22;
        int statsHeight = 14;
        int listStartY = panelY + headerHeight + 2 + statsHeight + 2;

        for (int i = 0; i < violations.size(); i++) {
            int yPos = listStartY + i * entryHeight;
            if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
                    mouseY >= yPos - 1 && mouseY <= yPos + entryHeight - 1) {
                manager.selectViolation(i);
                return true;
            }
        }
        return false;
    }

    public static boolean handleSlotClick(GenericContainerScreen containerScreen,
                                           double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;

        ClanScanManager manager = ClanScanManager.getInstance();
        if (!manager.isScanComplete()) return false;

        HandledScreenAccessor accessor = (HandledScreenAccessor) containerScreen;
        var handler = containerScreen.getScreenHandler();
        int containerSlots = handler.getRows() * 9;

        for (int i = 0; i < containerSlots && i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            int slotX = accessor.getX() + slot.x;
            int slotY = accessor.getY() + slot.y;

            if (mouseX >= slotX && mouseX < slotX + 16 &&
                mouseY >= slotY && mouseY < slotY + 16) {

                String creator = manager.getCreatorForSlot(i);
                if (creator == null) {
                    creator = manager.extractCreatorFromScreen(client, i);
                }

                if (creator != null && !creator.isEmpty()) {
                    client.keyboard.setClipboard(creator);
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal(
                            "\u00a7a[ClanChecker] \u00a7fCopied: \u00a7b" + creator), false);
                    }
                    return true;
                } else {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal(
                            "\u00a7c[ClanChecker] \u00a7fCreator not found in lore"), false);
                    }
                    return false;
                }
            }
        }
        return false;
    }
}
