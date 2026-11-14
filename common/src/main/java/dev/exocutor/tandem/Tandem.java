package dev.exocutor.tandem;

import com.mojang.logging.LogUtils;
import dev.exocutor.tandem.api.TandemApi;
import dev.exocutor.tandem.mixin.*;
import dev.exocutor.tandem.ui.AccountScreen;
import dev.exocutor.tandem.ui.ConnectingScreen;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;

public final class Tandem {
    public static final Logger LOG = LogUtils.getLogger();
    public static final KeyMapping SWITCH_KEY = new KeyMapping("key.tandem.switch", InputConstants.KEY_F6,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("tandem", "players")));
    public static final KeyMapping CYCLE_KEY = new KeyMapping("key.tandem.cycle", InputConstants.KEY_Z, SWITCH_KEY.getCategory());
    private static final List<Session> SESSIONS = new ArrayList<>();
    private static final Map<Connection, Session> CONNECTIONS = new ConcurrentHashMap<>();
    private static Session active;
    private static Session context;
    private static boolean initialized;
    private static boolean clientInitialized;
    private static boolean closing;
    private static User launcherUser;
    private static ProfileKeyPairManager launcherKeys;
    private static RenderContext baseRender;
    private static Component lastStatus = Component.empty();

    public static void reportStatus(Component message) {
        lastStatus = message;
    }

    public static Component takeStatus() {
        Component message = lastStatus;
        lastStatus = Component.empty();
        return message;
    }

    public static Minecraft client() {
        return Minecraft.getInstance();
    }

    public static Session active() {
        return active;
    }

    public static List<Session> sessions() {
        return List.copyOf(SESSIONS);
    }

    public static Session forConnection(Connection connection) {
        return CONNECTIONS.get(connection);
    }

    public static boolean inBackground() {
        return context != null && context != active && client().isSameThread();
    }

    public static void init() {
        initialized = true;
    }

    private static void initializeClient() {
        if (clientInitialized) return;
        clientInitialized = true;
        Config.load();
        IasProvider.install();
        ServiceLoader.load(dev.exocutor.tandem.api.AccountProvider.class).forEach(TandemApi::register);
    }

    private static void capture(Session s) {
        Minecraft mc = client();
        s.level = mc.level;
        s.player = mc.player;
        s.gameMode = mc.gameMode;
        if (s.player != null) s.viewDistance = ((ClientListenerAccess)s.player.connection).tandem$viewDistance();
    }

    private static void bind(Session s) {
        Minecraft mc = client();
        MinecraftAccess a = (MinecraftAccess) mc;
        a.tandem$user(s.user);
        a.tandem$gui(s.gui);
        a.tandem$keys(s.keys);
        mc.level = s.level;
        mc.player = s.player;
        mc.gameMode = s.gameMode;
        s.render.bind(mc);
        mc.options.setServerRenderDistance(s.viewDistance);
        if (s.tags != null) s.tags.bind();
    }

    public static void withListener(PacketListener listener, Runnable task) {
        Connection conn = listener instanceof CommonListenerAccess c ? c.tandem$connection()
                : listener instanceof HandshakeAccess h ? h.tandem$connection() : null;
        Session session = conn == null ? null : forConnection(conn);
        if (session == null) task.run();
        else inSession(session, task);
    }

    public static void inSession(Session session, Runnable task) {
        if (session.closed) return;
        Session previous = context == null ? active : context;
        if (previous == session) {
            task.run();
            capture(session);
            return;
        }
        if (previous != null) capture(previous);
        Session oldContext = context;
        context = session;
        bind(session);
        try {
            task.run();
        } finally {
            capture(session);
            context = oldContext;
            if (previous != null) bind(previous);
        }
    }

    public static void trackCurrent() {
        Minecraft mc = client();
        if (active != null || mc.player == null || mc.getSingleplayerServer() != null) return;
        if (launcherUser == null) {
            launcherUser = mc.getUser();
            launcherKeys = mc.getProfileKeyPairManager();
        }
        if (baseRender == null) baseRender = RenderContext.current(mc);
        active = new Session(mc.getUser(), mc.gui, mc.getProfileKeyPairManager(), mc.getCurrentServer(), baseRender);
        active.connection = mc.player.connection.getConnection();
        capture(active);
        active.tags = RegistryTags.capture(mc.player.connection.registryAccess());
        SESSIONS.add(active);
        CONNECTIONS.put(active.connection, active);
    }

    public static User offline(String name) {
        if (!name.matches("[A-Za-z0-9_]{3,16}")) throw new IllegalArgumentException("tandem.invalid_name");
        UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        return new User(name, id, "tandem:offline", Optional.empty(), Optional.empty());
    }

    public static void connect(User user, ProfileKeyPairManager keys, ServerData server) {
        Minecraft mc = client();
        trackCurrent();
        if (mc.getSingleplayerServer() != null) throw new IllegalStateException("tandem.multiplayer_only");
        if (active != null && !active.ready()) throw new IllegalStateException("tandem.wait_for_player");
        if (SESSIONS.size() >= Config.maxSessions) throw new IllegalStateException("tandem.session_limit");
        if (SESSIONS.stream().anyMatch(s -> !s.closed && sameServer(s.server, server)
                && (s.user.getProfileId().equals(user.getProfileId()) || s.user.getName().equalsIgnoreCase(user.getName()))))
            throw new IllegalStateException("tandem.already_connected");
        if (launcherUser == null) {
            launcherUser = mc.getUser();
            launcherKeys = mc.getProfileKeyPairManager();
        }
        if (active != null) {
            capture(active);
            release(active);
        }
        Gui gui = new Gui(mc, new Hud(mc), ((GuiAccess) mc.gui).tandem$renderState());
        if (baseRender == null) baseRender = RenderContext.current(mc);
        Session s = new Session(user, gui, keys, server, RenderContext.create(mc));
        SESSIONS.add(s);
        active = s;
        bind(s);
        mc.setLevel(null);
        mc.gui.setScreen(new ConnectingScreen(s));
        mc.prepareForMultiplayer();
        Connection connection = new Connection(PacketFlow.CLIENTBOUND);
        s.connection = connection;
        CONNECTIONS.put(connection, s);
        ((MinecraftAccess) mc).tandem$pending(connection);
        if (SESSIONS.size() == 1) mc.getDownloadedPackSource().configureForServerControl(connection,
            switch (server.getResourcePackStatus()) {
                case ENABLED -> net.minecraft.client.resources.server.ServerPackManager.PackPromptStatus.ALLOWED;
                case DISABLED -> net.minecraft.client.resources.server.ServerPackManager.PackPromptStatus.DECLINED;
                case PROMPT -> net.minecraft.client.resources.server.ServerPackManager.PackPromptStatus.PENDING;
            });
        Util.ioPool().execute(() -> {
            try {
                ServerAddress host = ServerAddress.parseString(server.ip);
                var address = ServerNameResolver.DEFAULT.resolveAddress(host).orElseThrow().asInetSocketAddress();
                if (s.closed) return;
                Connection.connect(address, EventLoopGroupHolder.remote(mc.options.useNativeTransport()), connection).syncUninterruptibly();
                if (s.closed) {
                    connection.disconnect(Component.translatable("connect.aborted"));
                    return;
                }
                connection.initiateServerboundPlayConnection(host.getHost(), host.getPort(), LoginProtocols.SERVERBOUND, LoginProtocols.CLIENTBOUND,
                        new ClientHandshakePacketListenerImpl(connection, mc, server, new AccountScreen(null), false, null,
                                status -> s.status = status.getString(), new LevelLoadTracker(), null), false);
                connection.send(new ServerboundHelloPacket(user.getName(), user.getProfileId()));
            } catch (Exception ex) {
                mc.execute(() -> {
                    reportStatus(Component.translatable("tandem.connect_failed"));
                    disconnect(s);
                });
                LOG.warn("Tandem connection failed for {}: {}", user.getName(), ex.getClass().getSimpleName());
            }
        });
    }

    private static void release(Session session) {
        if (session.player != null) {
            session.player.input = new ClientInput();
            session.player.setSprinting(false);
            if (session.gameMode != null) {
                session.gameMode.stopDestroyBlock();
                session.gameMode.releaseUsingItem(session.player);
            }
        }
        KeyMapping.releaseAll();
    }

    private static boolean sameServer(ServerData first, ServerData second) {
        if (first == null || second == null) return first == second;
        ServerAddress a = ServerAddress.parseString(first.ip.trim());
        ServerAddress b = ServerAddress.parseString(second.ip.trim());
        return a.getPort() == b.getPort() && a.getHost().equalsIgnoreCase(b.getHost());
    }

    public static boolean cycle() {
        Minecraft mc = client();
        if (!mc.isSameThread() || mc.gui.screen() != null || mc.player == null) return false;
        trackCurrent();
        int start = SESSIONS.indexOf(active);
        if (start < 0) return false;
        for (int offset = 1; offset < SESSIONS.size(); offset++) {
            Session candidate = SESSIONS.get((start + offset) % SESSIONS.size());
            if (candidate.ready()) {
                select(candidate);
                return true;
            }
        }
        return false;
    }

    public static void select(Session target) {
        Minecraft mc = client();
        if (!mc.isSameThread()) throw new IllegalStateException("Session selection must run on the client thread");
        if (target == active || !target.ready()) return;
        if (active != null) {
            capture(active);
            release(active);
        }
        active = target;
        bind(target);
        mc.getSoundManager().stop();
        mc.particleEngine.setLevel(target.level);
        mc.gameRenderer.resetData();
        mc.gameRenderer.setLevel(target.level);
        target.render.resize(mc);
        mc.setCameraEntity(target.player);
        target.player.input = new KeyboardInput(mc.options);
        ((MinecraftAccess) mc).tandem$pending(null);
        mc.gui.setScreen(null);
        mc.updateTitle();
        for (var listener : TandemApi.SWITCH_LISTENERS) {
            try {
                listener.accept(target.id);
            } catch (RuntimeException ex) {
                LOG.warn("Tandem switch listener failed", ex);
            }
        }
    }

    public static void tick() {
        if (!initialized || closing || inBackground()) return;
        initializeClient();
        Minecraft mc = client();
        trackCurrent();
        if (active != null) capture(active);
        for (Session s : List.copyOf(SESSIONS)) {
            if (s == active || s.closed) continue;
            inSession(s, () -> {
                if (s.gameMode != null && s.player != null && s.level != null) {
                    if (s.level.hasChunkAt(s.player.blockPosition())) {
                        Runnable compiled = s.player.connection.getPlayerCompiledSectionCallback();
                        if (compiled != null) compiled.run();
                    }
                    s.gameMode.tick();
                    s.player.input = new ClientInput();
                    s.level.tickRateManager().tick();
                    s.level.update();
                    s.level.tickEntities();
                    s.level.tick(() -> true);
                    s.player.sendChanges();
                    s.player.connection.send(ServerboundClientTickEndPacket.INSTANCE);
                    s.gui.hud.tick(false);
                    s.gui.chatListener().tick();
                } else if (s.connection != null) s.connection.tick();
            });
            if (s.connection != null && !s.connection.isConnected()) disconnect(s);
        }
        if (active != null && active.ready() && mc.gui.screen() instanceof ConnectingScreen) mc.gui.setScreen(null);
    }

    public static void disconnect(Session session) {
        if (session.closed) return;
        session.closed = true;
        if (session.connection != null) {
            CONNECTIONS.remove(session.connection);
            session.connection.disconnect(Component.translatable("disconnect.quitting"));
        }
        SESSIONS.remove(session);
        if (session == active) {
            Session next = SESSIONS.stream().filter(Session::ready).findFirst().orElse(null);
            if (next != null) select(next);
            else {
                active = null;
                mcRestoreUser();
                if (baseRender != null) baseRender.bind(client());
                client().disconnectWithProgressScreen();
                client().gui.setScreen(new AccountScreen(null));
            }
        }
        session.render.close();
    }

    private static void mcRestoreUser() {
        if (launcherUser != null) {
            ((MinecraftAccess) client()).tandem$user(launcherUser);
            ((MinecraftAccess) client()).tandem$keys(launcherKeys);
        }
    }

    public static void closeAll() {
        if (closing) return;
        closing = true;
        try {
            if (baseRender != null) baseRender.bind(client());
            for (Session s : SESSIONS) {
                s.closed = true;
                if (s.connection != null) s.connection.disconnect(Component.translatable("disconnect.quitting"));
                s.render.close();
            }
            SESSIONS.clear();
            CONNECTIONS.clear();
            active = null;
            context = null;
            mcRestoreUser();
        } finally {
            closing = false;
        }
    }

    public static void reloadRenderContexts() {
        if (!clientInitialized) return;
        for (Session session : SESSIONS) {
            session.render.extractor.onResourceManagerReload(client().getResourceManager());
            session.render.extractor.allChanged();
        }
    }
}
