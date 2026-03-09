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
        IDLE, WAITING, SCANNING, DONE
    }

    private ScanState state = ScanState.IDLE;
    private final List<ViolationDatabase.ViolationResult> violations = new ArrayList<>();
    private final List<ScannedClan> allClans = new ArrayList<>();
    private boolean scanComplete = false;
    private int selectedViolationIndex = -1;
    private int hoverSlot = -1;
    private int waitTicks = 0;

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
        if (client.currentScreen instanceof GenericContainerScreen) {
            state = ScanState.WAITING;
            waitTicks = 5;
            violations.clear();
            allClans.clear();
            scanComplete = false;
            selectedViolationIndex = -1;
            hoverSlot = -1;

            if (client.player != null) {
                client.player.sendMessage(Text.literal("\u00a7e[ClanChecker] \u00a7fScanning..."), false);
            }
        } else {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("\u00a7c[ClanChecker] \u00a7fOpen \u00a7e/clan list \u00a7ffirst!"), false);
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

        LOGGER.info("[ClanChecker] Total slots: {}, Container: {}, Rows: {}",
                totalSlots, containerSlots, handler.getRows());

        int clansChecked = 0;

        for (int i = 0; i < containerSlots && i < totalSlots; i++) {
            Slot slot = handler.getSlot(i);
            ItemStack stack = slot.getStack();

            if (stack.isEmpty()) continue;
            if (isDecorativeItem(stack)) continue;

            String clanName = extractClanName(stack);
            if (clanName == null || clanName.isEmpty()) continue;

            clansChecked++;

            List<ViolationDatabase.ViolationResult> results = ViolationDatabase.checkClanName(clanName, i);
            violations.addAll(results);
            allClans.add(new ScannedClan(clanName, i, !results.isEmpty()));

            LOGGER.info("[ClanChecker] Slot {}: '{}' violations: {}", i, clanName, results.size());
        }

        scanComplete = true;
        state = ScanState.DONE;

        if (client.player != null) {
            if (violations.isEmpty()) {
                client.player.sendMessage(Text.literal(
                        "\u00a7a[ClanChecker] \u00a7fNo violations found! \u00a77(Clans: " + clansChecked + ")"), false);
            } else {
                client.player.sendMessage(Text.literal(
                        "\u00a7c[ClanChecker] \u00a7fViolations: \u00a7c" + violations.size() +
                        " \u00a77(Clans: " + clansChecked + ")"), false);
                for (ViolationDatabase.ViolationResult v : violations) {
                    client.player.sendMessage(Text.literal(
                            "  \u00a7e> " + v.clanName + " \u00a77- \u00a7c" + v.category +
                            " \u00a77(\"" + v.matchedWord + "\")"), false);
                }
            }
        }
    }

    private boolean isDecorativeItem(ItemStack stack) {
        String itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();
        if (itemId.contains("glass_pane")) return true;
        if (stack.getItem() == Items.BARRIER) return true;
        if (stack.getItem() == Items.ARROW) return true;
        if (itemId.contains("gray_dye")) return true;
        return false;
    }

    private String extractClanName(ItemStack stack) {
        Text name = stack.getName();
        if (name == null) return null;

        String fullName = name.getString();
        if (fullName.isEmpty()) return null;

        fullName = fullName.replaceAll("\u00a7[0-9a-fk-or]", "");

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
}
