package dev.exocutor.tandem.ui;

import dev.exocutor.tandem.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConnectingScreen extends Screen {
    private final Session session;

    public ConnectingScreen(Session session) {
        super(Component.translatable("connect.connecting"));
        this.session = session;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> Tandem.disconnect(session)).bounds(width / 2 - 70, height / 2 + 25, 140, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        super.extractRenderState(g, x, y, delta);
        g.centeredText(font, Component.translatable("tandem.connecting", session.user.getName()), width / 2, height / 2 - 25, -1);
        g.centeredText(font, session.status, width / 2, height / 2 - 8, 0xffaaaaaa);
    }

    @Override
    public void onClose() {
        Tandem.disconnect(session);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
