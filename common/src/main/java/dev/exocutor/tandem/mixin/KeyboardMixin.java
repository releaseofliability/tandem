package dev.exocutor.tandem.mixin;

import dev.exocutor.tandem.Tandem;
import dev.exocutor.tandem.ui.AccountScreen;
import dev.exocutor.tandem.ui.SwitchScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void tandem$key(long handle, int action, KeyEvent event, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (action != 1 || handle != mc.getWindow().handle()) return;
        if (Tandem.CYCLE_KEY.matches(event)) {
            if (Tandem.cycle()) ci.cancel();
            return;
        }
        if (!Tandem.SWITCH_KEY.matches(event) || mc.gui.screen() instanceof SwitchScreen) return;
        if (mc.gui.screen() != null) return;
        mc.gui.setScreen(mc.hasControlDown() ? new AccountScreen(null) : new SwitchScreen(mc.options.keyDebugModifier.isDown()));
        ci.cancel();
    }
}
