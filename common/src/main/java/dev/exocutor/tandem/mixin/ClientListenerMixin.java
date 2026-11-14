package dev.exocutor.tandem.mixin;

import dev.exocutor.tandem.RegistryTags;
import dev.exocutor.tandem.Session;
import dev.exocutor.tandem.Tandem;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientListenerMixin {
    @Inject(method = {"<init>", "handleUpdateTags"}, at = @At("RETURN"))
    private void tandem$tags(CallbackInfo ci) {
        ClientPacketListener listener = (ClientPacketListener)(Object)this;
        Session session = Tandem.forConnection(listener.getConnection());
        if (session != null) session.tags = RegistryTags.capture(listener.registryAccess());
    }
}
