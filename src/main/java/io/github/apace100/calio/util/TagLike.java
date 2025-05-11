package io.github.apace100.calio.util;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public class TagLike<T> {

    private final Registry<T> registry;
    private final List<TagKey<T>> tags = new LinkedList<>();
    private final Set<T> items = new HashSet<>();

    public TagLike(Registry<T> registry) {
        this.registry = registry;
    }

    public void addTag(ResourceLocation id) {
        addTag(TagKey.create(registry.key(), id));
    }

    public void add(ResourceLocation id) {
        add(registry.get(id));
    }

    public void addTag(TagKey<T> tagKey) {
        tags.add(tagKey);
    }

    public void add(T t) {
        items.add(t);
    }

    public boolean contains(T t) {
        if(items.contains(t)) {
            return true;
        }
        Holder<T> entry = registry.wrapAsHolder(t);
        for(TagKey<T> tagKey : tags) {
            if(entry.is(tagKey)) {
                return true;
            }
        }
        return false;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(tags.size());
        for(TagKey<T> tagKey : tags) {
            buf.writeUtf(tagKey.location().toString());
        }
        buf.writeVarInt(items.size());
        for(T t : items) {
            buf.writeUtf(registry.getKey(t).toString());
        }
    }

    public void read(FriendlyByteBuf buf) {
        tags.clear();
        int count = buf.readVarInt();
        for(int i = 0; i < count; i++) {
            tags.add(TagKey.create(registry.key(), new ResourceLocation(buf.readUtf())));
        }
        items.clear();
        count = buf.readVarInt();
        for(int i = 0; i < count; i++) {
            T t = registry.get(new ResourceLocation(buf.readUtf()));
            items.add(t);
        }
    }
}
