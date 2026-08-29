package com.yuuhamu.fluidrouterupgrade.logic;

import com.yuuhamu.fluidrouterupgrade.config.FluidRouterUpgradeConfig;
import com.yuuhamu.routerupgradecore.api.ModuleKind;
import com.yuuhamu.routerupgradecore.api.RouterModeProvider;
import me.desht.modularrouters.block.tile.ModularRouterBlockEntity;
import me.desht.modularrouters.core.ModItems;
import me.desht.modularrouters.logic.ModuleTarget;
import me.desht.modularrouters.logic.compiled.CompiledModule;
import me.desht.modularrouters.util.BeamData;
import me.desht.modularrouters.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.Map;
import java.util.WeakHashMap;

public class FluidRouterModeProvider implements RouterModeProvider {

    private static final String NBT_TANK = "FluidRouterUpgradeTank";
    private static final String NBT_TANK_CAPACITY = "FluidRouterUpgradeTankCapacity";

    public static final int DEFAULT_BEAM_COLOR = 0x2E9BFF;

    private final Map<ModularRouterBlockEntity, RouterTankState> states = new WeakHashMap<>();

    private RouterTankState stateOf(ModularRouterBlockEntity router) {
        return states.computeIfAbsent(router, RouterTankState::new);
    }

    private static int baseTankCapacity() {
        return FluidRouterUpgradeConfig.BASE_TANK_CAPACITY_MB.get();
    }

    private static int maxTankUpgrades() {
        return FluidRouterUpgradeConfig.MAX_TANK_UPGRADES.get();
    }

    private static int mbPerTankUpgrade() {
        return FluidRouterUpgradeConfig.MB_PER_TANK_UPGRADE.get();
    }

    private static int computeMaxTransfer(ModularRouterBlockEntity router) {
        int n = router.getUpgradeCount(ModItems.STACK_UPGRADE.get());
        int maxExponent = FluidRouterUpgradeConfig.MAX_STACK_UPGRADE_EXPONENT.get();
        int multiplier = 1 << Math.min(n, maxExponent);
        return FluidRouterUpgradeConfig.BASE_TRANSFER_RATE_MB.get() * multiplier;
    }

    @Override
    public LazyOptional<?> getCapability(ModularRouterBlockEntity router, Capability<?> capability,
                                          Direction side, LazyOptional<?> vanillaDefault) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return stateOf(router).tankCap.cast();
        }
        if (capability == ForgeCapabilities.FLUID_HANDLER_ITEM) {
            return stateOf(router).tankItemCap.cast();
        }
        return vanillaDefault;
    }

    @Override
    public boolean relaxTargetValidation(Item moduleItem, UseOnContext context) {
        return true;
    }

    @Override
    public boolean executeModuleLogic(ModularRouterBlockEntity router, ModuleKind kind, CompiledModule vanillaCompiledModule) {
        return switch (kind) {
            case PULLER -> executePull(router, vanillaCompiledModule);
            case VOID -> executeVoid(router);
            // Sender/Distributorはme.desht.modularrouters.logic.compiled.CompiledSenderModule1系の
            // getEffectiveTarget()がITEM_HANDLER決め打ちの範囲探索を行うため、現状のRouterUpgradeCore APIでは
            // 生のターゲット(GlobalPos/face)を取得できず、フルード用に再実装できない。
            // RouterUpgradeCore側にターゲット取得用のAPI追加が必要(design doc §9参照、Phase 2の後続課題)。
            case SENDER, DISTRIBUTOR -> false;
        };
    }

    private boolean executePull(ModularRouterBlockEntity router, CompiledModule compiled) {
        if (!compiled.hasTarget()) {
            return false;
        }
        ModuleTarget target = compiled.getEffectiveTarget(router);
        if (target == null) {
            return false;
        }
        Level routerLevel = router.getLevel();
        if (routerLevel == null) {
            return false;
        }
        Level targetLevel = target.isSameWorld(routerLevel) ? routerLevel : MiscUtil.getWorldForGlobalPos(target.gPos);
        if (targetLevel == null) {
            return false;
        }
        BlockPos pos = target.gPos.pos();
        LazyOptional<IFluidHandler> sourceCap = FluidUtil.getFluidHandler(targetLevel, pos, target.face);
        if (!sourceCap.isPresent()) {
            return false;
        }
        FluidTank tank = stateOf(router).tank;
        int maxTransfer = computeMaxTransfer(router);
        return sourceCap.map(source -> {
            FluidStack simulated = source.drain(maxTransfer, IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty()) {
                return false;
            }
            int filled = tank.fill(simulated, IFluidHandler.FluidAction.EXECUTE);
            if (filled <= 0) {
                return false;
            }
            FluidStack drained = new FluidStack(simulated.getFluid(), filled);
            source.drain(drained, IFluidHandler.FluidAction.EXECUTE);
            addBeam(router, pos);
            return true;
        }).orElse(false);
    }

    private boolean executeVoid(ModularRouterBlockEntity router) {
        FluidTank tank = stateOf(router).tank;
        int maxTransfer = computeMaxTransfer(router);
        FluidStack drained = tank.drain(maxTransfer, IFluidHandler.FluidAction.EXECUTE);
        return !drained.isEmpty();
    }

    private static void addBeam(ModularRouterBlockEntity router, BlockPos targetPos) {
        if (router.getUpgradeCount(ModItems.MUFFLER_UPGRADE.get()) >= 2) {
            return;
        }
        router.addItemBeam(new BeamData(router.getTickRate(), targetPos, DEFAULT_BEAM_COLOR));
    }

    @Override
    public void load(ModularRouterBlockEntity router, CompoundTag tag) {
        FluidTank tank = stateOf(router).tank;
        if (tag.contains(NBT_TANK)) {
            tank.readFromNBT(tag.getCompound(NBT_TANK));
        }
    }

    @Override
    public void saveAdditional(ModularRouterBlockEntity router, CompoundTag tag) {
        tag.put(NBT_TANK, stateOf(router).tank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void getUpdateTag(ModularRouterBlockEntity router, CompoundTag tag) {
        FluidTank tank = stateOf(router).tank;
        tag.put(NBT_TANK, tank.writeToNBT(new CompoundTag()));
        tag.putInt(NBT_TANK_CAPACITY, tank.getCapacity());
    }

    @Override
    public void handleUpdateTag(ModularRouterBlockEntity router, CompoundTag tag) {
        FluidTank tank = stateOf(router).tank;
        if (tag.contains(NBT_TANK)) {
            tank.readFromNBT(tag.getCompound(NBT_TANK));
        }
        if (tag.contains(NBT_TANK_CAPACITY)) {
            tank.setCapacity(tag.getInt(NBT_TANK_CAPACITY));
        }
    }

    @Override
    public void onRemoved(ModularRouterBlockEntity router) {
        RouterTankState state = states.remove(router);
        if (state != null) {
            state.tankCap.invalidate();
            state.tankItemCap.invalidate();
        }
    }

    @Override
    public void onCompileUpgrades(ModularRouterBlockEntity router) {
        FluidTank tank = stateOf(router).tank;
        int newCapacity = baseTankCapacity()
                + Math.min(router.getUpgradeCount(com.yuuhamu.fluidrouterupgrade.registry.ModItems.TANK_UPGRADE.get()), maxTankUpgrades())
                * mbPerTankUpgrade();
        if (newCapacity == tank.getCapacity()) {
            return;
        }
        tank.setCapacity(newCapacity);
        FluidStack current = tank.getFluid();
        if (!current.isEmpty() && current.getAmount() > newCapacity) {
            current.setAmount(newCapacity);
        }
        router.setChanged();
    }

    @Override
    public Integer getBeamColor(ModularRouterBlockEntity router) {
        return DEFAULT_BEAM_COLOR;
    }

    private static final class RouterTankState {
        final FluidTank tank;
        final LazyOptional<IFluidHandler> tankCap;
        final LazyOptional<IFluidHandlerItem> tankItemCap;

        RouterTankState(ModularRouterBlockEntity router) {
            this.tank = new FluidTank(baseTankCapacity()) {
                @Override
                protected void onContentsChanged() {
                    router.setChanged();
                    Level level = router.getLevel();
                    if (level != null && !level.isClientSide) {
                        level.sendBlockUpdated(router.getBlockPos(), router.getBlockState(), router.getBlockState(), 3);
                    }
                }
            };
            this.tankCap = LazyOptional.of(() -> tank);
            this.tankItemCap = LazyOptional.of(() -> new RouterTankFluidHandlerItem(tank));
        }
    }
}
