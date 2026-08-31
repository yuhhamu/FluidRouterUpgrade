package com.yuuhamu.fluidrouterupgrade.logic;

import net.minecraft.core.BlockPos;

/**
 * 転送ビームを一意に識別するキー。
 *
 * サーバー側での稼働タイミングごとの継続判定(RouterUpgradeCore.reportBeamActive)と、
 * クライアント側での描画エントリ管理(FluidBeamRenderer)の両方で同じ定義を共有する。
 */
public record FluidBeamKey(BlockPos routerPos, BlockPos targetPos, boolean isPull, boolean crossDimensionSender) {
}
