package dev.exocutor.tandem.mixin;

import dev.exocutor.tandem.RegistryTags;
import net.minecraft.core.MappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MappedRegistry.class)
public class RegistryTagsMixin {
    @Inject(method = "refreshTagsInHolders", at = @At("HEAD"), cancellable = true)
    private void tandem$cachedMemberships(CallbackInfo ci) {
        if (RegistryTags.restoring()) ci.cancel();
    }
}
