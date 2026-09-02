package com.yuuhamu.fluidrouterupgrade.registry;

import com.yuuhamu.fluidrouterupgrade.FluidRouterUpgradeMod;
import com.yuuhamu.fluidrouterupgrade.item.BalancerAugment;
import com.yuuhamu.fluidrouterupgrade.item.FluidModeUpgrade;
import com.yuuhamu.fluidrouterupgrade.item.TankUpgrade;
import me.desht.modularrouters.item.upgrade.UpgradeItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister<Item> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.ITEM, FluidRouterUpgradeMod.MODID);

    public static final DeferredHolder<Item, UpgradeItem> FLUID_MODE_UPGRADE = REGISTRY.register("fluid_mode_upgrade",
            FluidModeUpgrade::new);

    public static final DeferredHolder<Item, UpgradeItem> TANK_UPGRADE = REGISTRY.register("tank_upgrade",
            TankUpgrade::new);

    public static final DeferredHolder<Item, Item> BALANCER_AUGMENT = REGISTRY.register("balancer_augment",
            BalancerAugment::new);
}
