package dev.exocutor.tandem.mixin;

import dev.exocutor.tandem.Tandem;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class BackgroundSoundMixin {
    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void tandem$sound(CallbackInfoReturnable<SoundEngine.PlayResult> ci) {
        if (Tandem.inBackground()) ci.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
    }
}
