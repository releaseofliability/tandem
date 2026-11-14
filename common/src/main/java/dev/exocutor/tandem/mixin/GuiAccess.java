package dev.exocutor.tandem.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiAccess {
    @Accessor("guiRenderState")
    GuiRenderState tandem$renderState();
}
