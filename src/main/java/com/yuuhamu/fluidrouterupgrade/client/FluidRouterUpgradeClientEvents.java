package com.yuuhamu.fluidrouterupgrade.client;

import com.yuuhamu.fluidrouterupgrade.FluidRouterUpgradeMod;
import com.yuuhamu.fluidrouterupgrade.client.render.FluidBeamRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = FluidRouterUpgradeMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FluidRouterUpgradeClientEvents {

    private FluidRouterUpgradeClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(FluidBeamRenderer.class);
    }
}
