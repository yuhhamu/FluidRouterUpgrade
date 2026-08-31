package com.yuuhamu.fluidrouterupgrade.registry;

import com.yuuhamu.fluidrouterupgrade.FluidRouterUpgradeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FluidRouterUpgradeMod.MODID);

    public static final RegistryObject<CreativeModeTab> FLUID_ROUTER_UPGRADE_TAB = REGISTRY.register("fluid_router_upgrade_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fluidrouterupgrade"))
                    .icon(() -> new ItemStack(ModItems.FLUID_MODE_UPGRADE.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.FLUID_MODE_UPGRADE.get());
                        output.accept(ModItems.TANK_UPGRADE.get());
                        output.accept(ModItems.BALANCER_AUGMENT.get());
                    })
                    .build());
}

