package dev.exocutor.tandem.mixin;

import dev.exocutor.tandem.Tandem;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class BackgroundParticleMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void tandem$create(CallbackInfoReturnable<Particle> ci) {
        if (Tandem.inBackground()) ci.setReturnValue(null);
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void tandem$add(CallbackInfo ci) {
        if (Tandem.inBackground()) ci.cancel();
    }
}
