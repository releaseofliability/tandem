package dev.exocutor.tandem;

import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.WeakHashMap;
import dev.exocutor.tandem.mixin.HolderTagsAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;

/** Binds the server's tag memberships for registries shared by the game. */
public final class RegistryTags {
    private static RegistryTags installed;
    private static boolean restoring;
    private final Map<Registry<?>, Entry> entries;
    private final Map<RegistryTags, List<Runnable>> transitions = new WeakHashMap<>();

    private record Entry(Map<?, ?> values, Registry.PendingTags<?> pending,
                         Map<Holder.Reference<?>, Set<?>> memberships) {}
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
        Map<Holder.Reference<?>, Set<?>> memberships = new IdentityHashMap<>();
        registry.listElements().forEach(holder -> memberships.put(holder, ((HolderTagsAccess) holder).tandem$tags()));
        return new Entry(Collections.unmodifiableMap(tags), registry.prepareTagReload(new TagLoader.LoadResult<>(registry.key(), tags)), memberships);
    }

    public static boolean restoring() { return restoring && Tandem.client().isSameThread(); }

    public void bind() {
        if (installed == this) return;
        if (installed != null) {
            List<Runnable> changes = transitions.computeIfAbsent(installed, this::transitionFrom);
            restoring = true;
            try {
                changes.forEach(Runnable::run);
            } finally {
                restoring = false;
            }
        }
        installed = this;
    }

    private List<Runnable> transitionFrom(RegistryTags previousState) {
        List<Runnable> changes = new ArrayList<>();
        entries.forEach((registry, entry) -> {
            Entry previous = previousState.entries.get(registry);
            if (previous == null || entry.values.equals(previous.values)) return;
            changes.add(entry.pending::apply);
            // Holder memberships are immutable snapshots; restore only changed references.
            entry.memberships.forEach((holder, tags) -> {
                if (!tags.equals(previous.memberships.get(holder)))
                    changes.add(() -> ((HolderTagsAccess) holder).tandem$tags(tags));
            });
        });
        return List.copyOf(changes);
    }
}
