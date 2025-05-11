package io.github.apace100.calio;

import io.github.apace100.calio.mixin.CriteriaRegistryInvoker;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

public class Calio implements ModInitializer {

	@Override
	public void onInitialize() {
		CriteriaRegistryInvoker.callRegister(CodeTriggerCriterion.INSTANCE);
	}

	public static boolean hasNonItalicName(ItemStack stack) {
		return stack.hasTag() && stack.getTagElement("display") != null && stack.getTagElement("display").getBoolean(NbtConstants.NON_ITALIC_NAME);
	}

	public static void setNameNonItalic(ItemStack stack) {
		if(stack != null)
			stack.getOrCreateTagElement("display").putBoolean(NbtConstants.NON_ITALIC_NAME, true);
	}

	public static boolean areEntityAttributesAdditional(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains(NbtConstants.ADDITIONAL_ATTRIBUTES) && stack.getTag().getBoolean(NbtConstants.ADDITIONAL_ATTRIBUTES);
	}

	/**
	 * Sets whether the item stack counts the entity attribute modifiers specified in its tag as additional,
	 * meaning they won't overwrite the equipment's inherent modifiers.
	 * @param stack
	 * @param additional
	 */
	public static void setEntityAttributesAdditional(ItemStack stack, boolean additional) {
		if(stack != null) {
			if(additional) {
				stack.getOrCreateTag().putBoolean(NbtConstants.ADDITIONAL_ATTRIBUTES, true);
			} else {
				if(stack.hasTag()) {
					stack.getTag().remove(NbtConstants.ADDITIONAL_ATTRIBUTES);
				}
			}
		}
	}

	public static <T> boolean areTagsEqual(ResourceKey<? extends Registry<T>> registryKey, TagKey<T> tag1, TagKey<T> tag2) {
		return areTagsEqual(tag1, tag2);
	}

	public static <T> boolean areTagsEqual(TagKey<T> tag1, TagKey<T> tag2) {
		if(tag1 == tag2) {
			return true;
		}
		if(tag1 == null || tag2 == null) {
			return false;
		}
		if(!tag1.registry().equals(tag2.registry())) {
			return false;
		}
		if(!tag1.location().equals(tag2.location())) {
			return false;
		}
		return true;
	}
}
