package io.github.apace100.calio.util;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface IngredientValue {
    Collection<ItemLike> getItems();
    JsonObject serialize();

    class TagValue implements IngredientValue {
        private final TagKey<Item> tag;

        public TagValue(TagKey<Item> tag) {
            this.tag = tag;
        }

        @Override
        public Collection<ItemLike> getItems() {
            List<ItemLike> list = Lists.newArrayList();
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                list.add(holder.value());
            }

            return list;
        }

        @Override
        public JsonObject serialize() {
            var json = new JsonObject();
            json.addProperty("tag", this.tag.location().toString());

            return json;
        }
    }

    class ItemValue implements IngredientValue {
        private final Item item;

        public ItemValue(Item stack) {
            this.item = stack;
        }

        @Override
        public Collection<ItemLike> getItems() {
            return Collections.singleton(item);
        }

        @Override
        public JsonObject serialize() {
            var json = new JsonObject();
            json.addProperty("item", BuiltInRegistries.ITEM.getKey(this.item).toString());
            return json;
        }
    }
}
