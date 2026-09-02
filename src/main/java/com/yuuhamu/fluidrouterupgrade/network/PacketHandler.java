package com.yuuhamu.fluidrouterupgrade.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PacketHandler {

    private PacketHandler() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(PacketHandler::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                FluidBeamStartMessage.TYPE,
                FluidBeamStartMessage.STREAM_CODEC,
                FluidBeamStartMessage::handle);
        registrar.playToClient(
                FluidBeamStopMessage.TYPE,
                FluidBeamStopMessage.STREAM_CODEC,
                FluidBeamStopMessage::handle);
    }
}
