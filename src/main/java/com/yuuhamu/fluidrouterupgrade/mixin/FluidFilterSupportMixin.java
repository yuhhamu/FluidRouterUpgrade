package com.yuuhamu.fluidrouterupgrade.mixin;

import com.yuuhamu.fluidrouterupgrade.logic.FluidFilterTag;
import me.desht.modularrouters.item.module.DistributorModule;
import me.desht.modularrouters.item.module.ModuleItem;
import me.desht.modularrouters.item.module.PullerModule1;
import me.desht.modularrouters.item.module.PullerModule2;
import me.desht.modularrouters.item.module.SenderModule1;
import me.desht.modularrouters.item.module.SenderModule2;
import me.desht.modularrouters.item.module.SenderModule3;
import me.desht.modularrouters.item.module.VoidModule;
import me.desht.modularrouters.logic.filter.matchers.FluidMatcher;
import me.desht.modularrouters.api.matching.IItemMatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModuleItem.class, remap = false)
public abstract class FluidFilterSupportMixin {

    @Inject(method = "getFilterItemMatcher", at = @At("HEAD"), cancellable = true)
    private void fluidrouterupgrade$getFilterItemMatcher(ItemStack stack, CallbackInfoReturnable<IItemMatcher> cir) {
        if (isFluidCapableModule() && isRegisteredFluidFilter(stack)) {
            cir.setReturnValue(new FluidMatcher(stack));
        }
    }

    @Inject(method = "getFilterItemDisplayName", at = @At("HEAD"), cancellable = true)
    private void fluidrouterupgrade$getFilterItemDisplayName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        if (isFluidCapableModule() && isRegisteredFluidFilter(stack)) {
            FluidUtil.getFluidContained(stack).ifPresent(fluidStack -> cir.setReturnValue(fluidStack.getHoverName()));
        }
    }

    private boolean isFluidCapableModule() {
        ModuleItem self = (ModuleItem) (Object) this;
        return self instanceof DistributorModule || self instanceof VoidModule
                || self instanceof PullerModule1 || self instanceof PullerModule2
                || self instanceof SenderModule1 || self instanceof SenderModule2 || self instanceof SenderModule3;
    }

    private static boolean isRegisteredFluidFilter(ItemStack stack) {
        return FluidFilterTag.isTaggedAsFluidFilter(stack) && FluidFilterTag.isEligibleFluidContainer(stack);
    }
}
