package io.github.apace100.calio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class SerializationHelper {

    // Use SerializableDataTypes.ATTRIBUTE_MODIFIER instead
    @Deprecated
    public static AttributeModifier readAttributeModifier(JsonElement jsonElement) {
        if(jsonElement.isJsonObject()) {
            JsonObject json = jsonElement.getAsJsonObject();
            String name = GsonHelper.getAsString(json, "name", "Unnamed attribute modifier");
            String operation = GsonHelper.getAsString(json, "operation").toUpperCase(Locale.ROOT);
            double value = GsonHelper.getAsFloat(json, "value");
            return new AttributeModifier(name, value, AttributeModifier.Operation.valueOf(operation));
        }
        throw new JsonSyntaxException("Attribute modifier needs to be a JSON object.");
    }

    // Use SerializableDataTypes.ATTRIBUTE_MODIFIER instead
    @Deprecated
    public static AttributeModifier readAttributeModifier(FriendlyByteBuf buf) {
        String modName = buf.readUtf(32767);
        double modValue = buf.readDouble();
        int operation = buf.readInt();
        return new AttributeModifier(modName, modValue, AttributeModifier.Operation.fromValue(operation));
    }

    // Use SerializableDataTypes.ATTRIBUTE_MODIFIER instead
    @Deprecated
    public static void writeAttributeModifier(FriendlyByteBuf buf, AttributeModifier modifier) {
        buf.writeUtf(modifier.getName());
        buf.writeDouble(modifier.getAmount());
        buf.writeInt(modifier.getOperation().toValue());
    }

    public static MobEffectInstance readStatusEffect(JsonElement jsonElement) {
        if(jsonElement.isJsonObject()) {
            JsonObject json = jsonElement.getAsJsonObject();
            String effect = GsonHelper.getAsString(json, "effect");
            Optional<MobEffect> effectOptional = BuiltInRegistries.MOB_EFFECT.getOptional(ResourceLocation.tryParse(effect));
            if(!effectOptional.isPresent()) {
                throw new JsonSyntaxException("Error reading status effect: could not find status effect with id: " + effect);
            }
            int duration = GsonHelper.getAsInt(json, "duration", 100);
            int amplifier = GsonHelper.getAsInt(json, "amplifier", 0);
            boolean ambient = GsonHelper.getAsBoolean(json, "is_ambient", false);
            boolean showParticles = GsonHelper.getAsBoolean(json, "show_particles", true);
            boolean showIcon = GsonHelper.getAsBoolean(json, "show_icon", true);
            return new MobEffectInstance(effectOptional.get(), duration, amplifier, ambient, showParticles, showIcon);
        } else {
            throw new JsonSyntaxException("Expected status effect to be a json object.");
        }
    }

    public static MobEffectInstance readStatusEffect(FriendlyByteBuf buf) {
        ResourceLocation effect = buf.readResourceLocation();
        int duration = buf.readInt();
        int amplifier = buf.readInt();
        boolean ambient = buf.readBoolean();
        boolean showParticles = buf.readBoolean();
        boolean showIcon = buf.readBoolean();
        return new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.get(effect), duration, amplifier, ambient, showParticles, showIcon);
    }

    public static void writeStatusEffect(FriendlyByteBuf buf, MobEffectInstance statusEffectInstance) {
        buf.writeResourceLocation(BuiltInRegistries.MOB_EFFECT.getKey(statusEffectInstance.getEffect()));
        buf.writeInt(statusEffectInstance.getDuration());
        buf.writeInt(statusEffectInstance.getAmplifier());
        buf.writeBoolean(statusEffectInstance.isAmbient());
        buf.writeBoolean(statusEffectInstance.isVisible());
        buf.writeBoolean(statusEffectInstance.showIcon());
    }

    public static <T extends Enum<T>> HashMap<String, T> buildEnumMap(Class<T> enumClass, Function<T, String> enumToString) {
        HashMap<String, T> map = new HashMap<>();
        for (T enumConstant : enumClass.getEnumConstants()) {
            map.put(enumToString.apply(enumConstant), enumConstant);
        }
        return map;
    }
}
