package com.yuuhamu.fluidrouterupgrade;

import com.yuuhamu.fluidrouterupgrade.config.FluidRouterUpgradeConfig;
import com.yuuhamu.fluidrouterupgrade.logic.FluidRouterModeProvider;
import com.yuuhamu.fluidrouterupgrade.network.PacketHandler;
import com.yuuhamu.fluidrouterupgrade.registry.ModBlocks;
import com.yuuhamu.fluidrouterupgrade.registry.ModCreativeTabs;
import com.yuuhamu.fluidrouterupgrade.registry.ModItems;
import com.yuuhamu.routerupgradecore.api.RouterUpgradeCore;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FluidRouterUpgradeMod.MODID)
public class FluidRouterUpgradeMod {

    public static final String MODID = "fluidrouterupgrade";

    public FluidRouterUpgradeMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.REGISTRY.register(modEventBus);
        ModBlocks.REGISTRY.register(modEventBus);
        // 1.19.2向け: CreativeModeTabはフィールド初期化時にTABS配列へ自己登録するため、クラス初期化のみを明示的にトリガーする
        ModCreativeTabs.FLUID_ROUTER_UPGRADE_TAB.getClass();

        modEventBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FluidRouterUpgradeConfig.SPEC);

        PacketHandler.register();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
                RouterUpgradeCore.registerMode(ModItems.FLUID_MODE_UPGRADE.get(), new FluidRouterModeProvider(),
                        FluidRouterModeProvider.IMAGE_COLOR));
    }
}

