package dev.exocutor.tandem.mixin;

import dev.exocutor.tandem.Session;
import dev.exocutor.tandem.Tandem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientHandshakePacketListenerImpl.class)
public class HandshakeMixin {
    @Redirect(method = "authenticateServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getUser()Lnet/minecraft/client/User;"))
    private User tandem$authIdentity(Minecraft mc) {
        Session session = Tandem.forConnection(((HandshakeAccess) this).tandem$connection());
        return session == null ? mc.getUser() : session.user;
    }
}
