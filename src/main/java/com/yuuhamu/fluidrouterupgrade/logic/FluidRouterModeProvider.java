package com.yuuhamu.fluidrouterupgrade.logic;

import com.yuuhamu.fluidrouterupgrade.client.render.FluidBeamRenderer;
import com.yuuhamu.fluidrouterupgrade.client.JeiFluidTooltipBridge;
import com.yuuhamu.fluidrouterupgrade.config.FluidRouterUpgradeConfig;
import com.yuuhamu.fluidrouterupgrade.network.FluidBeamStartMessage;
import com.yuuhamu.fluidrouterupgrade.network.FluidBeamStopMessage;
import com.yuuhamu.fluidrouterupgrade.registry.ModBlocks;
import com.yuuhamu.routerupgradecore.api.ModuleKind;
import com.yuuhamu.routerupgradecore.api.ModuleTargeting;
import com.yuuhamu.routerupgradecore.api.RouterModeProvider;
import com.yuuhamu.routerupgradecore.api.RouterUpgradeCore;
import me.desht.modularrouters.ModularRouters;
import me.desht.modularrouters.block.ModularRouterBlock;
import me.desht.modularrouters.block.tile.ModularRouterBlockEntity;
import me.desht.modularrouters.core.ModItems;
import me.desht.modularrouters.logic.ModuleTarget;
import me.desht.modularrouters.logic.settings.RelativeDirection;
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
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public class FluidRouterModeProvider implements RouterModeProvider {

    private static final String NBT_TANK = "FluidRouterUpgradeTank";
    private static final String NBT_TANK_CAPACITY = "FluidRouterUpgradeTankCapacity";
    private static final String NBT_ACTIVE_BEAMS = "FluidRouterUpgradeActiveBeams";

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

    // 2026-09-02修正(Forge版から継承): StackAugment(StackUpgradeをモジュール単体に適用するための、
    // モジュール自身の拡張スロットへ入れるアイテム)が実際の転送量に反映されない不具合の修正。Vanilla本体の
    // CompiledModule#getItemsPerTick(router)の仕様(モジュール単体のStackAugment数n>0の場合はRouterレベル
    // のStack Upgrade設定を完全に無視し、モジュール単体の値で上書きする)に合わせ、
    // ModuleTargeting.getAugmentCount(compiled, STACK_AUGMENT)が1以上ならそちらを優先して使う。
    private static int computeMaxTransfer(ModularRouterBlockEntity router, CompiledModule compiled) {
        int maxExponent = FluidRouterUpgradeConfig.MAX_STACK_UPGRADE_EXPONENT.get();
        int augmentCount = ModuleTargeting.getAugmentCount(compiled, ModItems.STACK_AUGMENT.get());
        int n = augmentCount > 0 ? augmentCount : router.getUpgradeCount(ModItems.STACK_UPGRADE.get());
        int multiplier = 1 << Math.min(n, maxExponent);
        return FluidRouterUpgradeConfig.BASE_TRANSFER_RATE_MB.get() * multiplier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(ModularRouterBlockEntity router, BlockCapability<T, Direction> capability, Direction side) {
        if (capability == Capabilities.FluidHandler.BLOCK) {
            return (T) stateOf(router).tank;
        }
        return null;
    }

    @Override
    public boolean relaxTargetValidation(Item moduleItem, UseOnContext context) {
        return true;
    }

    @Override
    public boolean onBufferSlotExtract(ModularRouterBlockEntity router, Player player) {
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty() || carried.getCount() != 1
                || carried.getCapability(Capabilities.FluidHandler.ITEM) == null) {
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
                || carried.getCapability(Capabilities.FluidHandler.ITEM) == null) {
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
        // ModularRouters 13.2.7ではCompiledModule#hasTarget()が廃止されているため、
        // getEffectiveTarget()のnullチェックのみで判定する。
        ModuleTarget target = compiled.getEffectiveTarget(router);
        if (target == null) {
            return false;
        }
        return pullFromTarget(router, target, compiled.getFilter(), compiled.getRegulationAmount(), compiled);
    }

    private boolean executeSend(ModularRouterBlockEntity router, CompiledModule compiled) {
        ModuleTarget target = resolveSenderTarget(router, compiled);
        if (target == null) {
            return false;
        }
        boolean crossDimensionSender = compiled.getClass() == CompiledSenderModule3.class;
        return pushToTarget(router, target, compiled.getFilter(), compiled.getRegulationAmount(), SEND_BEAM_COLOR, crossDimensionSender, compiled);
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
            if (te != null && FluidUtil.getFluidHandler(level, pos, face).isPresent()) {
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
            return pulling ? pullFromTarget(router, targets.get(0), filter, regulationAmount, compiled)
                    : pushToTarget(router, targets.get(0), filter, regulationAmount, SEND_BEAM_COLOR, false, compiled);
        }
        boolean balancerActive = strategy == CompiledDistributorModule.DistributionStrategy.ROUND_ROBIN
                && ModuleTargeting.getAugmentCount(compiled, com.yuuhamu.fluidrouterupgrade.registry.ModItems.BALANCER_AUGMENT.get()) > 0;
        if (balancerActive) {
            return executeBalanced(router, targets, pulling, filter, regulationAmount, compiled);
        }
        return switch (strategy) {
            case RANDOM -> {
                ModuleTarget target = targets.get(router.nonNullLevel().random.nextInt(n));
                yield pulling ? pullFromTarget(router, target, filter, regulationAmount, compiled)
                        : pushToTarget(router, target, filter, regulationAmount, SEND_BEAM_COLOR, false, compiled);
            }
            case NEAREST_FIRST -> {
                for (ModuleTarget target : targets) {
                    boolean ok = pulling ? pullFromTarget(router, target, filter, regulationAmount, compiled)
                            : pushToTarget(router, target, filter, regulationAmount, SEND_BEAM_COLOR, false, compiled);
                    if (ok) {
                        yield true;
                    }
                }
                yield false;
            }
            case FURTHEST_FIRST -> {
                for (int i = n - 1; i >= 0; i--) {
                    ModuleTarget target = targets.get(i);
                    boolean ok = pulling ? pullFromTarget(router, target, filter, regulationAmount, compiled)
                            : pushToTarget(router, target, filter, regulationAmount, SEND_BEAM_COLOR, false, compiled);
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
                    boolean ok = pulling ? pullFromTarget(router, target, filter, regulationAmount, compiled)
                            : pushToTarget(router, target, filter, regulationAmount, SEND_BEAM_COLOR, false, compiled);
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
                                     Filter filter, int regulationAmount, CompiledModule compiled) {
        RouterTankState state = stateOf(router);
        FluidTank tank = state.tank;
        int allowance = computeMaxTransfer(router, compiled);
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
                reportFluidTransfer(router, info.target().gPos.pos(), moved, beamColor, pulling, false);
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
                                  int beamColor, boolean crossDimensionSender, CompiledModule compiled) {
        Level routerLevel = router.getLevel();
        if (routerLevel == null) {
            return false;
        }
        Level targetLevel = target.isSameWorld(routerLevel) ? routerLevel : MiscUtil.getWorldForGlobalPos(target.gPos);
        if (targetLevel == null) {
            return false;
        }
        BlockPos pos = target.gPos.pos();
        Optional<IFluidHandler> destCap = FluidUtil.getFluidHandler(targetLevel, pos, target.face);
        if (destCap.isEmpty()) {
            return false;
        }
        FluidTank tank = stateOf(router).tank;
        if (regulationAmount > 0 && tank.getFluidAmount() <= regulationAmount) {
            return false;
        }
        int maxTransfer = computeMaxTransfer(router, compiled);
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
            reportFluidTransfer(router, pos, moved, beamColor, false, crossDimensionSender);
            return true;
        }).orElse(false);
    }

    private boolean pullFromTarget(ModularRouterBlockEntity router, ModuleTarget target, Filter filter, int regulationAmount, CompiledModule compiled) {
        Level routerLevel = router.getLevel();
        if (routerLevel == null) {
            return false;
        }
        Level targetLevel = target.isSameWorld(routerLevel) ? routerLevel : MiscUtil.getWorldForGlobalPos(target.gPos);
        if (targetLevel == null) {
            return false;
        }
        BlockPos pos = target.gPos.pos();
        Optional<IFluidHandler> sourceCap = FluidUtil.getFluidHandler(targetLevel, pos, target.face);
        if (sourceCap.isEmpty()) {
            return false;
        }
        FluidTank tank = stateOf(router).tank;
        if (regulationAmount > 0 && tank.getFluidAmount() >= regulationAmount) {
            return false;
        }
        int maxTransfer = computeMaxTransfer(router, compiled);
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
            reportFluidTransfer(router, pos, moved, PULL_BEAM_COLOR, true, false);
            return true;
        }).orElse(false);
    }

    private boolean executeVoid(ModularRouterBlockEntity router, CompiledModule compiled) {
        FluidTank tank = stateOf(router).tank;
        FluidStack current = tank.getFluid();
        if (current.isEmpty() || !compiled.getFilter().testFluid(current.getFluid())) {
            return false;
        }
        int maxTransfer = computeMaxTransfer(router, compiled);
        FluidStack drained = tank.drain(maxTransfer, IFluidHandler.FluidAction.EXECUTE);
        return !drained.isEmpty();
    }

    private void reportFluidTransfer(ModularRouterBlockEntity router, BlockPos targetPos, FluidStack fluid,
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
        if (crossDimensionSender) {
            Direction facing = router.getAbsoluteFacing(RelativeDirection.FRONT);
            effectiveTargetPos = routerPos.relative(facing, 1);
            baseColor = SEND_MK3_BEAM_COLOR;
        } else {
            baseColor = beamColor;
        }
        ResourceLocation fluidId = (fluid == null || fluid.isEmpty())
                ? null
                : BuiltInRegistries.FLUID.getKey(fluid.getFluid());

        BlockPos finalEffectiveTargetPos = effectiveTargetPos;
        int finalBaseColor = baseColor;
        FluidBeamKey key = new FluidBeamKey(routerPos, finalEffectiveTargetPos, isPull, crossDimensionSender);
        RouterTankState state = stateOf(router);
        RouterUpgradeCore.reportBeamActive(router, key,
                () -> {
                    state.activeBeams.put(key, new BeamVisual(finalBaseColor, fluidId));
                    PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(routerPos),
                            new FluidBeamStartMessage(routerPos, finalEffectiveTargetPos, finalBaseColor, isPull, crossDimensionSender,
                                    Optional.ofNullable(fluidId)));
                },
                () -> {
                    state.activeBeams.remove(key);
                    PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(routerPos),
                            new FluidBeamStopMessage(routerPos, finalEffectiveTargetPos, isPull, crossDimensionSender));
                });
    }

    private record BeamVisual(int color, ResourceLocation fluidId) {
    }

    @Override
    public void load(ModularRouterBlockEntity router, CompoundTag tag, HolderLookup.Provider registries) {
        FluidTank tank = stateOf(router).tank;
        if (tag.contains(NBT_TANK)) {
            tank.readFromNBT(registries, tag.getCompound(NBT_TANK));
        }
    }

    @Override
    public void saveAdditional(ModularRouterBlockEntity router, CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(NBT_TANK, stateOf(router).tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    public void getUpdateTag(ModularRouterBlockEntity router, CompoundTag tag, HolderLookup.Provider registries) {
        RouterTankState state = stateOf(router);
        FluidTank tank = state.tank;
        tag.put(NBT_TANK, tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt(NBT_TANK_CAPACITY, tank.getCapacity());

        ListTag beamList = new ListTag();
        for (Map.Entry<FluidBeamKey, BeamVisual> entry : state.activeBeams.entrySet()) {
            FluidBeamKey key = entry.getKey();
            BeamVisual visual = entry.getValue();
            CompoundTag beamTag = new CompoundTag();
            beamTag.putInt("X", key.targetPos().getX());
            beamTag.putInt("Y", key.targetPos().getY());
            beamTag.putInt("Z", key.targetPos().getZ());
            beamTag.putBoolean("Pull", key.isPull());
            beamTag.putBoolean("CrossDim", key.crossDimensionSender());
            beamTag.putInt("Color", visual.color());
            if (visual.fluidId() != null) {
                beamTag.putString("Fluid", visual.fluidId().toString());
            }
            beamList.add(beamTag);
        }
        tag.put(NBT_ACTIVE_BEAMS, beamList);
    }

    @Override
    public void handleUpdateTag(ModularRouterBlockEntity router, CompoundTag tag, HolderLookup.Provider registries) {
        FluidTank tank = stateOf(router).tank;
        if (tag.contains(NBT_TANK)) {
            tank.readFromNBT(registries, tag.getCompound(NBT_TANK));
        }
        if (tag.contains(NBT_TANK_CAPACITY)) {
            tank.setCapacity(tag.getInt(NBT_TANK_CAPACITY));
        }

        List<FluidBeamRenderer.SyncedBeam> synced = new ArrayList<>();
        if (tag.contains(NBT_ACTIVE_BEAMS, Tag.TAG_LIST)) {
            ListTag beamList = tag.getList(NBT_ACTIVE_BEAMS, Tag.TAG_COMPOUND);
            for (int i = 0; i < beamList.size(); i++) {
                CompoundTag beamTag = beamList.getCompound(i);
                BlockPos targetPos = new BlockPos(beamTag.getInt("X"), beamTag.getInt("Y"), beamTag.getInt("Z"));
                boolean isPull = beamTag.getBoolean("Pull");
                boolean crossDim = beamTag.getBoolean("CrossDim");
                int color = beamTag.getInt("Color");
                ResourceLocation fluidId = beamTag.contains("Fluid") ? ResourceLocation.parse(beamTag.getString("Fluid")) : null;
                synced.add(new FluidBeamRenderer.SyncedBeam(targetPos, isPull, crossDim, color, fluidId));
            }
        }
        FluidBeamRenderer.syncRouter(router.getBlockPos(), synced);
    }

    @Override
    public void onRemoved(ModularRouterBlockEntity router) {
        RouterTankState state = states.remove(router);
        if (state != null) {
            Level level = router.getLevel();
            if (level != null && !level.isClientSide() && !state.activeBeams.isEmpty()) {
                BlockPos routerPos = router.getBlockPos();
                for (Map.Entry<FluidBeamKey, BeamVisual> entry : state.activeBeams.entrySet()) {
                    FluidBeamKey key = entry.getKey();
                    PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(routerPos),
                            new FluidBeamStopMessage(key.routerPos(), key.targetPos(), key.isPull(), key.crossDimensionSender()));
                }
                state.activeBeams.clear();
            }
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

    // 2026-09-03追加: バッファのツールチップをJEIの液体ツールチップ表記へ統一(Forge 1.20.1版と同一仕様)。
    // 名前+Mod名グレー表示はJeiFluidTooltipBridge(既存、FluidFilterSlotRendererで使用実績あり)を
    // 再利用し、数量表記もJEI本体のFluidTankRenderer#getTooltip()と同じ書式("%s / %s mB"、
    // NumberFormat.getIntegerInstance()による桁区切り、グレー表示)へ統一する。
    private static final java.text.NumberFormat TANK_AMOUNT_FORMAT = java.text.NumberFormat.getIntegerInstance();

    @Override
    public List<Component> getBufferTooltip(ModularRouterBlockEntity router) {
        FluidTank tank = stateOf(router).tank;
        FluidStack contents = tank.getFluid();
        int capacity = tank.getCapacity();
        List<Component> lines = new ArrayList<>();
        if (contents.isEmpty()) {
            lines.add(Component.translatable("gui.fluidrouterupgrade.tank.capacity",
                    Component.translatable("gui.fluidrouterupgrade.tank.mb", TANK_AMOUNT_FORMAT.format(capacity))));
        } else {
            Optional<List<Component>> jeiLines = JeiFluidTooltipBridge.getTooltip(contents);
            if (jeiLines.isPresent()) {
                lines.addAll(jeiLines.get());
            } else {
                lines.add(contents.getHoverName());
            }
            lines.add(Component.translatable("gui.fluidrouterupgrade.tank.amount_with_capacity",
                            TANK_AMOUNT_FORMAT.format(contents.getAmount()), TANK_AMOUNT_FORMAT.format(capacity))
                    .withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    private static final class RouterTankState {
        final FluidTank tank;
        final Map<FluidBeamKey, BeamVisual> activeBeams = new LinkedHashMap<>();
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
        }
    }
}
