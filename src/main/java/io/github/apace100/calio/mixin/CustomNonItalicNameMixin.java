package io.github.apace100.calio.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.apace100.calio.Calio;
import io.github.apace100.calio.NbtConstants;
import net.minecraft.client.gui.Gui;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/***
 * These mixins allow setting a custom name for item stacks via NBT.
 */
public abstract class CustomNonItalicNameMixin {

    @Mixin(ItemStack.class)
    public abstract static class ModifyItalicDisplayItem {
        @ModifyExpressionValue(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hasCustomHoverName()Z"))
        private boolean hasCustomNameWhichIsItalic(boolean original) {
            return original && !Calio.hasNonItalicName((ItemStack) (Object) this);
        }
    }

    @Mixin(Gui.class)
    public abstract static class ModifyItalicDisplayHud {
        @Shadow private ItemStack lastToolHighlight;

        @ModifyExpressionValue(method = "renderSelectedItemName", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hasCustomHoverName()Z"))
        private boolean hasCustomNameWhichIsItalic(boolean original) {
            return original && !Calio.hasNonItalicName(this.lastToolHighlight);
        }
    }

    @Mixin(AnvilMenu.class)
    public abstract static class RemoveNonItalicOnRename {
        @Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;setHoverName(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/world/item/ItemStack;"))
        private void removeNonItalicFlag(CallbackInfo ci, @Local(ordinal = 1) ItemStack itemStack2) {
            CompoundTag display = itemStack2.getTagElement("display");
            if(display != null && display.contains(NbtConstants.NON_ITALIC_NAME)) {
                display.remove(NbtConstants.NON_ITALIC_NAME);
            }
        }
    }
}
