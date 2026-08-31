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

/**
 * 転送ビームの描画を開始する(または既に表示中であれば何もしない)よう
 * クライアントへ伝えるメッセージ。durationのような自動失効時間は持たない。
 * 稼働タイミングごとの継続判定はサーバー側のRouterUpgradeCore.reportBeamActiveが
 * 行い、新規開始と判定された場合にのみこのメッセージが送信される。
 */
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
