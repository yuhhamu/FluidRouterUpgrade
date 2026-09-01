package com.yuuhamu.fluidrouterupgrade.integration.jei;

import me.desht.modularrouters.container.FilterSlot;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/*
 * 1.19.2向け移植メモ(2026-09-01): JEI 11.39.0.1068(1.19.2)のIGhostIngredientHandlerは
 * getTargetsTyped(ITypedIngredient)ではなく旧来のgetTargets(S,I,boolean)を使う
 * (FluidRoutersの1.19.2移植で確立済みの同型対応をそのまま踏襲)。
 */
public class FluidModuleGhostIngredientHandler<S extends AbstractContainerScreen<?>> implements IGhostIngredientHandler<S> {

    @Override
    public <I> List<Target<I>> getTargets(S gui, I ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        AbstractContainerMenu menu = gui.getMenu();
        for (Slot slot : menu.slots) {
            if (slot instanceof FilterSlot) {
                targets.add(new FluidModuleGhostTarget<>(gui, slot));
            }
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }
}
