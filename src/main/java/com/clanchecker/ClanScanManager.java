package com.clanchecker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ClanScanManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("ClanChecker");
    private static ClanScanManager instance;

    public enum ScanState {
        IDLE,
        WAITING,
        SCANNING,
        DONE
    }

    private ScanState state = ScanState.IDLE;
    private final List<ViolationDatabase.ViolationResult> violations = new ArrayList<>();
    private final List<ScannedClan> allClans = new ArrayList<>();
    private boolean scanComplete = false;
    private int selectedViolationIndex = -1;
    private int hoverSlot = -1;
    private int waitTicks = 0;
    private String lastScreenTitle = "";

    public static class ScannedClan {
        public final String name;
        public final int slot;
        public final boolean hasViolation;

        public ScannedClan(String name, int slot, boolean hasViolation) {
            this.name = name;
            this.slot = slot;
            this.hasViolation = hasViolation;
        }
    }

    public static ClanScanManager getInstance() {
        if (instance == null) {
            instance = new ClanScanManager();
        }
        return instance;
    }

    public void startScan(MinecraftClient client) {
        if (client.currentScreen instanceof GenericContainerScreen containerScreen) {
            String title = containerScreen.getTitle().getString();
            LOGGER.info("[ClanChecker] Начинаем сканирование... Заголовок: '{}'", title);
            lastScreenTitle = title;
            state = ScanState.WAITING;
            waitTicks = 5;
            violations.clear();
            allClans.clear();
            scanComplete = false;
            selectedViolationIndex = -1;
            hoverSlot = -1;

            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("§e[ClanChecker] §fСканирование..."),
                        false
                );
            }
        } else {
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("§c[ClanChecker] §fОткройте меню §e/clan list §fи нажмите R!"),
                        false
                );
            }
        }
    }

    public void tick(MinecraftClient client) {
        if (state == ScanState.WAITING) {
            if (!(client.currentScreen instanceof GenericContainerScreen)) {
                state = ScanState.IDLE;
                return;
            }
            waitTicks--;
            if (waitTicks <= 0) {
                state = ScanState.SCANNING;
                performScan(client);
            }
        }
    }

    private void performScan(MinecraftClient client) {
        if (!(client.currentScreen instanceof GenericContainerScreen containerScreen)) {
            state = ScanState.IDLE;
            return;
        }

        GenericContainerScreenHandler handler = containerScreen.getScreenHandler();
        int totalSlots = handler.slots.size();
        int containerSlots = handler.getRows() * 9;

        LOGGER.info("[ClanChecker] Всего слотов: {}, Контейнер: {}, Рядов: {}",
                totalSlots, containerSlots, handler.getRows());

        int clansChecked = 0;

        for (int slotIndex = 0; slotIndex < containerSlots && slotIndex < totalSlots; slotIndex++) {
            Slot slot = handler.getSlot(slotIndex);
            ItemStack stack = slot.getStack();

            if (stack.isEmpty()) continue;

            if (isDecorativeItem(stack)) {
                LOGGER.debug("[ClanChecker] Слот {} — декоративный: {}", slotIndex,
                        net.minecraft.registry.Registries.ITEM.getId(stack.getItem()));
                continue;
            }

            String clanName = extractClanName(stack);
            if (clanName == null || clanName.isEmpty()) continue;

            clansChecked++;

            List<ViolationDatabase.ViolationResult> results = ViolationDatabase.checkClanName(clanName, slotIndex);
            violations.addAll(results);

            boolean hasViolation = !results.isEmpty();
            allClans.add(new ScannedClan(clanName, slotIndex, hasViolation));

            LOGGER.info("[ClanChecker] Слот {}: '{}' — нарушений: {}",
                    slotIndex, clanName, results.size());
        }

        scanComplete = true;
        state = ScanState.DONE;

        LOGGER.info("[ClanChecker] Завершено. Проверено: {}, Нарушений: {}",
                clansChecked, violations.size());

        if (client.player != null) {
            if (violations.isEmpty()) {
                client.player.sendMessage(
                        Text.literal("§a[ClanChecker] ✓ §fНарушений нет! §7(Кланов: " + clansChecked + ")"),
                        false
                );
            } else {
                client.player.sendMessage(
                        Text.literal("§c[ClanChecker] ✗ §fНарушений: §c" + violations.size() + " §7(Кланов: " + clansChecked + ")"),
                        false
                );
                for (ViolationDatabase.ViolationResult v : violations) {
                    client.player.sendMessage(
                            Text.literal("  §e► " + v.clanName + " §7— §c" + v.category + " §7(\"" + v.matchedWord + "\")"),
                            false
                    );
                }
            }
        }
    }

    private boolean isDecorativeItem(ItemStack stack) {
        String itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();
        if (itemId.contains("stained_glass_pane") || itemId.contains("glass_pane")) return true;
        if (stack.getItem() == Items.BARRIER) return true;
        if (stack.getItem() == Items.ARROW) return true;
        if (itemId.contains("gray_dye") || itemId.contains("light_gray_dye")) return true;
        return false;
    }

    private String extractClanName(ItemStack stack) {
        Text name = stack.getName();
        if (name == null) return null;

        String fullName = name.getString();
        if (fullName.isEmpty()) return null;

        fullName = fullName.replaceAll("§[0-9a-fk-or]", "");

        if (fullName.contains("|")) {
            return fullName.split("\\|")[0].trim();
        }
        if (fullName.contains("(")) {
            return fullName.split("\\(")[0].trim();
        }
        return fullName.trim();
    }

    public void selectViolation(int index) {
        if (index >= 0 && index < violations.size()) {
            selectedViolationIndex = index;
            hoverSlot = violations.get(index).slot;
        }
    }

    public void selectNext() {
        if (violations.isEmpty()) return;
        selectedViolationIndex = (selectedViolationIndex + 1) % violations.size();
        hoverSlot = violations.get(selectedViolationIndex).slot;
    }

    public void selectPrevious() {
        if (violations.isEmpty()) return;
        selectedViolationIndex--;
        if (selectedViolationIndex < 0) selectedViolationIndex = violations.size() - 1;
        hoverSlot = violations.get(selectedViolationIndex).slot;
    }

    public void reset() {
        state = ScanState.IDLE;
        violations.clear();
        allClans.clear();
        scanComplete = false;
        selectedViolationIndex = -1;
        hoverSlot = -1;
    }

    public ScanState getState() { return state; }
    public List<ViolationDatabase.ViolationResult> getViolations() { return violations; }
    public List<ScannedClan> getAllClans() { return allClans; }
    public boolean isScanComplete() { return scanComplete; }
    public int getSelectedViolationIndex() { return selectedViolationIndex; }
    public int getHoverSlot() { return hoverSlot; }
    public String getLastScreenTitle() { return lastScreenTitle; }
}
