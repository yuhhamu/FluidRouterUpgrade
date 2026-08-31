package com.yuuhamu.fluidrouterupgrade.registry;

import com.yuuhamu.fluidrouterupgrade.FluidRouterUpgradeMod;
import com.yuuhamu.fluidrouterupgrade.item.BalancerAugment;
import com.yuuhamu.fluidrouterupgrade.item.FluidModeUpgrade;
import com.yuuhamu.fluidrouterupgrade.item.TankUpgrade;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ITEMS, FluidRouterUpgradeMod.MODID);

    public static final RegistryObject<Item> FLUID_MODE_UPGRADE = REGISTRY.register("fluid_mode_upgrade",
            FluidModeUpgrade::new);

    public static final RegistryObject<Item> TANK_UPGRADE = REGISTRY.register("tank_upgrade",
            TankUpgrade::new);

    public static final RegistryObject<Item> BALANCER_AUGMENT = REGISTRY.register("balancer_augment",
            BalancerAugment::new);
}

