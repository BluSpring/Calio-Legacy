package io.github.apace100.calio.mixin;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CriteriaTriggers.class)
public interface CriteriaRegistryInvoker {

    @Invoker
    static <T extends CriterionTrigger<?>> T callRegister(T object) {
        throw new RuntimeException("Invoker :)");
    }
}
