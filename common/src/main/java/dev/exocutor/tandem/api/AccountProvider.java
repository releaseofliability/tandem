package dev.exocutor.tandem.api;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.User;

public interface AccountProvider {
    String id();

    List<Account> accounts();

    CompletableFuture<User> authenticate(Account account);

    record Account(String provider, UUID uuid, String name, boolean online) {
    }
}
