package com.clanchecker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Менеджер сканирования кланов.
 * Считывает предметы из слотов инвентаря /clan list и проверяет их названия.
 */
public class ClanScanManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("ClanChecker");
    private static ClanScanManager instance;

    public enum ScanState {
        IDLE,
        SCANNING,
        DONE
    }

    private ScanState state = ScanState.IDLE;
    private final List<ViolationDatabase.ViolationResult> violations = new ArrayList<>();
    private boolean scanComplete = false;
    private int selectedViolationIndex = -1;
    private int hoverSlot = -1;

    // Слоты, которые содержат кланы (10-36, исключая 10, 18, 19, 27, 28, 36)
    private static final Set<Integer> CLAN_SLOTS = new HashSet<>();

    static {
        // Слоты с 10 по 36, исключая 10, 18, 19, 27, 28, 36
        for (int i = 10; i <= 36; i++) {
            CLAN_SLOTS.add(i);
        }
        CLAN_SLOTS.remove(10);
        CLAN_SLOTS.remove(18);
        CLAN_SLOTS.remove(19);
        CLAN_SLOTS.remove(27);
        CLAN_SLOTS.remove(28);
        CLAN_SLOTS.remove(36);
    }

    public static ClanScanManager getInstance() {
        if (instance == null) {
            instance = new ClanScanManager();
        }
        return instance;
    }

    /**
     * Запускает сканирование при нажатии клавиши.
     */
    public void startScan(MinecraftClient client) {
        if (client.currentScreen instanceof GenericContainerScreen) {
            LOGGER.info("[ClanChecker] Начинаем сканирование кланов...");
            state = ScanState.SCANNING;
            violations.clear();
            scanComplete = false;
            selectedViolationIndex = -1;
            hoverSlot = -1;
            performScan(client);
        } else {
            LOGGER.warn("[ClanChecker] Откройте меню /clan list перед сканированием!");
            // Отправляем сообщение в чат
            if (client.player != null) {
                client.player.sendMessage(
                        Text.literal("§c[ClanChecker] §fОткройте меню §e/clan list §fперед сканированием!"),
                        false
                );
            }
        }
    }

    /**
     * Выполняет сканирование всех слотов в открытом инвентаре.
     */
    private void performScan(MinecraftClient client) {
        if (!(client.currentScreen instanceof GenericContainerScreen containerScreen)) {
            state = ScanState.IDLE;
            return;
        }

        GenericContainerScreenHandler handler = containerScreen.getScreenHandler();
        int totalSlots = handler.slots.size();

        int clansChecked = 0;

        for (int slotIndex : CLAN_SLOTS) {
            if (slotIndex >= totalSlots) continue;

            Slot slot = handler.getSlot(slotIndex);
            ItemStack stack = slot.getStack();

            if (stack.isEmpty()) continue;

            // Получаем название предмета (название клана)
            String clanName = extractClanName(stack);
            if (clanName == null || clanName.isEmpty()) continue;

            clansChecked++;

            // Проверяем название
            List<ViolationDatabase.ViolationResult> results = ViolationDatabase.checkClanName(clanName, slotIndex);
            violations.addAll(results);

            LOGGER.info("[ClanChecker] Проверен клан '{}' в слоте {} — нарушений: {}",
                    clanName, slotIndex, results.size());
        }

        scanComplete = true;
        state = ScanState.DONE;

        LOGGER.info("[ClanChecker] Сканирование завершено. Проверено кланов: {}, Нарушений: {}",
                clansChecked, violations.size());

        if (client.player != null) {
            if (violations.isEmpty()) {
                client.player.sendMessage(
                        Text.literal("§a[ClanChecker] §fСканирование завершено. §aНарушений не обнаружено! §7(Проверено: " + clansChecked + ")"),
                        false
                );
            } else {
                client.player.sendMessage(
                        Text.literal("§c[ClanChecker] §fСканирование завершено. §cНайдено нарушений: " + violations.size() + " §7(Проверено: " + clansChecked + ")"),
                        false
                );
                for (ViolationDatabase.ViolationResult v : violations) {
                    client.player.sendMessage(
                            Text.literal("  §e" + v.clanName + " §7— §c" + v.category + " §7(\"" + v.matchedWord + "\")"),
                            false
                    );
                }
            }
        }
    }

    /**
     * Извлекает название клана из ItemStack.
     * Формат: "clanname | 1 уровень" — берём часть до |
     */
    private String extractClanName(ItemStack stack) {
        if (!stack.hasCustomName()) {
            Text name = stack.getName();
            if (name != null) {
                String fullName = name.getString();
                // Убираем форматирование
                fullName = fullName.replaceAll("§.", "");
                // Если есть разделитель |, берём часть до него
                if (fullName.contains("|")) {
                    return fullName.split("\\|")[0].trim();
                }
                return fullName.trim();
            }
            return null;
        }

        Text customName = stack.getName();
        if (customName != null) {
            String fullName = customName.getString();
            fullName = fullName.replaceAll("§.", "");
            if (fullName.contains("|")) {
                return fullName.split("\\|")[0].trim();
            }
            return fullName.trim();
        }
        return null;
    }

    /**
     * Тик — обрабатывается каждый клиентский тик.
     */
    public void tick(MinecraftClient client) {
        // Если экран закрыт — сбрасываем состояние
        if (!(client.currentScreen instanceof GenericContainerScreen)) {
            if (state == ScanState.DONE) {
                // Оставляем результаты доступными даже после закрытия
            }
        }
    }

    /**
     * Выбирает нарушение по индексу и устанавливает слот для наведения.
     */
    public void selectViolation(int index) {
        if (index >= 0 && index < violations.size()) {
            selectedViolationIndex = index;
            hoverSlot = violations.get(index).slot;
        }
    }

    /**
     * Выбирает следующее нарушение.
     */
    public void selectNext() {
        if (violations.isEmpty()) return;
        selectedViolationIndex = (selectedViolationIndex + 1) % violations.size();
        hoverSlot = violations.get(selectedViolationIndex).slot;
    }

    /**
     * Выбирает предыдущее нарушение.
     */
    public void selectPrevious() {
        if (violations.isEmpty()) return;
        selectedViolationIndex--;
        if (selectedViolationIndex < 0) selectedViolationIndex = violations.size() - 1;
        hoverSlot = violations.get(selectedViolationIndex).slot;
    }

    /**
     * Сбрасывает сканирование.
     */
    public void reset() {
        state = ScanState.IDLE;
        violations.clear();
        scanComplete = false;
        selectedViolationIndex = -1;
        hoverSlot = -1;
    }

    // --- Getters ---

    public ScanState getState() {
        return state;
    }

    public List<ViolationDatabase.ViolationResult> getViolations() {
        return violations;
    }

    public boolean isScanComplete() {
        return scanComplete;
    }

    public int getSelectedViolationIndex() {
        return selectedViolationIndex;
    }

    public int getHoverSlot() {
        return hoverSlot;
    }
}
