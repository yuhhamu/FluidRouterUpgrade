package com.yuuhamu.fluidrouterupgrade.logic;

import com.yuuhamu.fluidrouterupgrade.config.FluidRouterUpgradeConfig;
import com.yuuhamu.fluidrouterupgrade.network.FluidBeamMessage;
import com.yuuhamu.fluidrouterupgrade.network.PacketHandler;
import com.yuuhamu.fluidrouterupgrade.registry.ModBlocks;
import com.yuuhamu.routerupgradecore.api.ModuleKind;
import com.yuuhamu.routerupgradecore.api.ModuleTargeting;
import com.yuuhamu.routerupgradecore.api.RouterModeProvider;
import me.desht.modularrouters.ModularRouters;
import me.desht.modularrouters.block.ModularRouterBlock;
import me.desht.modularrouters.block.tile.ModularRouterBlockEntity;
import me.desht.modularrouters.core.ModItems;
import me.desht.modularrouters.item.module.ModuleItem;
import me.desht.modularrouters.logic.ModuleTarget;
import me.desht.modularrouters.logic.compiled.CompiledDistributorModule;
import me.desht.modularrouters.logic.compiled.CompiledModule;
import me.desht.modularrouters.logic.compiled.CompiledSenderModule1;
import me.desht.modularrouters.logic.compiled.CompiledSenderModule3;
import me.desht.modularrouters.logic.filter.Filter;
import me.desht.modularrouters.util.BlockUtil;
import me.desht.modularrouters.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class FluidRouterModeProvider implements RouterModeProvider {

    private static final String NBT_TANK = "FluidRouterUpgradeTank";
    private static final String NBT_TANK_CAPACITY = "FluidRouterUpgradeTankCapacity";

    public static final int IMAGE_COLOR = 0x2E9BFF;

    private static final int PULL_BEAM_COLOR = 0x2060FF;
    private static final int SEND_BEAM_COLOR = 0x30C040;
    private static final int SEND_MK3_BEAM_COLOR = 0x800080;

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
    public boolean onBufferSlotExtract(ModularRouterBlockEntity router, Player player) {
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty() || carried.getCount() != 1
                || !carried.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
            return false;
        }
        FluidTank tank = stateOf(router).tank;
        FluidActionResult result = FluidUtil.tryFillContainer(carried, tank, Integer.MAX_VALUE, player, true);
        if (result.isSuccess()) {
            player.containerMenu.setCarried(result.getResult());
        }
        return true;
    }

    @Override
    public boolean onBufferSlotCollect(ModularRouterBlockEntity router, Player player) {
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty() || carried.getCount() != 1
                || !carried.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
            return false;
        }
        FluidTank tank = stateOf(router).tank;
        FluidActionResult result = FluidUtil.tryEmptyContainer(carried, tank, Integer.MAX_VALUE, player, true);
        if (result.isSuccess()) {
            player.containerMenu.setCarried(result.getResult());
        }
        return true;
    }

    @Override
    public boolean executeModuleLogic(ModularRouterBlockEntity router, ModuleKind kind, CompiledModule vanillaCompiledModule) {
        return switch (kind) {
            case PULLER -> executePull(router, vanillaCompiledModule);
            case VOID -> executeVoid(router, vanillaCompiledModule);
            case SENDER -> executeSend(router, vanillaCompiledModule);
            case DISTRIBUTOR -> executeDistribute(router, vanillaCompiledModule);
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
        return pullFromTarget(router, target, compiled.getFilter(), compiled.getRegulationAmount());
    }

    private boolean executeSend(ModularRouterBlockEntity router, CompiledModule compiled) {
        ModuleTarget target = resolveSenderTarget(router, compiled);
        if (target == null) {
            return false;
        }
        boolean crossDimensionSender = compiled.getClass() == CompiledSenderModule3.class;
        return pushToTarget(router, target, compiled.getFilter(), compiled.getRegulationAmount(), SEND_BEAM_COLOR, crossDimensionSender);
    }

    private ModuleTarget resolveSenderTarget(ModularRouterBlockEntity router, CompiledModule compiled) {
        if (compiled.getClass() == CompiledSenderModule1.class) {
            return scanForFluidTarget(router, compiled);
        }
        ModuleTarget target = ModuleTargeting.getTarget(compiled);
        if (target == null) {
            return null;
        }
        Level level = router.getLevel();
        if (level == null) {
            return null;
        }
        if (compiled.getClass() == CompiledSenderModule3.class) {
            boolean allowed = target.isSameWorld(level)
                    || (!ModularRouters.getDimensionBlacklist().test(target.gPos.dimension().location())
                        && !ModularRouters.getDimensionBlacklist().test(level.dimension().location()));
            return allowed ? target : null;
        }
        if (!target.isSameWorld(level)) {
            return null;
        }
        if (router.getBlockPos().distSqr(target.gPos.pos()) > (double) ModuleTargeting.getRangeSquared(compiled)) {
            return null;
        }
        return target;
    }

    private ModuleTarget scanForFluidTarget(ModularRouterBlockEntity router, CompiledModule compiled) {
        ModuleTarget base = ModuleTargeting.getTarget(compiled);
        if (base == null) {
            return null;
        }
        Level level = router.nonNullLevel();
        BlockPos p0 = base.gPos.pos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(p0.getX(), p0.getY(), p0.getZ());
        Direction face = base.face;
        Direction facing = ModuleTargeting.getFacing(compiled);
        int range = ModuleTargeting.getRange(compiled);
        for (int i = 1; i <= range; i++) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te != null && te.getCapability(ForgeCapabilities.FLUID_HANDLER, face).isPresent()) {
                GlobalPos gPos = MiscUtil.makeGlobalPos(level, pos.immutable());
                return new ModuleTarget(gPos, face, BlockUtil.getBlockName(level, pos));
            }
            if (!isPassable(level, pos, face)) {
                return null;
            }
            pos.move(facing);
        }
        return null;
    }

    private static boolean isPassable(Level level, BlockPos pos, Direction face) {
        BlockState state = level.getBlockState(pos);
        return !MiscUtil.blockHasSolidSide(state, level, pos, face.getOpposite()) || !state.isSolidRender(level, pos);
    }

    private boolean executeDistribute(ModularRouterBlockEntity router, CompiledModule compiled) {
        boolean pulling = compiled instanceof CompiledDistributorModule dist && dist.isPulling();
        List<ModuleTarget> targets = ModuleTargeting.getTargets(compiled);
        if (targets == null || targets.isEmpty()) {
            return false;
        }
        Filter filter = compiled.getFilter();
        int regulationAmount = compiled.getRegulationAmount();
        CompiledDistributorModule.DistributionStrategy strategy =
                compiled instanceof CompiledDistributorModule dist
                        ? dist.getDistributionStrategy()
                        : CompiledDistributorModule.DistributionStrategy.ROUND_ROBIN;
        int n = targets.size();
        if (n == 1) {
            return pulling ? pullFromTarget(router, targets.get(0), filter, regulationAmount)
                    : pushToTarget(router, targets.get(0), filter, regulationAmount, SEND_BEAM_COLOR, false);
        }
        boolean balancerActive = strategy == CompiledDistributorModule.DistributionStrategy.ROUND_ROBIN
                && ModuleTargeting.getAugmentCount(compiled, com.yuuhamu.fluidrouterupgrade.registry.ModItems.BALANCER_AUGMENT.get()) > 0;
        if (balancerActive) {
            return executeBalanced(router, targets, pulling, filter, regulationAmount);
        }
        return switch (strategy) {
            case RANDOM -> {
                ModuleTarget target = targets.get(router.nonNullLevel().random.nextInt(n));
                yield pulling ? pullFromTarget(router, target, filter, regulationAmount)
                        : pushToTarget(router, target, filter, regulationAmount, SEND_BEAM_COLOR, false);
            }
            case NEAREST_FIRST -> {
                for (ModuleTarget target : targets) {
                    boolean ok = pulling ? pullFromTarget(router, target, filter, regulationAmount)
                            : pushToTarget(router, target, filter, regulationAmount, SEND_BEAM_COLOR, false);
                    if (ok) {
                        yield true;
                    }
                }
                yield false;
            }
            case FURTHEST_FIRST -> {
                for (int i = n - 1; i >= 0; i--) {
                    ModuleTarget target = targets.get(i);
                    boolean ok = pulling ? pullFromTarget(router, target, filter, regulationAmount)
                            : pushToTarget(router, target, filter, regulationAmount, SEND_BEAM_COLOR, false);
                    if (ok) {
                        yield true;
                    }
                }
                yield false;
            }
            case ROUND_ROBIN -> {
                int start = stateOf(router).distributorIndex;
                boolean success = false;
                for (int i = 0; i < n; i++) {
                    int idx = (start + i) % n;
                    ModuleTarget target = targets.get(idx);
                    boolean ok = pulling ? pullFromTarget(router, target, filter, regulationAmount)
                            : pushToTarget(router, target, filter, regulationAmount, SEND_BEAM_COLOR, false);
                    if (ok) {
                        stateOf(router).distributorIndex = idx + 1;
                        success = true;
                        break;
                    }
                }
                yield success;
            }
        };
    }

    private boolean executeBalanced(ModularRouterBlockEntity router, List<ModuleTarget> targets, boolean pulling,
                                     Filter filter, int regulationAmount) {
        RouterTankState state = stateOf(router);
        FluidTank tank = state.tank;
        int allowance = computeMaxTransfer(router);
        if (allowance <= 0) {
            return false;
        }
        if (regulationAmount > 0) {
            int tankLevel = tank.getFluidAmount();
            if (!pulling && tankLevel <= regulationAmount) {
                return false;
            }
            if (pulling && tankLevel >= regulationAmount) {
                return false;
            }
        }

        List<TargetHandler> infos = new ArrayList<>();
        for (ModuleTarget target : targets) {
            IFluidHandler handler = resolveBalanceHandler(router, target);
            if (handler != null) {
                infos.add(new TargetHandler(target, handler));
            }
        }
        if (infos.isEmpty()) {
            return false;
        }

        int budget = pulling
                ? Math.min(allowance, tank.getCapacity() - tank.getFluidAmount())
                : Math.min(allowance, tank.getFluidAmount());
        if (budget <= 0) {
            return false;
        }

        int n = infos.size();
        int base = budget / n;
        int remainder = budget % n;
        int start = state.balancerRotation % n;

        boolean any = false;
        for (int i = 0; i < n; i++) {
            int idx = (start + i) % n;
            TargetHandler info = infos.get(idx);
            int share = base + (i < remainder ? 1 : 0);
            if (share <= 0) {
                continue;
            }
            IFluidHandler src = pulling ? info.handler() : tank;
            IFluidHandler dst = pulling ? tank : info.handler();
            FluidStack simulated = FluidUtil.tryFluidTransfer(dst, src, share, false);
            if (simulated.isEmpty() || (filter != null && !filter.testFluid(simulated.getFluid()))) {
                continue;
            }
            FluidStack moved = FluidUtil.tryFluidTransfer(dst, src, simulated.getAmount(), true);
            if (!moved.isEmpty()) {
                int beamColor = pulling ? PULL_BEAM_COLOR : SEND_BEAM_COLOR;
                addFluidBeam(router, info.target().gPos.pos(), moved, beamColor, pulling, false);
                any = true;
            }
        }
        state.balancerRotation = (start + 1) % n;
        return any;
    }

    private record TargetHandler(ModuleTarget target, IFluidHandler handler) {
    }

    private IFluidHandler resolveBalanceHandler(ModularRouterBlockEntity router, ModuleTarget target) {
        Level routerLevel = router.getLevel();
        if (routerLevel == null) {
            return null;
        }
        Level targetLevel = target.isSameWorld(routerLevel) ? routerLevel : MiscUtil.getWorldForGlobalPos(target.gPos);
        if (targetLevel == null || !targetLevel.isLoaded(target.gPos.pos())) {
            return null;
        }
        return FluidUtil.getFluidHandler(targetLevel, target.gPos.pos(), target.face).orElse(null);
    }

    private boolean pushToTarget(ModularRouterBlockEntity router, ModuleTarget target, Filter filter, int regulationAmount,
                                  int beamColor, boolean crossDimensionSender) {
        Level routerLevel = router.getLevel();
        if (routerLevel == null) {
            return false;
        }
        Level targetLevel = target.isSameWorld(routerLevel) ? routerLevel : MiscUtil.getWorldForGlobalPos(target.gPos);
        if (targetLevel == null) {
            return false;
        }
        BlockPos pos = target.gPos.pos();
        LazyOptional<IFluidHandler> destCap = FluidUtil.getFluidHandler(targetLevel, pos, target.face);
        if (!destCap.isPresent()) {
            return false;
        }
        FluidTank tank = stateOf(router).tank;
        if (regulationAmount > 0 && tank.getFluidAmount() <= regulationAmount) {
            return false;
        }
        int maxTransfer = computeMaxTransfer(router);
        FluidStack simulated = tank.drain(maxTransfer, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty() || (filter != null && !filter.testFluid(simulated.getFluid()))) {
            return false;
        }
        return destCap.map(dest -> {
            int filled = dest.fill(simulated, IFluidHandler.FluidAction.EXECUTE);
            if (filled <= 0) {
                return false;
            }
            FluidStack moved = new FluidStack(simulated.getFluid(), filled);
            tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
            addFluidBeam(router, pos, moved, beamColor, false, crossDimensionSender);
            return true;
        }).orElse(false);
    }

    private boolean pullFromTarget(ModularRouterBlockEntity router, ModuleTarget target, Filter filter, int regulationAmount) {
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
        if (regulationAmount > 0 && tank.getFluidAmount() >= regulationAmount) {
            return false;
        }
        int maxTransfer = computeMaxTransfer(router);
        return sourceCap.map(source -> {
            FluidStack simulated = source.drain(maxTransfer, IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty() || (filter != null && !filter.testFluid(simulated.getFluid()))) {
                return false;
            }
            int filled = tank.fill(simulated, IFluidHandler.FluidAction.EXECUTE);
            if (filled <= 0) {
                return false;
            }
            FluidStack moved = new FluidStack(simulated.getFluid(), filled);
            source.drain(moved, IFluidHandler.FluidAction.EXECUTE);
            addFluidBeam(router, pos, moved, PULL_BEAM_COLOR, true, false);
            return true;
        }).orElse(false);
    }

    private boolean executeVoid(ModularRouterBlockEntity router, CompiledModule compiled) {
        FluidTank tank = stateOf(router).tank;
        FluidStack current = tank.getFluid();
        if (current.isEmpty() || !compiled.getFilter().testFluid(current.getFluid())) {
            return false;
        }
        int maxTransfer = computeMaxTransfer(router);
        FluidStack drained = tank.drain(maxTransfer, IFluidHandler.FluidAction.EXECUTE);
        return !drained.isEmpty();
    }

    private static void addFluidBeam(ModularRouterBlockEntity router, BlockPos targetPos, FluidStack fluid,
                                      int beamColor, boolean isPull, boolean crossDimensionSender) {
        if (router.getUpgradeCount(ModItems.MUFFLER_UPGRADE.get()) >= 2) {
            return;
        }
        Level level = router.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockPos routerPos = router.getBlockPos();
        BlockPos effectiveTargetPos = targetPos;
        int baseColor;
        boolean fade;
        boolean reversed;
        if (crossDimensionSender) {
            Direction facing = router.getAbsoluteFacing(ModuleItem.RelativeDirection.FRONT);
            effectiveTargetPos = routerPos.relative(facing, 1);
            baseColor = SEND_MK3_BEAM_COLOR;
            fade = true;
            reversed = false;
        } else {
            baseColor = beamColor;
            fade = false;
            reversed = isPull;
        }
        ResourceLocation fluidId = (fluid == null || fluid.isEmpty())
                ? null
                : ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        BlockPos finalEffectiveTargetPos = effectiveTargetPos;
        PacketHandler.NETWORK.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(routerPos)),
                new FluidBeamMessage(routerPos, finalEffectiveTargetPos, router.getTickRate(), baseColor, reversed, fade, fluidId));
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
    public BlockState getVisualCamouflage(ModularRouterBlockEntity router) {
        BlockState routerState = router.getBlockState();
        Direction facing = routerState.getValue(ModularRouterBlock.FACING);
        boolean active = routerState.getValue(ModularRouterBlock.ACTIVE);
        return ModBlocks.FLUID_ROUTER_VISUAL.get().defaultBlockState()
                .setValue(ModularRouterBlock.FACING, facing)
                .setValue(ModularRouterBlock.ACTIVE, active);
    }

    @Override
    public ResourceLocation getBufferContentTexture(ModularRouterBlockEntity router) {
        FluidStack contents = stateOf(router).tank.getFluid();
        if (contents.isEmpty()) {
            return null;
        }
        IClientFluidTypeExtensions props = IClientFluidTypeExtensions.of(contents.getFluid());
        return props.getStillTexture(contents);
    }

    @Override
    public int getBufferContentTintColor(ModularRouterBlockEntity router) {
        FluidStack contents = stateOf(router).tank.getFluid();
        if (contents.isEmpty()) {
            return 0xFFFFFF;
        }
        IClientFluidTypeExtensions props = IClientFluidTypeExtensions.of(contents.getFluid());
        return props.getTintColor(contents);
    }

    @Override
    public List<Component> getBufferTooltip(ModularRouterBlockEntity router) {
        FluidTank tank = stateOf(router).tank;
        FluidStack contents = tank.getFluid();
        int capacity = tank.getCapacity();
        List<Component> lines = new ArrayList<>();
        if (contents.isEmpty()) {
            lines.add(Component.translatable("gui.fluidrouterupgrade.tank.capacity",
                    Component.translatable("modularrouters.guiText.tooltip.regulator.labelFluidmB", capacity)));
        } else {
            lines.add(contents.getDisplayName());
            lines.add(Component.translatable("modularrouters.guiText.tooltip.regulator.labelFluidmB", contents.getAmount())
                    .append(" / ")
                    .append(Component.translatable("modularrouters.guiText.tooltip.regulator.labelFluidmB", capacity)));
        }
        return lines;
    }

    private static final class RouterTankState {
        final FluidTank tank;
        final LazyOptional<IFluidHandler> tankCap;
        final LazyOptional<IFluidHandlerItem> tankItemCap;
        int distributorIndex = 0;

        int balancerRotation = 0;

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

