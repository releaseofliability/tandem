package dev.exocutor.tandem.ui;

import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import dev.exocutor.tandem.*;
import dev.exocutor.tandem.api.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

public final class AccountScreen extends Screen {
    private final Screen parent;
    private EditBox address, name;
    private String serverText = "", nameText = "";
    private Component status = Tandem.takeStatus();
    private int page;
    private boolean busy;
    private boolean cancelled;
    private CompletableFuture<?> pending;

    public AccountScreen(Screen parent) {
        super(Component.literal("Tandem"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Tandem.trackCurrent();
        int w = Math.min(310, width - 20), left = (width - w) / 2;
        if (serverText.isBlank() && Tandem.active() != null && Tandem.active().server != null)
            serverText = Tandem.active().server.ip;
        address = addRenderableWidget(new EditBox(font, left, 36, w, 20, Component.translatable("tandem.server")));
        address.setHint(Component.translatable("tandem.server"));
        address.setMaxLength(255);
        address.setValue(serverText);
        address.setResponder(t -> serverText = t);
        name = addRenderableWidget(new EditBox(font, left, 64, w - 84, 20, Component.translatable("tandem.nickname")));
        name.setHint(Component.translatable("tandem.nickname"));
        name.setMaxLength(16);
        name.setValue(nameText);
        name.setResponder(t -> nameText = t);
        if (TandemApi.PROVIDERS.isEmpty())
            name.setTooltip(Tooltip.create(Component.translatable("tandem.ias_hint")));
        Button join = addRenderableWidget(Button.builder(Component.translatable("tandem.join"), b -> {
            try {
                User user = Tandem.offline(name.getValue());
                if (!Config.offlineNames.contains(user.getName())) {
                    Config.offlineNames.add(user.getName());
                    Config.save();
                }
                connect(user, ProfileKeyPairManager.EMPTY_KEY_MANAGER);
            } catch (RuntimeException ex) {
                status = Component.translatable(ex.getMessage());
            }
        }).bounds(left + w - 80, 64, 80, 20).build());
        join.active = !busy;
        List<RunnableRow> rows = new ArrayList<>();
        for (Session session : Tandem.sessions()) {
            rows.add(new RunnableRow(session.user.getName()
                    + (session.server == null ? "" : " — " + session.server.ip)
                    + (session == Tandem.active() ? " (" + Component.translatable("tandem.active").getString() + ")" : ""), () -> {
                Tandem.select(session);
                minecraft.gui.setScreen(null);
            }, () -> {
                Tandem.disconnect(session);
                rebuildWidgets();
            }));
        }
        User current = minecraft.getUser();
        var currentKeys = minecraft.getProfileKeyPairManager();
        rows.add(new RunnableRow(Component.translatable("tandem.connect_as", current.getName()).getString(),
                () -> connect(current, currentKeys), null));
        for (AccountProvider provider : TandemApi.PROVIDERS)
            for (AccountProvider.Account account : provider.accounts()) {
                rows.add(new RunnableRow(account.name() + "  [" + provider.id() + "]", () -> authenticate(provider, account), null));
            }
        for (String offline : Config.offlineNames) {
            rows.add(new RunnableRow(offline, () -> connect(Tandem.offline(offline), ProfileKeyPairManager.EMPTY_KEY_MANAGER), () -> {
                Config.offlineNames.remove(offline);
                Config.save();
                rebuildWidgets();
            }));
        }
        int perPage = Math.max(1, (height - 194) / 24), pages = Math.max(1, (rows.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < perPage && page * perPage + i < rows.size(); i++) {
            RunnableRow row = rows.get(page * perPage + i);
            int y = 96 + i * 24;
            Button button = addRenderableWidget(Button.builder(Component.literal(row.name), b -> row.action.run()).bounds(left, y, w - (row.remove == null ? 0 : 25), 20).build());
            button.active = !busy;
            if (row.remove != null)
                addRenderableWidget(Button.builder(Component.literal("×"), b -> row.remove.run()).bounds(left + w - 21, y, 21, 20).build());
        }
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page = Math.floorMod(page - 1, pages);
                rebuildWidgets();
            }).bounds(left, height - 72, 25, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page = (page + 1) % pages;
                rebuildWidgets();
            }).bounds(left + w - 25, height - 72, 25, 20).build());
        }
        if (TandemApi.PROVIDERS.stream().anyMatch(p -> p.id().equals("ias"))) {
            addRenderableWidget(Button.builder(Component.translatable("tandem.microsoft_accounts"), b -> {
                try {
                    Screen screen = (Screen) Class.forName("ru.vidtu.ias.screen.AccountScreen").getConstructor(Screen.class).newInstance(this);
                    minecraft.gui.setScreen(screen);
                } catch (ReflectiveOperationException ex) {
                    status = Component.translatable("tandem.auth_failed");
                }
            }).bounds(left + 29, height - 72, w - 58, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("tandem.animations", Component.translatable(Config.animations ? "options.on" : "options.off")), b -> {
            Config.animations = !Config.animations;
            Config.save();
            rebuildWidgets();
        }).bounds(left, height - 48, w / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + w / 2 + 2, height - 48, w / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("tandem.sponsor"),
                ConfirmLinkScreen.confirmLink(this, java.net.URI.create("https://launcher.ctf.mom")))
                .bounds(left, height - 24, w, 20).build());
    }

    private void authenticate(AccountProvider provider, AccountProvider.Account account) {
        if (busy) return;
        busy = true;
        status = Component.translatable("tandem.authenticating");
        rebuildWidgets();
        CompletableFuture<User> authentication = provider.authenticate(account);
        pending = authentication;
        authentication.thenApply(user -> {
            ProfileKeyPairManager keys = account.online() ? ProfileKeyPairManager.create(MinecraftServicesDiscoveryService.create(minecraft.getProxy()).createUserApiService(user.getAccessToken()), user, minecraft.gameDirectory.toPath()) : ProfileKeyPairManager.EMPTY_KEY_MANAGER;
            return new AbstractMap.SimpleEntry<>(user, keys);
        }).whenComplete((pair, error) -> minecraft.execute(() -> {
            if (cancelled) return;
            busy = false;
            if (error != null) {
                status = Component.translatable("tandem.auth_failed");
                rebuildWidgets();
            } else connect(pair.getKey(), pair.getValue());
        }));
    }

    private void connect(User user, ProfileKeyPairManager keys) {
        if (serverText.isBlank()) {
            status = Component.translatable("tandem.enter_server");
            return;
        }
        try {
            Tandem.connect(user, keys, new ServerData("Tandem", serverText, ServerData.Type.OTHER));
        } catch (RuntimeException ex) {
            status = Component.translatable(ex.getMessage());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        super.extractRenderState(g, x, y, delta);
        g.centeredText(font, title, width / 2, 16, -1);
        int contentWidth = Math.min(310, width - 20);
        g.textWithWordWrap(font, status, (width - contentWidth) / 2, height - 96, contentWidth, 0xffffaaaa);
    }

    @Override
    public void onClose() {
        cancelled = true;
        if (pending != null && !pending.isDone()) pending.cancel(false);
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record RunnableRow(String name, Runnable action, Runnable remove) {
    }
}
