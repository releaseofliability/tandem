package dev.exocutor.tandem.mixin;

import dev.exocutor.tandem.*;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class DisconnectMixin {
    @Inject(method = "onDisconnect", at = @At("HEAD"), cancellable = true)
    private void tandem$disconnect(DisconnectionDetails details, CallbackInfo ci) {
        Session session = Tandem.forConnection(((CommonListenerAccess) this).tandem$connection());
        if (session != null) {
            session.status = details.reason().getString();
            Tandem.reportStatus(details.reason());
            Tandem.client().execute(() -> Tandem.disconnect(session));
            ci.cancel();
        }
    }
}
