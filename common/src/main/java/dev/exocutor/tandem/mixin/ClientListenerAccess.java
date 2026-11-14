package dev.exocutor.tandem.mixin;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ClientPacketListener.class)
public interface ClientListenerAccess {
    @Accessor("serverChunkRadius") int tandem$viewDistance();
}
