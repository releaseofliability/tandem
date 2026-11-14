package dev.exocutor.tandem.mixin;

import dev.exocutor.tandem.Tandem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void tandem$tick(CallbackInfo ci) {
        Tandem.tick();
    }

    @Inject(method = "setLevel", at = @At("HEAD"), cancellable = true)
    private void tandem$backgroundLevel(ClientLevel level, CallbackInfo ci) {
        if (Tandem.inBackground()) {
            Minecraft mc = (Minecraft) (Object) this;
            mc.level = level;
            mc.levelExtractor.setLevel(level);
            ci.cancel();
        }
    }

    @Inject(method = "setCameraEntity", at = @At("HEAD"), cancellable = true)
    private void tandem$camera(CallbackInfo ci) {
        if (Tandem.inBackground()) ci.cancel();
    }

    @Inject(method = "clearDownloadedResourcePacks", at = @At("HEAD"), cancellable = true)
    private void tandem$packs(CallbackInfo ci) {
        if (Tandem.inBackground()) ci.cancel();
    }

    @Inject(method = "setScreenAndShow", at = @At("HEAD"), cancellable = true)
    private void tandem$backgroundScreen(CallbackInfo ci) {
        if (Tandem.inBackground()) ci.cancel();
    }

    @Inject(method = "clearClientLevel", at = @At("HEAD"), cancellable = true)
    private void tandem$clearBackground(CallbackInfo ci) {
        if (Tandem.inBackground()) {
            Minecraft mc = (Minecraft) (Object) this;
            mc.level = null;
            mc.player = null;
            mc.gameMode = null;
            ci.cancel();
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"), cancellable = true)
    private void tandem$disconnect(CallbackInfo ci) {
        if (Tandem.inBackground()) {
            ci.cancel();
            return;
        }
        Tandem.closeAll();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void tandem$close(CallbackInfo ci) {
        Tandem.closeAll();
    }

    @Inject(method = "onResourceLoadFinished", at = @At("TAIL"))
    private void tandem$reload(CallbackInfo ci) {
        Tandem.reloadRenderContexts();
    }
}
