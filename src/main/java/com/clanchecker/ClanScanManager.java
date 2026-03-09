package com.clanchecker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final Map<Integer, String> slotCreatorMap = new HashMap<>();

    public static class ScannedClan {
        public final String name;
        public final String creator;
        public final int slot;
        public final boolean hasViolation;

        public ScannedClan(String name, String creator, int slot, boolean hasViolation) {
            this.name = name;
            this.creator = creator;
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
            waitTicks = 15;
            violations.clear();
            allClans.clear();
            slotCreatorMap.clear();
            scanComplete = false;
            selectedViolationIndex = -1;
            hoverSlot = -1;

            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal("\u00a7e[ClanChecker] \u00a7fScanning..."), false);
            }
        } else {
            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal("\u00a7c[ClanChecker] \u00a7fOpen \u00a7e/clan list \u00a7ffirst!"), false);
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

        LOGGER.info("[ClanChecker] === SCAN START ===");
        LOGGER.info("[ClanChecker] Total slots: {}, Container slots: {}, Rows: {}",
                totalSlots, containerSlots, handler.getRows());

        int clansChecked = 0;

        for (int i = 0; i < containerSlots && i < totalSlots; i++) {
            Slot slot = handler.getSlot(i);
            ItemStack stack = slot.getStack();

            if (stack.isEmpty()) continue;
            if (isDecorativeItem(stack)) {
                LOGGER.debug("[ClanChecker] Slot {}: SKIP decorative", i);
                continue;
            }

            String clanName = extractClanName(stack);
            if (clanName == null || clanName.isEmpty()) {
                LOGGER.debug("[ClanChecker] Slot {}: SKIP no name from '{}'",
                    i, stack.getName().getString());
                continue;
            }
            if (clanName.length() < 2) continue;

            String creator = extractCreator(stack);
            if (creator != null) {
                slotCreatorMap.put(i, creator);
            }

            clansChecked++;

            String normalized = ViolationDatabase.normalize(clanName);
            LOGGER.info("[ClanChecker] Slot {}: '{}' -> norm: '{}' | creator: '{}'",
                    i, clanName, normalized, creator != null ? creator : "?");

            List<ViolationDatabase.ViolationResult> results =
                ViolationDatabase.checkClanName(clanName, i);
            violations.addAll(results);
            allClans.add(new ScannedClan(clanName, creator, i, !results.isEmpty()));

            if (!results.isEmpty()) {
                LOGGER.warn("[ClanChecker] VIOLATION slot {}: '{}' -> {} ({})",
                    i, clanName, results.get(0).category, results.get(0).matchedWord);
            }
        }

        scanComplete = true;
        state = ScanState.DONE;

        if (client.player != null) {
            if (violations.isEmpty()) {
                client.player.sendMessage(Text.literal(
                    "\u00a7a[ClanChecker] \u00a7fNo violations! \u00a77(" +
                    clansChecked + " clans)"), false);
            } else {
                client.player.sendMessage(Text.literal(
                    "\u00a7c[ClanChecker] \u00a7fViolations: \u00a7c" + violations.size() +
                    " \u00a77(" + clansChecked + " clans)"), false);
                for (ViolationDatabase.ViolationResult v : violations) {
                    String creatorInfo = slotCreatorMap.containsKey(v.slot)
                        ? " \u00a77by \u00a7b" + slotCreatorMap.get(v.slot) : "";
                    client.player.sendMessage(Text.literal(
                        "  \u00a7e> \u00a7f" + v.clanName + " \u00a77-> \u00a7c" +
                        v.category + " \u00a77(\"" + v.matchedWord + "\")" +
                        creatorInfo + " \u00a78[slot " + v.slot + "]"), false);
                }
                client.player.sendMessage(Text.literal(
                    "\u00a77  LMB on clan slot to copy creator name"), false);
            }
        }
    }

    private boolean isDecorativeItem(ItemStack stack) {
        String itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();
        if (itemId.contains("glass_pane")) return true;
        if (stack.getItem() == Items.BARRIER) return true;
        if (stack.getItem() == Items.ARROW) return true;
        if (itemId.contains("_dye")) return true;
        if (itemId.contains("_sign")) return true;
        if (itemId.contains("_bed") && !stack.hasCustomName()) return true;
        if (itemId.contains("_wool") && !stack.hasCustomName()) return true;
        if (itemId.contains("_concrete") && !stack.hasCustomName()) return true;
        return false;
    }

    private String extractClanName(ItemStack stack) {
        Text name = stack.getName();
        if (name == null) return null;

        String fullName = name.getString();
        if (fullName.isEmpty()) return null;

        fullName = fullName.replaceAll("\u00a7[0-9a-fk-orA-FK-OR]", "");
        fullName = fullName.trim();
        if (fullName.isEmpty()) return null;

        if (fullName.contains("|")) {
            fullName = fullName.split("\\|")[0].trim();
        }
        if (fullName.contains("(")) {
            fullName = fullName.split("\\(")[0].trim();
        }
        if (fullName.startsWith("[") && fullName.contains("]")) {
            int bracketEnd = fullName.indexOf(']');
            String tag = fullName.substring(1, bracketEnd).trim();
            String rest = fullName.substring(bracketEnd + 1).trim();
            fullName = rest.isEmpty() ? tag : rest;
        }

        String lowerFull = fullName.toLowerCase();
        if (lowerFull.startsWith("\u043a\u043b\u0430\u043d:") || lowerFull.startsWith("clan:")) {
            fullName = fullName.substring(fullName.indexOf(':') + 1).trim();
        }

        fullName = fullName.replaceAll("^[\\u25ba\\u25b6\\u25cf\\u25cb\\u25c6\\u25c7\\u25a0\\u25a1\\u25aa\\u25ab\\u2022\\u2023\\u2043\\u2192\\u21d2\\u00bb\\u00ab\\u203b\\u2726\\u2727\\u2605\\u2606\\u2730\\u272a\\u2756]+\\s*", "");

        return fullName.trim();
    }

    private String extractCreator(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return null;

        if (!nbt.contains("display", NbtElement.COMPOUND_TYPE)) return null;
        NbtCompound display = nbt.getCompound("display");
        if (!display.contains("Lore", NbtElement.LIST_TYPE)) return null;

        NbtList lore = display.getList("Lore", NbtElement.STRING_TYPE);

        for (int i = 0; i < lore.size(); i++) {
            String loreLine = lore.getString(i);
            String plainText;
            try {
                Text text = Text.Serializer.fromJson(loreLine);
                plainText = text != null ? text.getString() : loreLine;
            } catch (Exception e) {
                plainText = loreLine;
            }

            plainText = plainText.replaceAll("\u00a7[0-9a-fk-orA-FK-OR]", "").trim();
            String lower = plainText.toLowerCase();

            String[] patterns = {
                "\u0441\u043e\u0437\u0434\u0430\u0442\u0435\u043b\u044c:",
                "\u0441\u043e\u0437\u0434\u0430\u0442\u0435\u043b\u044c ",
                "creator:", "creator ",
                "\u043b\u0438\u0434\u0435\u0440:", "\u043b\u0438\u0434\u0435\u0440 ",
                "leader:", "leader ",
                "\u0432\u043b\u0430\u0434\u0435\u043b\u0435\u0446:",
                "\u0432\u043b\u0430\u0434\u0435\u043b\u0435\u0446 ",
                "owner:", "owner ",
                "\u0433\u043b\u0430\u0432\u0430:", "\u0433\u043b\u0430\u0432\u0430 ",
                "head:", "head ",
                "\u043e\u0441\u043d\u043e\u0432\u0430\u0442\u0435\u043b\u044c:",
                "\u043e\u0441\u043d\u043e\u0432\u0430\u0442\u0435\u043b\u044c ",
                "founder:", "founder ",
                "sozdatel:", "sozdatel ",
                "lider:", "lider "
            };

            for (String pattern : patterns) {
                int idx = lower.indexOf(pattern.toLowerCase());
                if (idx >= 0) {
                    String after = plainText.substring(idx + pattern.length()).trim();
                    String creatorName = after.split("[\\s,;|]")[0].trim();
                    if (!creatorName.isEmpty()) return creatorName;
                }
            }
        }
        return null;
    }

    public String getCreatorForSlot(int slotIndex) {
        return slotCreatorMap.get(slotIndex);
    }

    public String extractCreatorFromScreen(MinecraftClient client, int slotIndex) {
        if (!(client.currentScreen instanceof GenericContainerScreen containerScreen)) return null;
        GenericContainerScreenHandler handler = containerScreen.getScreenHandler();
        if (slotIndex >= handler.slots.size()) return null;
        ItemStack stack = handler.getSlot(slotIndex).getStack();
        if (stack.isEmpty()) return null;

        String cached = slotCreatorMap.get(slotIndex);
        if (cached != null) return cached;

        String extracted = extractCreator(stack);
        if (extracted != null) slotCreatorMap.put(slotIndex, extracted);
        return extracted;
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
        slotCreatorMap.clear();
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
