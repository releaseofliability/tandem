package dev.exocutor.tandem.fabric;

import dev.exocutor.tandem.Tandem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public final class TandemFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Tandem.init();
        KeyMappingHelper.registerKeyMapping(Tandem.SWITCH_KEY);
        KeyMappingHelper.registerKeyMapping(Tandem.CYCLE_KEY);
    }
}
