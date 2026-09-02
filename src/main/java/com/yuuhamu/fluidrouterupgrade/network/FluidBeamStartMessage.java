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

import java.util.Optional;

public record FluidBeamStartMessage(BlockPos routerPos, BlockPos targetPos, int beamColor, boolean isPull,
                                     boolean crossDimensionSender, Optional<ResourceLocation> fluidId)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FluidBeamStartMessage> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FluidRouterUpgradeMod.MODID, "fluid_beam_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidBeamStartMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FluidBeamStartMessage::routerPos,
            BlockPos.STREAM_CODEC, FluidBeamStartMessage::targetPos,
            ByteBufCodecs.INT, FluidBeamStartMessage::beamColor,
            ByteBufCodecs.BOOL, FluidBeamStartMessage::isPull,
            ByteBufCodecs.BOOL, FluidBeamStartMessage::crossDimensionSender,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), FluidBeamStartMessage::fluidId,
            FluidBeamStartMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FluidBeamStartMessage payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Integer haloColor = FluidBeamRenderer.resolveHaloColor(payload.fluidId().orElse(null));
            FluidBeamKey key = new FluidBeamKey(payload.routerPos(), payload.targetPos(), payload.isPull(), payload.crossDimensionSender());
            FluidBeamRenderer.start(key, payload.routerPos(), payload.targetPos(), payload.beamColor(), haloColor);
        });
    }
}
