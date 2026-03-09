package com.clanchecker.mixin;

import com.clanchecker.ClanCheckerHud;
import com.clanchecker.ClanScanManager;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void clanchecker_onMouseClicked(double mouseX, double mouseY, int button,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof GenericContainerScreen screen)) return;
        if (button != 0) return;

        ClanScanManager manager = ClanScanManager.getInstance();
        if (!manager.isScanComplete()) return;

        if (ClanCheckerHud.handlePanelClick(mouseX, mouseY)) {
            cir.setReturnValue(true);
            return;
        }

        if (ClanCheckerHud.handleSlotClick(screen, mouseX, mouseY)) {
            cir.setReturnValue(true);
            return;
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void clanchecker_onKeyPressed(int keyCode, int scanCode, int modifiers,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof GenericContainerScreen)) return;
        ClanScanManager manager = ClanScanManager.getInstance();
        if (manager.isScanComplete() && !manager.getViolations().isEmpty()) {
            if (keyCode == GLFW.GLFW_KEY_UP) {
                manager.selectPrevious();
                cir.setReturnValue(true);
            } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
                manager.selectNext();
                cir.setReturnValue(true);
            }
        }
    }
}
