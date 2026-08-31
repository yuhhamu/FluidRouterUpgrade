package com.yuuhamu.fluidrouterupgrade.network;

import com.yuuhamu.fluidrouterupgrade.FluidRouterUpgradeMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FluidRouterUpgradeMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    private PacketHandler() {
    }

    public static void register() {
        NETWORK.registerMessage(id++,
                FluidBeamStartMessage.class,
                FluidBeamStartMessage::toBytes,
                FluidBeamStartMessage::new,
                FluidBeamStartMessage::handle);
        NETWORK.registerMessage(id++,
                FluidBeamStopMessage.class,
                FluidBeamStopMessage::toBytes,
                FluidBeamStopMessage::new,
                FluidBeamStopMessage::handle);
    }
}
