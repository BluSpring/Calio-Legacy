package io.github.apace100.calio.data;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.google.gson.internal.LazilyParsedNumber;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.apace100.calio.ClassUtil;
import io.github.apace100.calio.SerializationHelper;
import io.github.apace100.calio.data.SerializableData.Instance;
import io.github.apace100.calio.util.ArgumentWrapper;
import io.github.apace100.calio.util.StatusEffectChance;
import io.github.apace100.calio.util.TagLike;
import net.minecraft.ResourceLocationException;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.tags.TagKey;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import java.util.*;

@SuppressWarnings("unused")
public final class SerializableDataTypes {

    public static final SerializableDataType<Integer> INT = new SerializableDataType<>(
        Integer.class,
        FriendlyByteBuf::writeInt,
        FriendlyByteBuf::readInt,
        JsonElement::getAsInt);

    public static final SerializableDataType<List<Integer>> INTS = SerializableDataType.list(INT);

    public static final SerializableDataType<Boolean> BOOLEAN = new SerializableDataType<>(
        Boolean.class,
        FriendlyByteBuf::writeBoolean,
        FriendlyByteBuf::readBoolean,
        JsonElement::getAsBoolean);

    public static final SerializableDataType<Float> FLOAT = new SerializableDataType<>(
        Float.class,
        FriendlyByteBuf::writeFloat,
        FriendlyByteBuf::readFloat,
        JsonElement::getAsFloat);

    public static final SerializableDataType<List<Float>> FLOATS = SerializableDataType.list(FLOAT);

    public static final SerializableDataType<Double> DOUBLE = new SerializableDataType<>(
        Double.class,
        FriendlyByteBuf::writeDouble,
        FriendlyByteBuf::readDouble,
        JsonElement::getAsDouble);

    public static final SerializableDataType<List<Double>> DOUBLES = SerializableDataType.list(DOUBLE);

    public static final SerializableDataType<String> STRING = new SerializableDataType<>(
        String.class,
        FriendlyByteBuf::writeUtf,
        (buf) -> buf.readUtf(32767),
        JsonElement::getAsString);

    public static final SerializableDataType<List<String>> STRINGS = SerializableDataType.list(STRING);

    public static final SerializableDataType<Number> NUMBER = new SerializableDataType<>(
        Number.class,
        (buf, number) -> {
            if(number instanceof Double) {
                buf.writeByte(0);
                buf.writeDouble(number.doubleValue());
            } else if(number instanceof Float) {
                buf.writeByte(1);
                buf.writeFloat(number.floatValue());
            } else if(number instanceof Integer) {
                buf.writeByte(2);
                buf.writeInt(number.intValue());
            } else if(number instanceof Long) {
                buf.writeByte(3);
                buf.writeLong(number.longValue());
            } else {
                buf.writeByte(4);
                buf.writeUtf(number.toString());
            }
        },
        buf -> {
            byte type = buf.readByte();
            switch(type) {
                case 0:
                    return buf.readDouble();
                case 1:
                    return buf.readFloat();
                case 2:
                    return buf.readInt();
                case 3:
                    return buf.readLong();
                case 4:
                    return new LazilyParsedNumber(buf.readUtf());
            }
            throw new RuntimeException("Could not receive number, unexpected type id \"" + type + "\" (allowed range: [0-4])");
        },
        je -> {
            if(je.isJsonPrimitive()) {
                JsonPrimitive primitive = je.getAsJsonPrimitive();
                if(primitive.isNumber()) {
                    return primitive.getAsNumber();
                } else if(primitive.isBoolean()) {
                    return primitive.getAsBoolean() ? 1 : 0;
                }
            }
            throw new JsonParseException("Expected a primitive");
        });

    public static final SerializableDataType<List<Number>> NUMBERS = SerializableDataType.list(NUMBER);

    public static final SerializableDataType<Vec3> VECTOR = new SerializableDataType<>(Vec3.class,
        (packetByteBuf, vector3d) -> {
            packetByteBuf.writeDouble(vector3d.x);
            packetByteBuf.writeDouble(vector3d.y);
            packetByteBuf.writeDouble(vector3d.z);
        },
        (packetByteBuf -> new Vec3(
            packetByteBuf.readDouble(),
            packetByteBuf.readDouble(),
            packetByteBuf.readDouble())),
        (jsonElement -> {
            if(jsonElement.isJsonObject()) {
                JsonObject jo = jsonElement.getAsJsonObject();
                return new Vec3(
                    GsonHelper.getAsDouble(jo, "x", 0),
                    GsonHelper.getAsDouble(jo, "y", 0),
                    GsonHelper.getAsDouble(jo, "z", 0)
                );
            } else {
                throw new JsonParseException("Expected an object with x, y, and z fields.");
            }
        }));

    public static final SerializableDataType<ResourceLocation> IDENTIFIER = new SerializableDataType<>(
        ResourceLocation.class,
        FriendlyByteBuf::writeResourceLocation,
        FriendlyByteBuf::readResourceLocation,
        (json) -> {
            String idString = json.getAsString();
            if(idString.contains(":")) {
                String[] idSplit = idString.split(":");
                if(idSplit.length != 2) {
                    throw new ResourceLocationException("Incorrect number of `:` in identifier: \"" + idString + "\".");
                }
                if(idSplit[0].contains("*")) {
                    if(SerializableData.CURRENT_NAMESPACE != null) {
                        idSplit[0] = idSplit[0].replace("*", SerializableData.CURRENT_NAMESPACE);
                    } else {
                        throw new ResourceLocationException("Identifier may not contain a `*` in the namespace when read here.");
                    }
                }
                if(idSplit[1].contains("*")) {
                    if(SerializableData.CURRENT_PATH != null) {
                        idSplit[1] = idSplit[1].replace("*", SerializableData.CURRENT_PATH);
                    } else {
                        throw new ResourceLocationException("Identifier may only contain a `*` in the path inside of powers.");
                    }
                }
                idString = idSplit[0] + ":" + idSplit[1];
            } else {
                if(idString.contains("*")) {
                    if(SerializableData.CURRENT_PATH != null) {
                        idString = idString.replace("*", SerializableData.CURRENT_PATH);
                    } else {
                        throw new ResourceLocationException("Identifier may only contain a `*` in the path inside of powers.");
                    }
                }
            }
            return new ResourceLocation(idString);
        });

    public static final SerializableDataType<List<ResourceLocation>> IDENTIFIERS = SerializableDataType.list(IDENTIFIER);

    public static final SerializableDataType<Enchantment> ENCHANTMENT = SerializableDataType.registry(Enchantment.class, BuiltInRegistries.ENCHANTMENT);

    public static SerializableDataType<ResourceKey<Level>> DIMENSION = SerializableDataType.registryKey(Registries.DIMENSION);


    public static final SerializableDataType<Attribute> ATTRIBUTE = SerializableDataType.registry(Attribute.class, BuiltInRegistries.ATTRIBUTE);

    public static final SerializableDataType<AttributeModifier.Operation> MODIFIER_OPERATION = SerializableDataType.enumValue(AttributeModifier.Operation.class);

    public static final SerializableDataType<AttributeModifier> ATTRIBUTE_MODIFIER = SerializableDataType.compound(AttributeModifier.class, new SerializableData()
            .add("name", STRING, "Unnamed attribute modifier")
            .add("operation", MODIFIER_OPERATION)
            .add("value", DOUBLE),
        data -> new AttributeModifier(
            data.getString("name"),
            data.getDouble("value"),
            data.get("operation")
        ),
        (serializableData, modifier) -> {
            SerializableData.Instance inst = serializableData.new Instance();
            inst.set("name", modifier.getName());
            inst.set("value", modifier.getAmount());
            inst.set("operation", modifier.getOperation());
            return inst;
        });

    public static final SerializableDataType<List<AttributeModifier>> ATTRIBUTE_MODIFIERS =
        SerializableDataType.list(ATTRIBUTE_MODIFIER);

    public static final SerializableDataType<Item> ITEM = SerializableDataType.registry(Item.class, BuiltInRegistries.ITEM);

    public static final SerializableDataType<MobEffect> STATUS_EFFECT = SerializableDataType.registry(MobEffect.class, BuiltInRegistries.MOB_EFFECT);

    public static final SerializableDataType<List<MobEffect>> STATUS_EFFECTS =
        SerializableDataType.list(STATUS_EFFECT);

    public static final SerializableDataType<MobEffectInstance> STATUS_EFFECT_INSTANCE = new SerializableDataType<>(
        MobEffectInstance.class,
        SerializationHelper::writeStatusEffect,
        SerializationHelper::readStatusEffect,
        SerializationHelper::readStatusEffect);

    public static final SerializableDataType<List<MobEffectInstance>> STATUS_EFFECT_INSTANCES =
        SerializableDataType.list(STATUS_EFFECT_INSTANCE);

    public static final SerializableDataType<TagKey<Item>> ITEM_TAG = SerializableDataType.tag(Registries.ITEM);

    public static final SerializableDataType<TagKey<Fluid>> FLUID_TAG = SerializableDataType.tag(Registries.FLUID);

    public static final SerializableDataType<TagKey<Block>> BLOCK_TAG = SerializableDataType.tag(Registries.BLOCK);

    public static final SerializableDataType<TagKey<EntityType<?>>> ENTITY_TAG = SerializableDataType.tag(Registries.ENTITY_TYPE);

    public static final SerializableDataType<Ingredient.Value> INGREDIENT_ENTRY = SerializableDataType.compound(ClassUtil.castClass(Ingredient.Value.class),
        new SerializableData()
            .add("item", ITEM, null)
            .add("tag", ITEM_TAG, null),
        dataInstance -> {
            boolean tagPresent = dataInstance.isPresent("tag");
            boolean itemPresent = dataInstance.isPresent("item");
            if(tagPresent == itemPresent) {
                throw new JsonParseException("An ingredient entry is either a tag or an item, " + (tagPresent ? "not both" : "one has to be provided."));
            }
            if(tagPresent) {
                TagKey<Item> tag = dataInstance.get("tag");
                return new Ingredient.TagValue(tag);
            } else {
                return new Ingredient.ItemValue(new ItemStack((Item)dataInstance.get("item")));
            }
        }, (data, entry) -> data.read(entry.serialize()));

    public static final SerializableDataType<List<Ingredient.Value>> INGREDIENT_ENTRIES = SerializableDataType.list(INGREDIENT_ENTRY);

    // An alternative version of an ingredient deserializer which allows `minecraft:air`
    public static final SerializableDataType<Ingredient> INGREDIENT = new SerializableDataType<>(
        Ingredient.class,
        (buffer, ingredient) -> ingredient.toNetwork(buffer),
        Ingredient::fromNetwork,
        jsonElement -> {
            List<Ingredient.Value> entryList = INGREDIENT_ENTRIES.read(jsonElement);
            return Ingredient.fromValues(entryList.stream());
        });

    // The regular vanilla Minecraft ingredient.
    public static final SerializableDataType<Ingredient> VANILLA_INGREDIENT = new SerializableDataType<>(
        Ingredient.class,
        (buffer, ingredient) -> ingredient.toNetwork(buffer),
        Ingredient::fromNetwork,
        Ingredient::fromJson);

    public static final SerializableDataType<Block> BLOCK = SerializableDataType.registry(Block.class, BuiltInRegistries.BLOCK);

    public static final SerializableDataType<BlockState> BLOCK_STATE = SerializableDataType.wrap(BlockState.class, STRING,
        BlockStateParser::serialize,
        string -> {
            try {
                return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), string, false).blockState();
            } catch (CommandSyntaxException e) {
                throw new JsonParseException(e);
            }
        });

    public static final SerializableDataType<ResourceKey<DamageType>> DAMAGE_TYPE = SerializableDataType.registryKey(Registries.DAMAGE_TYPE);

    public static final SerializableDataType<MobType> ENTITY_GROUP =
        SerializableDataType.mapped(MobType.class, HashBiMap.create(ImmutableMap.of(
            "default", MobType.UNDEFINED,
            "undead", MobType.UNDEAD,
            "arthropod", MobType.ARTHROPOD,
            "illager", MobType.ILLAGER,
            "aquatic", MobType.WATER
        )));

    public static final SerializableDataType<EquipmentSlot> EQUIPMENT_SLOT = SerializableDataType.enumValue(EquipmentSlot.class);

    public static final SerializableDataType<SoundEvent> SOUND_EVENT = SerializableDataType.registry(SoundEvent.class, BuiltInRegistries.SOUND_EVENT);

    public static final SerializableDataType<EntityType<?>> ENTITY_TYPE = SerializableDataType.registry(ClassUtil.castClass(EntityType.class), BuiltInRegistries.ENTITY_TYPE);

    public static final SerializableDataType<ParticleType<?>> PARTICLE_TYPE = SerializableDataType.registry(ClassUtil.castClass(ParticleType.class), BuiltInRegistries.PARTICLE_TYPE);

    public static final SerializableDataType<ParticleOptions> PARTICLE_EFFECT = SerializableDataType.compound(ParticleOptions.class,
        new SerializableData()
            .add("type", PARTICLE_TYPE)
            .add("params", STRING, ""),
        dataInstance -> {
            ParticleType<? extends ParticleOptions> particleType = dataInstance.get("type");
            ParticleOptions.Deserializer factory = particleType.getDeserializer();
            ParticleOptions effect = null;
            try {
                effect = factory.fromCommand(particleType, new StringReader(" " + dataInstance.getString("params")));
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
            return effect;
        },
        ((serializableData, particleEffect) -> {
            SerializableData.Instance data = serializableData.new Instance();
            data.set("type", particleEffect.getType());
            String params = particleEffect.writeToString();
            int spaceIndex = params.indexOf(' ');
            if(spaceIndex > -1) {
                params = params.substring(spaceIndex + 1);
            } else {
                params = "";
            }
            data.set("params", params);
            return data;
        }));

    public static final SerializableDataType<ParticleOptions> PARTICLE_EFFECT_OR_TYPE = new SerializableDataType<>(ParticleOptions.class,
        PARTICLE_EFFECT::send,
        PARTICLE_EFFECT::receive,
        jsonElement -> {
            if(jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString()) {
                ParticleType<?> type = PARTICLE_TYPE.read(jsonElement);
                if(type instanceof ParticleOptions) {
                    return (ParticleOptions) type;
                }
                throw new RuntimeException("Expected either a string with a parameter-less particle effect, or an object.");
            } else if(jsonElement.isJsonObject()) {
                return PARTICLE_EFFECT.read(jsonElement);
            }
            throw new RuntimeException("Expected either a string with a parameter-less particle effect, or an object.");
        });

    public static final SerializableDataType<CompoundTag> NBT = new SerializableDataType<>(
        CompoundTag.class,
        FriendlyByteBuf::writeNbt,
        FriendlyByteBuf::readNbt,
        jsonElement -> {

            if (!(jsonElement.isJsonObject()|| jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString()))
                throw new JsonSyntaxException("Expected either a string or an object.");

            try {
                String stringifiedJsonElement = jsonElement.isJsonObject() ? jsonElement.getAsJsonObject().toString() : jsonElement.getAsJsonPrimitive().getAsString();
                return new TagParser(new StringReader(stringifiedJsonElement)).readStruct();
            }
            catch (CommandSyntaxException e) {
                throw new JsonSyntaxException("Could not parse NBT: " + e.getMessage());
            }

        }
    );

    public static final SerializableDataType<ItemStack> ITEM_STACK = SerializableDataType.compound(ItemStack.class,
        new SerializableData()
            .add("item", SerializableDataTypes.ITEM)
            .add("amount", SerializableDataTypes.INT, 1)
            .add("tag", NBT, null),
        (data) ->  {
            ItemStack stack = new ItemStack((Item)data.get("item"), data.getInt("amount"));
            if(data.isPresent("tag")) {
                stack.setTag(data.get("tag"));
            }
            return stack;
        },
        ((serializableData, itemStack) -> {
            SerializableData.Instance data = serializableData.new Instance();
            data.set("item", itemStack.getItem());
            data.set("amount", itemStack.getCount());
            data.set("tag", itemStack.hasTag() ? itemStack.getTag() : null);
            return data;
        }));

    public static final SerializableDataType<List<ItemStack>> ITEM_STACKS = SerializableDataType.list(ITEM_STACK);

    public static final SerializableDataType<Component> TEXT = new SerializableDataType<>(Component.class,
        (buffer, text) -> buffer.writeUtf(Component.Serializer.toJson(text)),
        (buffer) -> Component.Serializer.fromJson(buffer.readUtf(32767)),
        Component.Serializer::fromJson);

    public static final SerializableDataType<List<Component>> TEXTS = SerializableDataType.list(TEXT);

    public static final SerializableDataType<Recipe> RECIPE = new SerializableDataType<>(Recipe.class,
        (buffer, recipe) -> {
            buffer.writeResourceLocation(BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer()));
            buffer.writeResourceLocation(recipe.getId());
            recipe.getSerializer().toNetwork(buffer, recipe);
        },
        (buffer) -> {
            ResourceLocation recipeSerializerId = buffer.readResourceLocation();
            ResourceLocation recipeId = buffer.readResourceLocation();
            RecipeSerializer<?> serializer = BuiltInRegistries.RECIPE_SERIALIZER.get(recipeSerializerId);
            return serializer.fromNetwork(recipeId, buffer);
        },
        (jsonElement) -> {
            if(!jsonElement.isJsonObject()) {
                throw new RuntimeException("Expected recipe to be a JSON object.");
            }
            JsonObject json = jsonElement.getAsJsonObject();
            ResourceLocation recipeSerializerId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "type"));
            ResourceLocation recipeId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "id"));
            RecipeSerializer<?> serializer = BuiltInRegistries.RECIPE_SERIALIZER.get(recipeSerializerId);
            return serializer.fromJson(recipeId, json);
        });

    public static final SerializableDataType<GameEvent> GAME_EVENT = SerializableDataType.registry(GameEvent.class, BuiltInRegistries.GAME_EVENT);

    public static final SerializableDataType<List<GameEvent>> GAME_EVENTS =
        SerializableDataType.list(GAME_EVENT);

    public static final SerializableDataType<TagKey<GameEvent>> GAME_EVENT_TAG = SerializableDataType.tag(Registries.GAME_EVENT);

    public static final SerializableDataType<Fluid> FLUID = SerializableDataType.registry(Fluid.class, BuiltInRegistries.FLUID);

    public static final SerializableDataType<FogType> CAMERA_SUBMERSION_TYPE = SerializableDataType.enumValue(FogType.class);

    public static final SerializableDataType<InteractionHand> HAND = SerializableDataType.enumValue(InteractionHand.class);

    public static final SerializableDataType<EnumSet<InteractionHand>> HAND_SET = SerializableDataType.enumSet(InteractionHand.class, HAND);

    public static final SerializableDataType<EnumSet<EquipmentSlot>> EQUIPMENT_SLOT_SET = SerializableDataType.enumSet(EquipmentSlot.class, EQUIPMENT_SLOT);

    public static final SerializableDataType<InteractionResult> ACTION_RESULT = SerializableDataType.enumValue(InteractionResult.class);

    public static final SerializableDataType<UseAnim> USE_ACTION = SerializableDataType.enumValue(UseAnim.class);

    public static final SerializableDataType<StatusEffectChance> STATUS_EFFECT_CHANCE =
        SerializableDataType.compound(StatusEffectChance.class, new SerializableData()
            .add("effect", STATUS_EFFECT_INSTANCE)
            .add("chance", FLOAT, 1.0F),
            (data) -> {
                StatusEffectChance sec = new StatusEffectChance();
                sec.statusEffectInstance = data.get("effect");
                sec.chance = data.getFloat("chance");
                return sec;
            },
            (data, csei) -> {
                SerializableData.Instance inst = data.new Instance();
                inst.set("effect", csei.statusEffectInstance);
                inst.set("chance", csei.chance);
                return inst;
            });

    public static final SerializableDataType<List<StatusEffectChance>> STATUS_EFFECT_CHANCES = SerializableDataType.list(STATUS_EFFECT_CHANCE);

    public static final SerializableDataType<FoodProperties> FOOD_COMPONENT = SerializableDataType.compound(FoodProperties.class, new SerializableData()
            .add("hunger", INT)
            .add("saturation", FLOAT)
            .add("meat", BOOLEAN, false)
            .add("always_edible", BOOLEAN, false)
            .add("snack", BOOLEAN, false)
            .add("effect", STATUS_EFFECT_CHANCE, null)
            .add("effects", STATUS_EFFECT_CHANCES, null),
        (data) -> {
            FoodProperties.Builder builder = new FoodProperties.Builder().nutrition(data.getInt("hunger")).saturationMod(data.getFloat("saturation"));
            if (data.getBoolean("meat")) {
                builder.meat();
            }
            if (data.getBoolean("always_edible")) {
                builder.alwaysEat();
            }
            if (data.getBoolean("snack")) {
                builder.fast();
            }
            data.<StatusEffectChance>ifPresent("effect", sec -> {
                builder.effect(sec.statusEffectInstance, sec.chance);
            });
            data.<List<StatusEffectChance>>ifPresent("effects", secs -> secs.forEach(sec -> {
                builder.effect(sec.statusEffectInstance, sec.chance);
            }));
            return builder.build();
        },
        (data, fc) -> {
            SerializableData.Instance inst = data.new Instance();
            inst.set("hunger", fc.getNutrition());
            inst.set("saturation", fc.getSaturationModifier());
            inst.set("meat", fc.isMeat());
            inst.set("always_edible", fc.canAlwaysEat());
            inst.set("snack", fc.isFastFood());
            inst.set("effect", null);
            List<StatusEffectChance> statusEffectChances = new LinkedList<>();
            fc.getEffects().forEach(pair -> {
                StatusEffectChance sec = new StatusEffectChance();
                sec.statusEffectInstance = pair.getFirst();
                sec.chance = pair.getSecond();
                statusEffectChances.add(sec);
            });
            if(statusEffectChances.size() > 0) {
                inst.set("effects", statusEffectChances);
            } else {
                inst.set("effects", null);
            }
            return inst;
        });

    public static final SerializableDataType<Direction> DIRECTION = SerializableDataType.enumValue(Direction.class);

    public static final SerializableDataType<EnumSet<Direction>> DIRECTION_SET = SerializableDataType.enumSet(Direction.class, DIRECTION);

    public static final SerializableDataType<Class<?>> CLASS = SerializableDataType.wrap(ClassUtil.castClass(Class.class), SerializableDataTypes.STRING,
        Class::getName,
        str -> {
            try {
                return Class.forName(str);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Specified class does not exist: \"" + str + "\".");
            }
        });

    public static final SerializableDataType<ClipContext.Block> SHAPE_TYPE = SerializableDataType.enumValue(ClipContext.Block.class);

    public static final SerializableDataType<ClipContext.Fluid> FLUID_HANDLING = SerializableDataType.enumValue(ClipContext.Fluid.class);

    public static final SerializableDataType<Explosion.BlockInteraction> DESTRUCTION_TYPE = SerializableDataType.enumValue(Explosion.BlockInteraction.class);

    public static final SerializableDataType<Direction.Axis> AXIS = SerializableDataType.enumValue(Direction.Axis.class);

    public static final SerializableDataType<EnumSet<Direction.Axis>> AXIS_SET = SerializableDataType.enumSet(Direction.Axis.class, AXIS);

    public static final SerializableDataType<ArgumentWrapper<NbtPathArgument.NbtPath>> NBT_PATH =
        SerializableDataType.argumentType(NbtPathArgument.nbtPath());

    public static final SerializableDataType<ClipContext.Block> RAYCAST_SHAPE_TYPE = SerializableDataType.enumValue(ClipContext.Block.class);

    public static final SerializableDataType<ClipContext.Fluid> RAYCAST_FLUID_HANDLING = SerializableDataType.enumValue(ClipContext.Fluid.class);

    public static final SerializableDataType<Stat<?>> STAT = SerializableDataType.compound(ClassUtil.castClass(Stat.class),
        new SerializableData()
            .add("type", SerializableDataType.registry(ClassUtil.castClass(StatType.class), BuiltInRegistries.STAT_TYPE))
            .add("id", SerializableDataTypes.IDENTIFIER),
        data -> {
            StatType statType = data.get("type");
            Registry<?> statRegistry = statType.getRegistry();
            ResourceLocation statId = data.get("id");
            if(statRegistry.containsKey(statId)) {
                Object statObject = statRegistry.get(statId);
                return statType.get(statObject);
            }
            throw new IllegalArgumentException("Desired stat \"" + statId + "\" does not exist in stat type ");
        },
        (data, stat) -> {
            SerializableData.Instance inst = data.new Instance();
            inst.set("type", stat.getType());
            Registry reg = stat.getType().getRegistry();
            ResourceLocation statId = reg.getKey(stat.getValue());
            inst.set("id", statId);
            return inst;
        });

    public static final SerializableDataType<TagKey<Biome>> BIOME_TAG = SerializableDataType.tag(Registries.BIOME);

    public static final SerializableDataType<TagLike<Item>> ITEM_TAG_LIKE = SerializableDataType.tagLike(BuiltInRegistries.ITEM);

    public static final SerializableDataType<TagLike<Block>> BLOCK_TAG_LIKE = SerializableDataType.tagLike(BuiltInRegistries.BLOCK);

    public static final SerializableDataType<TagLike<EntityType<?>>> ENTITY_TYPE_TAG_LIKE = SerializableDataType.tagLike(BuiltInRegistries.ENTITY_TYPE);
}
