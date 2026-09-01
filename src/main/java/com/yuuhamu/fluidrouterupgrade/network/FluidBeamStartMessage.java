package com.yuuhamu.fluidrouterupgrade.network;

import com.yuuhamu.fluidrouterupgrade.client.render.FluidBeamRenderer;
import com.yuuhamu.fluidrouterupgrade.logic.FluidBeamKey;
import me.desht.modularrouters.client.util.ClientUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class FluidBeamStartMessage {
    private final BlockPos routerPos;
    private final BlockPos targetPos;
    private final int beamColor;
    private final boolean isPull;
    private final boolean crossDimensionSender;
    @Nullable
    private final ResourceLocation fluidId;

    public FluidBeamStartMessage(BlockPos routerPos, BlockPos targetPos, int beamColor, boolean isPull,
                                  boolean crossDimensionSender, @Nullable ResourceLocation fluidId) {
        this.routerPos = routerPos;
        this.targetPos = targetPos;
        this.beamColor = beamColor;
        this.isPull = isPull;
        this.crossDimensionSender = crossDimensionSender;
        this.fluidId = fluidId;
    }

    FluidBeamStartMessage(FriendlyByteBuf buf) {
        this.routerPos = buf.readBlockPos();
        this.targetPos = buf.readBlockPos();
        this.beamColor = buf.readInt();
        this.isPull = buf.readBoolean();
        this.crossDimensionSender = buf.readBoolean();
        this.fluidId = buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(routerPos);
        buf.writeBlockPos(targetPos);
        buf.writeInt(beamColor);
        buf.writeBoolean(isPull);
        buf.writeBoolean(crossDimensionSender);
        buf.writeBoolean(fluidId != null);
        if (fluidId != null) {
            buf.writeResourceLocation(fluidId);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Level level = ClientUtil.theClientWorld();
            if (level == null) {
                return;
            }
            Integer haloColor = FluidBeamRenderer.resolveHaloColor(fluidId);
            FluidBeamKey key = new FluidBeamKey(routerPos, targetPos, isPull, crossDimensionSender);
            FluidBeamRenderer.start(key, routerPos, targetPos, beamColor, haloColor);
        });
        ctx.get().setPacketHandled(true);
    }
}
