package com.yuuhamu.fluidrouterupgrade.integration.jei;

import com.yuuhamu.fluidrouterupgrade.logic.FluidFilterTag;
import me.desht.modularrouters.network.messages.ModuleFilterMessage;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.network.PacketDistributor;

record FluidModuleGhostTarget<I>(AbstractContainerScreen<?> gui, Slot slot) implements IGhostIngredientHandler.Target<I> {

    @Override
    public Rect2i getArea() {
        return new Rect2i(slot.x + gui.getGuiLeft(), slot.y + gui.getGuiTop(), 16, 16);
    }

    @Override
    public void accept(I ingredient) {
        if (ingredient instanceof ItemStack stack) {
            PacketDistributor.sendToServer(new ModuleFilterMessage(slot.index, stack));
        } else if (ingredient instanceof FluidStack fluidStack) {
            ItemStack bucket = FluidUtil.getFilledBucket(fluidStack);
            if (!bucket.isEmpty()) {
                ItemStack tagged = FluidFilterTag.createFluidFilterStack(bucket);
                PacketDistributor.sendToServer(new ModuleFilterMessage(slot.index, tagged));
            }
        }
    }
}
