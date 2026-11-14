package dev.exocutor.tandem.mixin;

import dev.exocutor.tandem.Tandem;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class BackgroundGuiMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void tandem$screen(CallbackInfo ci) {
        if (Tandem.inBackground()) ci.cancel();
    }
}
