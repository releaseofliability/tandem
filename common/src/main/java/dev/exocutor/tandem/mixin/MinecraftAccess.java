package dev.exocutor.tandem.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.network.Connection;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccess {
    @Mutable
    @Accessor("user")
    void tandem$user(User value);

    @Mutable
    @Accessor("gui")
    void tandem$gui(Gui value);

    @Mutable
    @Accessor("profileKeyPairManager")
    void tandem$keys(ProfileKeyPairManager value);

    @Accessor("pendingConnection")
    void tandem$pending(Connection value);

    @Mutable
    @Accessor("levelRenderer")
    void tandem$renderer(LevelRenderer value);

    @Mutable
    @Accessor("levelExtractor")
    void tandem$extractor(LevelExtractor value);
}
