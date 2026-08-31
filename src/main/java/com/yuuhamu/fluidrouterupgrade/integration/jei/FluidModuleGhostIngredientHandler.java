package com.yuuhamu.fluidrouterupgrade.integration.jei;

import me.desht.modularrouters.container.FilterSlot;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class FluidModuleGhostIngredientHandler<S extends AbstractContainerScreen<?>> implements IGhostIngredientHandler<S> {

    private static final Logger FRU_DEBUG = LogManager.getLogger("FRU-DEBUG-GhostHandler");

    @Override
    public <I> List<Target<I>> getTargetsTyped(S gui, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        AbstractContainerMenu menu = gui.getMenu();
        for (Slot slot : menu.slots) {
            if (slot instanceof FilterSlot) {
                targets.add(new FluidModuleGhostTarget<>(gui, slot));
            }
        }
        FRU_DEBUG.info("[FRU-DEBUG][GhostHandler] gui={} ingredient={} doStart={} targets={}",
                gui.getClass().getName(),
                ingredient.getIngredient() == null ? "null" : ingredient.getIngredient().getClass().getName(),
                doStart, targets.size());
        return targets;
    }

    @Override
    public void onComplete() {
    }
}

