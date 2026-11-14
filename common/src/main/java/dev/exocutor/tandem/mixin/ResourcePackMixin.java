package dev.exocutor.tandem.mixin;

import dev.exocutor.tandem.Session;
import dev.exocutor.tandem.Tandem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ResourcePackMixin {
    @Inject(method = "handleResourcePackPush", at = @At("HEAD"), cancellable = true)
    private void tandem$push(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        ClientCommonPacketListenerImpl listener = (ClientCommonPacketListenerImpl)(Object)this;
        Session session = Tandem.forConnection(((CommonListenerAccess)this).tandem$connection());
        if (session == null) return;
        PacketUtils.ensureRunningOnSameThread(packet, listener, Minecraft.getInstance().packetProcessor());
        if (Tandem.sessions().size() < 2) return;
        session.connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.DECLINED));
        if (packet.required()) {
            Component reason = Component.translatable("tandem.required_pack");
            Tandem.reportStatus(reason);
            session.connection.disconnect(reason);
        }
        ci.cancel();
    }

    @Inject(method = "handleResourcePackPop", at = @At("HEAD"), cancellable = true)
    private void tandem$pop(ClientboundResourcePackPopPacket packet, CallbackInfo ci) {
        Session session = Tandem.forConnection(((CommonListenerAccess)this).tandem$connection());
        if (session == null) return;
        PacketUtils.ensureRunningOnSameThread(packet, (ClientCommonPacketListenerImpl)(Object)this, Minecraft.getInstance().packetProcessor());
        if (Tandem.sessions().size() > 1) ci.cancel();
    }
}
