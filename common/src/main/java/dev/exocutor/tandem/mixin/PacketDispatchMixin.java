package dev.exocutor.tandem.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.exocutor.tandem.Tandem;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
public class PacketDispatchMixin {
    @WrapOperation(method = "handle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"))
    private void tandem$dispatch(Packet<?> packet, PacketListener listener, Operation<Void> original) {
        Tandem.withListener(listener, () -> original.call(packet, listener));
    }
}
