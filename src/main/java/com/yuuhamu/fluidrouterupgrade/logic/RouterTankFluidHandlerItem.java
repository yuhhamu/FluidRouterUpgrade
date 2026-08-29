package com.yuuhamu.fluidrouterupgrade.logic;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;

public class RouterTankFluidHandlerItem implements IFluidHandlerItem {

    private final FluidTank tank;

    public RouterTankFluidHandlerItem(FluidTank tank) {
        this.tank = tank;
    }

    @Nonnull
    @Override
    public ItemStack getContainer() {
        return ItemStack.EMPTY;
    }

    @Override
    public int getTanks() {
        return tank.getTanks();
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank_) {
        return tank.getFluidInTank(tank_);
    }

    @Override
    public int getTankCapacity(int tank_) {
        return tank.getTankCapacity(tank_);
    }

    @Override
    public boolean isFluidValid(int tank_, @Nonnull FluidStack stack) {
        return tank.isFluidValid(tank_, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return tank.fill(resource, action);
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return tank.drain(resource, action);
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return tank.drain(maxDrain, action);
    }
}
