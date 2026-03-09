package com.clanchecker;

import com.clanchecker.mixin.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;

import java.util.List;

/**
 * Отрисовка панели Clan Checker справа от инвентаря кланов.
 */
public class ClanCheckerHud {

    private static int panelX = 0;
    private static int panelY = 0;
    private static int panelWidth = 160;
    private static int entryHeight = 12;

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) return;

            if (!(client.currentScreen instanceof GenericContainerScreen containerScreen)) {
                return;
            }

            ClanScanManager manager = ClanScanManager.getInstance();

            if (!manager.isScanComplete()) {
                return;
            }

            renderPanel(drawContext, client, containerScreen, manager);

            // Если выбрано нарушение — перемещаем курсор на нужный слот
            int hoverSlot = manager.getHoverSlot();
            if (hoverSlot >= 0) {
                moveMouseToSlot(client, containerScreen, hoverSlot);
            }
        });
    }

    private static void renderPanel(DrawContext drawContext, MinecraftClient client,
                                     GenericContainerScreen containerScreen,
                                     ClanScanManager manager) {
        TextRenderer textRenderer = client.textRenderer;
        List<ViolationDatabase.ViolationResult> violations = manager.getViolations();

        HandledScreenAccessor accessor = (HandledScreenAccessor) containerScreen;
        int guiLeft = accessor.getX();
        int guiTop = accessor.getY();
        int guiWidth = accessor.getBackgroundWidth();

        panelX = guiLeft + guiWidth + 5;
        panelY = guiTop;

        int headerHeight = 20;
        int contentHeight = violations.isEmpty() ? 14 : violations.size() * entryHeight + 4;
        int totalHeight = headerHeight + contentHeight + 20; // +20 для подсказки

        // Фон панели
        drawContext.fill(panelX - 2, panelY - 2,
                panelX + panelWidth + 2, panelY + totalHeight + 2,
                0xCC000000);

        // Рамка (зелёная)
        int borderColor = 0xFF00AA00;
        // Верх
        drawContext.fill(panelX - 2, panelY - 2,
                panelX + panelWidth + 2, panelY - 1, borderColor);
        // Низ
        drawContext.fill(panelX - 2, panelY + totalHeight + 1,
                panelX + panelWidth + 2, panelY + totalHeight + 2, borderColor);
        // Лево
        drawContext.fill(panelX - 2, panelY - 2,
                panelX - 1, panelY + totalHeight + 2, borderColor);
        // Право
        drawContext.fill(panelX + panelWidth + 1, panelY - 2,
                panelX + panelWidth + 2, panelY + totalHeight + 2, borderColor);

        // Заголовок
        drawContext.drawText(textRenderer, "§a§lClan Checker",
                panelX + 4, panelY + 4, 0x55FF55, true);

        // Разделительная линия
        drawContext.fill(panelX, panelY + headerHeight - 2,
                panelX + panelWidth, panelY + headerHeight - 1, borderColor);

        if (violations.isEmpty()) {
            drawContext.drawText(textRenderer, "§aНарушений нет ✓",
                    panelX + 4, panelY + headerHeight + 4, 0x55FF55, true);
        } else {
            int selectedIndex = manager.getSelectedViolationIndex();

            for (int i = 0; i < violations.size(); i++) {
                ViolationDatabase.ViolationResult v = violations.get(i);
                int yPos = panelY + headerHeight + 4 + i * entryHeight;

                // Подсветка выбранного элемента
                if (i == selectedIndex) {
                    drawContext.fill(panelX, yPos - 1,
                            panelX + panelWidth, yPos + entryHeight - 1, 0x44FFAA00);
                }

                String displayName = v.clanName;
                if (displayName.length() > 14) {
                    displayName = displayName.substring(0, 12) + "..";
                }

                String prefix = (i == selectedIndex) ? "§e▶ " : "§c• ";
                drawContext.drawText(textRenderer, prefix + displayName,
                        panelX + 4, yPos, 0xFFAA00, true);
            }

            // Подсказки управления
            int helpY = panelY + headerHeight + 4 + violations.size() * entryHeight + 6;
            drawContext.drawText(textRenderer, "§7↑↓ выбор | клик",
                    panelX + 4, helpY, 0x777777, true);
        }
    }

    /**
     * Перемещает курсор мыши к центру указанного слота.
     */
    private static void moveMouseToSlot(MinecraftClient client,
                                         GenericContainerScreen containerScreen, int slotIndex) {
        try {
            var handler = containerScreen.getScreenHandler();
            if (slotIndex >= handler.slots.size()) return;

            HandledScreenAccessor accessor = (HandledScreenAccessor) containerScreen;
            Slot slot = handler.getSlot(slotIndex);

            int slotScreenX = accessor.getX() + slot.x + 8;
            int slotScreenY = accessor.getY() + slot.y + 8;

            double scaleFactor = client.getWindow().getScaleFactor();
            double mouseX = slotScreenX * scaleFactor;
            double mouseY = slotScreenY * scaleFactor;

            org.lwjgl.glfw.GLFW.glfwSetCursorPos(
                    client.getWindow().getHandle(),
                    mouseX, mouseY
            );
        } catch (Exception e) {
            ClanCheckerMod.LOGGER.error("[ClanChecker] Ошибка перемещения курсора: {}", e.getMessage());
        }
    }

    /**
     * Обработка клика мышью по панели.
     * Возвращает true если клик обработан.
     */
    public static boolean handleClick(double mouseX, double mouseY) {
        ClanScanManager manager = ClanScanManager.getInstance();
        if (!manager.isScanComplete()) return false;

        List<ViolationDatabase.ViolationResult> violations = manager.getViolations();
        if (violations.isEmpty()) return false;

        int headerHeight = 20;
        for (int i = 0; i < violations.size(); i++) {
            int yPos = panelY + headerHeight + 4 + i * entryHeight;
            if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
                    mouseY >= yPos - 1 && mouseY <= yPos + entryHeight - 1) {
                manager.selectViolation(i);
                return true;
            }
        }
        return false;
    }
}
