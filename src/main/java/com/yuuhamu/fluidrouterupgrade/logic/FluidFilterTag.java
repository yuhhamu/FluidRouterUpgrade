package com.yuuhamu.fluidrouterupgrade.logic;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.ItemHandlerHelper;

public final class FluidFilterTag {

    private static final String KEY = "FluidRouterUpgradeFluidFilter";

    private FluidFilterTag() {
    }

    public static boolean isEligibleFluidContainer(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != 1) {
            return false;
        }
        return FluidUtil.getFluidContained(stack).map(fluidStack -> !fluidStack.isEmpty()).orElse(false);
    }

    public static boolean isTaggedAsFluidFilter(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        return stack.getTag().getBoolean(KEY);
    }

    public static ItemStack createFluidFilterStack(ItemStack source) {
        ItemStack copy = ItemHandlerHelper.copyStackWithSize(source, 1);
        copy.getOrCreateTag().putBoolean(KEY, true);
        return copy;
    }
}

