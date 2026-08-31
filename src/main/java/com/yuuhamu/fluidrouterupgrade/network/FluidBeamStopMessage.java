package com.yuuhamu.fluidrouterupgrade.network;

import com.yuuhamu.fluidrouterupgrade.client.render.FluidBeamRenderer;
import com.yuuhamu.fluidrouterupgrade.logic.FluidBeamKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 転送ビームの描画を即座に終了するようクライアントへ伝えるメッセージ。
 * サーバー側で、直前の稼働タイミングまでアクティブだったキーが、
 * 次の稼働タイミングで報告されなかった(=輸送が行われなかった)時点で送信される。
 */
public class FluidBeamStopMessage {
    private final BlockPos routerPos;
    private final BlockPos targetPos;
    private final boolean isPull;
    private final boolean crossDimensionSender;

    public FluidBeamStopMessage(BlockPos routerPos, BlockPos targetPos, boolean isPull, boolean crossDimensionSender) {
        this.routerPos = routerPos;
        this.targetPos = targetPos;
        this.isPull = isPull;
        this.crossDimensionSender = crossDimensionSender;
    }

    FluidBeamStopMessage(FriendlyByteBuf buf) {
        this.routerPos = buf.readBlockPos();
        this.targetPos = buf.readBlockPos();
        this.isPull = buf.readBoolean();
        this.crossDimensionSender = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(routerPos);
        buf.writeBlockPos(targetPos);
        buf.writeBoolean(isPull);
        buf.writeBoolean(crossDimensionSender);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            FluidBeamKey key = new FluidBeamKey(routerPos, targetPos, isPull, crossDimensionSender);
            FluidBeamRenderer.stop(key);
        });
        ctx.get().setPacketHandled(true);
    }
}
