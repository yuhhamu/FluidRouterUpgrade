package com.yuuhamu.fluidrouterupgrade.logic;

import com.yuuhamu.fluidrouterupgrade.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidUtil;

public final class FluidFilterTag {
    private FluidFilterTag() {}

    public static boolean isEligibleFluidContainer(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != 1) return false;
        return FluidUtil.getFluidContained(stack).map(fluidStack -> !fluidStack.isEmpty()).orElse(false);
    }

    public static boolean isTaggedAsFluidFilter(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return Boolean.TRUE.equals(stack.get(ModDataComponents.FLUID_FILTER.get()));
    }

    public static ItemStack createFluidFilterStack(ItemStack source) {
        ItemStack copy = source.copyWithCount(1);
        copy.set(ModDataComponents.FLUID_FILTER.get(), Boolean.TRUE);
        return copy;
    }
}
