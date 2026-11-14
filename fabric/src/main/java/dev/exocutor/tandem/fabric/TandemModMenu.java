package dev.exocutor.tandem.fabric;

import com.terraformersmc.modmenu.api.*;
import dev.exocutor.tandem.ui.AccountScreen;

public final class TandemModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AccountScreen::new;
    }
}
