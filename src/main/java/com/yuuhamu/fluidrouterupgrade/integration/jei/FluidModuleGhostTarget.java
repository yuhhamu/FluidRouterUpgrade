package com.yuuhamu.fluidrouterupgrade.integration.jei;

import com.yuuhamu.fluidrouterupgrade.logic.FluidFilterTag;
import me.desht.modularrouters.network.ModuleFilterMessage;
import me.desht.modularrouters.network.PacketHandler;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

record FluidModuleGhostTarget<I>(AbstractContainerScreen<?> gui, Slot slot) implements IGhostIngredientHandler.Target<I> {

    private static final Logger FRU_DEBUG = LogManager.getLogger("FRU-DEBUG-GhostTarget");

    @Override
    public Rect2i getArea() {
        return new Rect2i(slot.x + gui.getGuiLeft(), slot.y + gui.getGuiTop(), 16, 16);
    }

    @Override
    public void accept(I ingredient) {
        FRU_DEBUG.info("[FRU-DEBUG][GhostTarget] accept() slot={} ingredientClass={} ingredient={}",
                slot.index, ingredient == null ? "null" : ingredient.getClass().getName(), ingredient);
        if (ingredient instanceof ItemStack stack) {
            FRU_DEBUG.info("[FRU-DEBUG][GhostTarget] ItemStack branch: stack={}", stack);
            PacketHandler.NETWORK.sendToServer(new ModuleFilterMessage(slot.index, stack));
        } else if (ingredient instanceof FluidStack fluidStack) {
            ItemStack bucket = FluidUtil.getFilledBucket(fluidStack);
            FRU_DEBUG.info("[FRU-DEBUG][GhostTarget] FluidStack branch: fluidStack={} bucket={} bucketEmpty={}",
                    fluidStack, bucket, bucket.isEmpty());
            if (!bucket.isEmpty()) {
                ItemStack tagged = FluidFilterTag.createFluidFilterStack(bucket);
                FRU_DEBUG.info("[FRU-DEBUG][GhostTarget] tagged stack={} hasTag={} tag={} isTaggedAsFluidFilter={}",
                        tagged, tagged.hasTag(), tagged.getTag(), FluidFilterTag.isTaggedAsFluidFilter(tagged));
                PacketHandler.NETWORK.sendToServer(new ModuleFilterMessage(slot.index, tagged));
            }
        }
    }
}

