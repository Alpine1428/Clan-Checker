package com.clanchecker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClanCheckerMod implements ClientModInitializer {

    public static final String MOD_ID = "clanchecker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static KeyBinding scanKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[ClanChecker] Мод загружен!");

        scanKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.clanchecker.scan",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.clanchecker"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (scanKey.wasPressed()) {
                ClanScanManager.getInstance().startScan(client);
            }
            ClanScanManager.getInstance().tick(client);
        });

        LOGGER.info("[ClanChecker] Клавиша сканирования: R");
    }
}
