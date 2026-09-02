package com.yuuhamu.fluidrouterupgrade;

import com.yuuhamu.fluidrouterupgrade.config.FluidRouterUpgradeConfig;
import com.yuuhamu.fluidrouterupgrade.logic.FluidRouterModeProvider;
import com.yuuhamu.fluidrouterupgrade.network.PacketHandler;
import com.yuuhamu.fluidrouterupgrade.registry.ModBlocks;
import com.yuuhamu.fluidrouterupgrade.registry.ModCreativeTabs;
import com.yuuhamu.fluidrouterupgrade.registry.ModDataComponents;
import com.yuuhamu.fluidrouterupgrade.registry.ModItems;
import com.yuuhamu.routerupgradecore.api.RouterUpgradeCore;
import me.desht.modularrouters.core.ModBlockEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(FluidRouterUpgradeMod.MODID)
public class FluidRouterUpgradeMod {

    public static final String MODID = "fluidrouterupgrade";

    public FluidRouterUpgradeMod(IEventBus modEventBus, ModContainer container) {
        ModItems.REGISTRY.register(modEventBus);
        ModBlocks.REGISTRY.register(modEventBus);
        ModCreativeTabs.REGISTRY.register(modEventBus);
        ModDataComponents.REGISTRY.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(FluidRouterUpgradeMod::registerCapabilities);

        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, FluidRouterUpgradeConfig.SPEC);

        PacketHandler.register(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
                RouterUpgradeCore.registerMode(ModItems.FLUID_MODE_UPGRADE.get(), new FluidRouterModeProvider(),
                        FluidRouterModeProvider.IMAGE_COLOR));
    }

    // NeoForgeではModularRouterBlockEntity#getCapabilityがオーバーライドされていないため、
    // RegisterCapabilitiesEventでBlockCapability(FLUID_HANDLER)をRouter自身に登録し、
    // RouterUpgradeCore.getActiveCapability経由で現在アクティブなRouterModeProviderへ委譲する。
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.MODULAR_ROUTER.get(),
                (router, side) -> RouterUpgradeCore.getActiveCapability(router, Capabilities.FluidHandler.BLOCK, side));
    }
}
