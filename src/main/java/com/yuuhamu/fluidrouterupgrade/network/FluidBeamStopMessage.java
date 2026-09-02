package com.yuuhamu.fluidrouterupgrade.network;

import com.yuuhamu.fluidrouterupgrade.FluidRouterUpgradeMod;
import com.yuuhamu.fluidrouterupgrade.client.render.FluidBeamRenderer;
import com.yuuhamu.fluidrouterupgrade.logic.FluidBeamKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FluidBeamStopMessage(BlockPos routerPos, BlockPos targetPos, boolean isPull, boolean crossDimensionSender)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FluidBeamStopMessage> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FluidRouterUpgradeMod.MODID, "fluid_beam_stop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidBeamStopMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FluidBeamStopMessage::routerPos,
            BlockPos.STREAM_CODEC, FluidBeamStopMessage::targetPos,
            ByteBufCodecs.BOOL, FluidBeamStopMessage::isPull,
            ByteBufCodecs.BOOL, FluidBeamStopMessage::crossDimensionSender,
            FluidBeamStopMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FluidBeamStopMessage payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            FluidBeamKey key = new FluidBeamKey(payload.routerPos(), payload.targetPos(), payload.isPull(), payload.crossDimensionSender());
            FluidBeamRenderer.stop(key);
        });
    }
}
