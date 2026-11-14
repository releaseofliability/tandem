package dev.exocutor.tandem;

import dev.exocutor.tandem.api.AccountProvider;
import dev.exocutor.tandem.api.TandemApi;
import dev.exocutor.tandem.ui.PasswordScreen;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.User;

// optional
public final class IasProvider implements AccountProvider {
    private final Class<?> accountClass;
    private final Class<?> handlerClass;
    private final Class<?> storageClass;

    private IasProvider() throws ClassNotFoundException {
        accountClass = Class.forName("ru.vidtu.ias.account.Account");
        handlerClass = Class.forName("ru.vidtu.ias.auth.handlers.LoginHandler");
        storageClass = Class.forName("ru.vidtu.ias.config.IASStorage");
    }

    public static void install() {
        try {
            TandemApi.register(new IasProvider());
        } catch (ClassNotFoundException ignored) {
        } catch (LinkageError | RuntimeException ex) {
            Tandem.LOG.warn("IAS integration unavailable: {}", ex.getClass().getSimpleName());
        }
    }

    @Override
    public String id() {
        return "ias";
    }

    private List<?> entries() throws ReflectiveOperationException {
        return (List<?>) storageClass.getField("ACCOUNTS").get(null);
    }

    @Override
    public List<Account> accounts() {
        try {
            List<Account> result = new ArrayList<>();
            for (Object item : entries())
                result.add(new Account(id(), (UUID) accountClass.getMethod("uuid").invoke(item),
                        (String) accountClass.getMethod("name").invoke(item), (boolean) accountClass.getMethod("canLogin").invoke(item)));
            return result;
        } catch (ReflectiveOperationException ex) {
            return List.of();
        }
    }

    @Override
    public CompletableFuture<User> authenticate(Account account) {
        CompletableFuture<User> result = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                Object entry = null;
                for (Object item : entries())
                    if (account.uuid().equals(accountClass.getMethod("uuid").invoke(item))) {
                        entry = item;
                        break;
                    }
                if (entry == null) throw new IllegalArgumentException("Account unavailable");
                Object handler = Proxy.newProxyInstance(handlerClass.getClassLoader(), new Class[]{handlerClass}, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "cancelled" -> result.isCancelled();
                        case "stage" -> null;
                        case "password" -> {
                            CompletableFuture<String> password = new CompletableFuture<>();
                            Tandem.client().execute(() -> Tandem.client().gui.setScreen(new PasswordScreen(Tandem.client().gui.screen(), password)));
                            yield password;
                        }
                        case "success" -> {
                            Object data = args[0];
                            Class<?> type = data.getClass();
                            if (Boolean.TRUE.equals(args[1])) {
                                try {
                                    Class.forName("ru.vidtu.ias.IAS").getMethod("saveStorage").invoke(null);
                                } catch (ReflectiveOperationException ex) {
                                    Tandem.LOG.warn("IAS account storage could not be saved: {}", ex.getClass().getSimpleName());
                                }
                            }
                            User user = new User((String) type.getMethod("name").invoke(data), (UUID) type.getMethod("uuid").invoke(data),
                                    (String) type.getMethod("token").invoke(data), Optional.empty(), Optional.empty());
                            result.complete(user);
                            yield null;
                        }
                        case "error" -> {
                            result.completeExceptionally((Throwable) args[0]);
                            yield null;
                        }
                        case "toString" -> "TandemLoginHandler";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                });
                accountClass.getMethod("login", handlerClass, Runnable.class).invoke(entry, handler, null);
            } catch (Exception ex) {
                result.completeExceptionally(ex);
            }
        });
        return result;
    }
}
