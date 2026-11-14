package dev.exocutor.tandem.mixin;

import java.util.Set;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Holder.Reference.class)
public interface HolderTagsAccess {
    @Accessor("tags")
    Set<?> tandem$tags();

    @Accessor("tags")
    void tandem$tags(Set<?> tags);
}
