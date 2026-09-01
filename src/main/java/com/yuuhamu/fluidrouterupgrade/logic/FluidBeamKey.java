package com.yuuhamu.fluidrouterupgrade.logic;

import net.minecraft.core.BlockPos;

public record FluidBeamKey(BlockPos routerPos, BlockPos targetPos, boolean isPull, boolean crossDimensionSender) {
}
