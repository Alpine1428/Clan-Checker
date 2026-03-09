package com.clanchecker.mixin;

import com.clanchecker.ClanCheckerHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericContainerScreen.class)
public abstract class GenericContainerScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void clanchecker_onRender(DrawContext drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GenericContainerScreen self = (GenericContainerScreen) (Object) this;
        ClanCheckerHud.renderOverlay(drawContext, self, mouseX, mouseY, delta);
    }
}
