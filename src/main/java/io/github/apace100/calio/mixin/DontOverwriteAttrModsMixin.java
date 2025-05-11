package io.github.apace100.calio.mixin;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.apace100.calio.Calio;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/***
 * This mixin makes sure that adding attribute modifiers to an equipment item does not overwrite the existing ones.
 */
@Mixin(ItemStack.class)
public abstract class DontOverwriteAttrModsMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;getList(Ljava/lang/String;I)Lnet/minecraft/nbt/ListTag;", ordinal = 0), method = "getAttributeModifiers")
    private void addAttributeModifiersFromItem(EquipmentSlot slot, CallbackInfoReturnable info, @Local Multimap multimap) {
        ItemStack thisStack = (ItemStack)(Object)this;
        if(Calio.areEntityAttributesAdditional(thisStack)) {
            multimap.putAll(thisStack.getItem().getDefaultAttributeModifiers(slot));
        }
    }
}
