package dev.exocutor.tandem.api;

import dev.exocutor.tandem.Tandem;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class TandemApi {
    public static final List<AccountProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    public static final List<Consumer<UUID>> SWITCH_LISTENERS = new CopyOnWriteArrayList<>();
    private TandemApi() {
    }

    public static void register(AccountProvider provider) {
        if (PROVIDERS.stream().anyMatch(p -> p.id().equals(provider.id())))
            throw new IllegalArgumentException("Duplicate account provider: " + provider.id());
        PROVIDERS.add(provider);
    }

    public static List<SessionInfo> sessions() {
        return Tandem.sessions().stream().map(s -> new SessionInfo(s.id, s.user.getName(), s.ready(), s == Tandem.active())).toList();
    }

    public static void select(UUID sessionId) {
        Tandem.client().execute(() -> Tandem.sessions().stream().filter(s -> s.id.equals(sessionId)).findFirst().ifPresent(Tandem::select));
    }

    public record SessionInfo(UUID id, String name, boolean ready, boolean active) {
    }
}
