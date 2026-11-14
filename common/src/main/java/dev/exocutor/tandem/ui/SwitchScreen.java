package dev.exocutor.tandem.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.exocutor.tandem.*;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;

public final class SwitchScreen extends Screen {
    private static final Identifier SLOT = Identifier.withDefaultNamespace("gamemode_switcher/slot");
    private static final Identifier SELECTION = Identifier.withDefaultNamespace("gamemode_switcher/selection");
    private final long opened = System.nanoTime();
    private final boolean holdDebug;
    private List<Session> entries = List.of();
    private int selected;
    private double selectionX;
    private long lastFrame = System.nanoTime();

    public SwitchScreen() {
        this(false);
    }

    public SwitchScreen(boolean holdDebug) {
        super(Component.translatable("tandem.accounts"));
        this.holdDebug = holdDebug;
    }

    @Override
    protected void init() {
        Tandem.trackCurrent();
        entries = Tandem.sessions().stream().filter(Session::ready).toList();
        if (!entries.isEmpty()) selected = Math.max(0, (entries.indexOf(Tandem.active()) + 1) % entries.size());
        selectionX = selected * 31;
        int left = width / 2 - (entries.size() * 31 - 5) / 2;
        for (int i = 0; i < entries.size(); i++) {
            final int index = i;
            Button button = Button.builder(Component.literal(entries.get(i).user.getName()), b -> {
                selected = index;
                commit();
            }).bounds(left + i * 31, height / 2 - 22, 26, 26).build();
            button.setAlpha(0);
            addWidget(button);
        }
        addRenderableWidget(Button.builder(Component.translatable("tandem.manage"), b -> minecraft.gui.setScreen(new AccountScreen(null))).bounds(width / 2 - 70, height / 2 + 58, 140, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) {
        int hintWidth = Math.max(font.width(Component.translatable("tandem.switch_hint", Tandem.SWITCH_KEY.getTranslatedKeyMessage())), font.width(Component.translatable("tandem.confirm_hint")));
        int span = Math.min(width - 12, Math.max(Math.max(125, entries.size() * 31 + 16), hintWidth + 24));
        if (!entries.isEmpty() && entries.get(selected).server != null)
            span = Math.max(span, Math.min(width - 12, font.width(entries.get(selected).server.ip) + 24));
        g.fill(width / 2 - span / 2, height / 2 - 64, width / 2 + (span + 1) / 2, height / 2 + 44, 0xc0101010);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        long now = System.nanoTime();
        double dt = Math.min(.05, (now - lastFrame) / 1e9);
        lastFrame = now;
        selectionX = Config.animations ? selectionX + (selected * 31 - selectionX) * (1 - Math.exp(-24 * dt)) : selected * 31;
        int rise = Config.animations ? (int) Math.round(5 * Math.exp(-(now - opened) / 70_000_000.0)) : 0;
        int left = width / 2 - (entries.size() * 31 - 5) / 2, top = height / 2 - 22 + rise;
        Component label = entries.isEmpty() ? Component.translatable("tandem.no_sessions") : Component.literal(entries.get(selected).user.getName());
        g.centeredText(font, label, width / 2, height / 2 - 54, -1);
        if (!entries.isEmpty() && entries.get(selected).server != null)
            g.centeredText(font, font.plainSubstrByWidth(entries.get(selected).server.ip, width - 36), width / 2, height / 2 - 41, 0xffaaaaaa);
        for (int i = 0; i < entries.size(); i++) {
            int sx = left + i * 31;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, sx, top, 26, 26);
            PlayerFaceExtractor.extractRenderState(g, ResolvableProfile.createUnresolved(entries.get(i).user.getProfileId()), sx + 5, top + 5, 16);
            if (entries.get(i) == Tandem.active()) g.fill(sx + 9, top + 27, sx + 17, top + 29, 0xff80c671);
        }
        if (!entries.isEmpty())
            g.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTION, left + (int) Math.round(selectionX), top, 26, 26);
        g.centeredText(font, Component.translatable("tandem.switch_hint", Tandem.SWITCH_KEY.getTranslatedKeyMessage()), width / 2, height / 2 + 18, 0xffcccccc);
        g.centeredText(font, Component.translatable("tandem.confirm_hint"), width / 2, height / 2 + 30, 0xffcccccc);
        super.extractRenderState(g, x, y, delta);
    }

    private void commit() {
        if (!entries.isEmpty()) Tandem.select(entries.get(selected));
        minecraft.gui.setScreen(null);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (Tandem.SWITCH_KEY.matches(event) || key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_LEFT) {
            if (!entries.isEmpty())
                selected = Math.floorMod(selected + (key == InputConstants.KEY_LEFT ? -1 : 1), entries.size());
            return true;
        }
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            commit();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (holdDebug && minecraft.options.keyDebugModifier.matches(event)) {
            commit();
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
