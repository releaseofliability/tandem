package dev.exocutor.tandem;

import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;

/** Binds the server's tag memberships for registries shared by the game. */
public final class RegistryTags {
    private static RegistryTags installed;
    private final Map<Registry<?>, Entry> entries;

    private record Entry(Map<?, ?> values, Registry.PendingTags<?> pending) {}
    private RegistryTags(Map<Registry<?>, Entry> entries) { this.entries = Collections.unmodifiableMap(entries); }

    public static RegistryTags capture(RegistryAccess access) {
        Map<Registry<?>, Entry> entries = new IdentityHashMap<>();
        access.registries().forEach(entry -> entries.put(entry.value(), snapshot(entry.value())));
        RegistryTags result = new RegistryTags(entries);
        installed = result;
        return result;
    }

    private static <T> Entry snapshot(Registry<T> registry) {
        Map<TagKey<T>, List<Holder<T>>> tags = new LinkedHashMap<>();
        registry.getTags().forEach(tag -> tags.put(tag.key(), tag.stream().toList()));
        return new Entry(Collections.unmodifiableMap(tags), registry.prepareTagReload(new TagLoader.LoadResult<>(registry.key(), tags)));
    }

    public void bind() {
        if (installed == this) return;
        entries.forEach((registry, entry) -> {
            Entry previous = installed == null ? null : installed.entries.get(registry);
            if (previous != null && !entry.values.equals(previous.values)) entry.pending.apply();
        });
        installed = this;
    }
}
