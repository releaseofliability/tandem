package dev.exocutor.tandem.ui;

import java.util.concurrent.CompletableFuture;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;

public final class PasswordScreen extends Screen {
    private final Screen parent;
    private final CompletableFuture<String> result;

    public PasswordScreen(Screen parent, CompletableFuture<String> result) {
        super(Component.translatable("tandem.vault_password"));
        this.parent = parent;
        this.result = result;
    }

    @Override
    protected void init() {
        EditBox field = addRenderableWidget(new EditBox(font, width / 2 - 100, height / 2 - 10, 200, 20, title));
        field.addFormatter((text, offset) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
        addRenderableWidget(Button.builder(Component.translatable("gui.continue"), b -> {
            result.complete(field.getValue());
            field.setValue("");
            minecraft.gui.setScreen(parent);
        }).bounds(width / 2 - 100, height / 2 + 20, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(width / 2 + 2, height / 2 + 20, 98, 20).build());
        setInitialFocus(field);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        super.extractRenderState(g, x, y, delta);
        g.centeredText(font, title, width / 2, height / 2 - 35, -1);
    }

    @Override
    public void onClose() {
        result.cancel(false);
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
