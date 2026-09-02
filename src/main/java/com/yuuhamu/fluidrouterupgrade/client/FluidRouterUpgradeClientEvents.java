package com.yuuhamu.fluidrouterupgrade.client;

import com.yuuhamu.fluidrouterupgrade.FluidRouterUpgradeMod;
import com.yuuhamu.fluidrouterupgrade.client.render.FluidBeamRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = FluidRouterUpgradeMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FluidRouterUpgradeClientEvents {

    private FluidRouterUpgradeClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.register(FluidBeamRenderer.class);
    }
}
