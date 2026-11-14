package dev.exocutor.tandem;

import java.util.UUID;

import net.minecraft.client.User;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;

public final class Session {
    public final UUID id = UUID.randomUUID();
    public final User user;
    public final Gui gui;
    public final ProfileKeyPairManager keys;
    public final ServerData server;
    public final RenderContext render;
    public volatile Connection connection;
    public ClientLevel level;
    public LocalPlayer player;
    public MultiPlayerGameMode gameMode;
    public RegistryTags tags;
    public int viewDistance = 3;
    public String status = "";
    public boolean closed;

    public Session(User user, Gui gui, ProfileKeyPairManager keys, ServerData server, RenderContext render) {
        this.user = user;
        this.gui = gui;
        this.keys = keys;
        this.server = server;
        this.render = render;
    }

    public boolean ready() {
        return !closed && player != null && level != null && connection != null && connection.isConnected() && player.connection.hasClientLoaded();
    }

    @Override
    public String toString() {
        return "Session[" + user.getName() + ", " + id + "]";
    }
}
