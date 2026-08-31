package com.yuuhamu.fluidrouterupgrade.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class FluidRouterUpgradeConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue BASE_TRANSFER_RATE_MB;
    public static final ForgeConfigSpec.IntValue MAX_STACK_UPGRADE_EXPONENT;
    public static final ForgeConfigSpec.IntValue BASE_TANK_CAPACITY_MB;
    public static final ForgeConfigSpec.IntValue MAX_TANK_UPGRADES;
    public static final ForgeConfigSpec.IntValue MB_PER_TANK_UPGRADE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("fluidTransfer");
        BASE_TRANSFER_RATE_MB = builder
                .comment(" Puller/Void系モジュール1回あたりの転送量の基本値(mB)。",
                        " Stack Upgradeの装着段数に応じて2倍ずつ増加する。")
                .defineInRange("baseTransferRateMb", 1000, 1, Integer.MAX_VALUE);
        MAX_STACK_UPGRADE_EXPONENT = builder
                .comment(" Stack Upgradeによる転送量倍率の上限(2^n倍)。")
                .defineInRange("maxStackUpgradeExponent", 6, 0, 30);
        builder.pop();

        builder.push("tank");
        BASE_TANK_CAPACITY_MB = builder
                .comment(" Fluid Mode Upgrade装着時のRouter内蔵タンクの基礎容量(mB)。Tank Upgrade未装着時の容量。")
                .defineInRange("baseTankCapacityMb", 8000, 1, Integer.MAX_VALUE);
        MAX_TANK_UPGRADES = builder
                .comment(" Tank Upgradeの最大有効装着数。")
                .defineInRange("maxTankUpgrades", 56, 0, Integer.MAX_VALUE);
        MB_PER_TANK_UPGRADE = builder
                .comment(" Tank Upgrade1個あたりのタンク容量増加量(mB)。")
                .defineInRange("mbPerTankUpgrade", 1000, 1, Integer.MAX_VALUE);
        builder.pop();

        SPEC = builder.build();
    }

    private FluidRouterUpgradeConfig() {
    }
}

