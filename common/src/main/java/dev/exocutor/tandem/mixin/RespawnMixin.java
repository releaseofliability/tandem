package dev.exocutor.tandem.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.exocutor.tandem.Tandem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPacketListener.class)
public class RespawnMixin {
    @WrapMethod(method = "handleRespawn")
    private void tandem$keepExperience(ClientboundRespawnPacket packet, Operation<Void> original) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread() || Tandem.forConnection(((CommonListenerAccess) this).tandem$connection()) == null) {
            original.call(packet);
            return;
        }
        LocalPlayer previous = mc.player;
        original.call(packet);
        if (packet.shouldKeep((byte) 2) && previous != null && mc.player != null && previous != mc.player)
            mc.player.setExperienceValues(previous.experienceProgress, previous.totalExperience, previous.experienceLevel);
    }
}
