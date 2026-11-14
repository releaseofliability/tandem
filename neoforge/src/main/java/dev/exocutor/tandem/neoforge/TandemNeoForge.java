package dev.exocutor.tandem.neoforge;

import dev.exocutor.tandem.Tandem;
import dev.exocutor.tandem.ui.AccountScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value = "tandem", dist = Dist.CLIENT)
public final class TandemNeoForge {
    public TandemNeoForge(ModContainer container, IEventBus bus) {
        Tandem.init();
        bus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(Tandem.SWITCH_KEY);
            event.register(Tandem.CYCLE_KEY);
        });
        container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> new AccountScreen(parent));
    }
}
