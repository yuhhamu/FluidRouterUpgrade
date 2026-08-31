package com.yuuhamu.fluidrouterupgrade.network;

import com.yuuhamu.fluidrouterupgrade.client.render.FluidBeamHaloRenderer;
import me.desht.modularrouters.block.tile.ModularRouterBlockEntity;
import me.desht.modularrouters.client.util.ClientUtil;
import me.desht.modularrouters.util.BeamData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class FluidBeamMessage {
    private final BlockPos routerPos;
    private final BlockPos targetPos;
    private final int duration;
    private final int baseColor;
    private final boolean reversed;
    private final boolean fade;
    @Nullable
    private final ResourceLocation fluidId;

    public FluidBeamMessage(BlockPos routerPos, BlockPos targetPos, int duration, int baseColor,
                             boolean reversed, boolean fade, @Nullable ResourceLocation fluidId) {
        this.routerPos = routerPos;
        this.targetPos = targetPos;
        this.duration = duration;
        this.baseColor = baseColor;
        this.reversed = reversed;
        this.fade = fade;
        this.fluidId = fluidId;
    }

    FluidBeamMessage(FriendlyByteBuf buf) {
        this.routerPos = buf.readBlockPos();
        this.targetPos = buf.readBlockPos();
        this.duration = buf.readVarInt();
        this.baseColor = buf.readInt();
        this.reversed = buf.readBoolean();
        this.fade = buf.readBoolean();
        this.fluidId = buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(routerPos);
        buf.writeBlockPos(targetPos);
        buf.writeVarInt(duration);
        buf.writeInt(baseColor);
        buf.writeBoolean(reversed);
        buf.writeBoolean(fade);
        buf.writeBoolean(fluidId != null);
        if (fluidId != null) {
            buf.writeResourceLocation(fluidId);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Level level = ClientUtil.theClientWorld();
            if (level == null || !(level.getBlockEntity(routerPos) instanceof ModularRouterBlockEntity router)) {
                return;
            }
            BeamData data = new BeamData(duration, targetPos, baseColor);
            if (reversed) {
                data = data.reverseItems();
            }
            if (fade) {
                data = data.fadeItems();
            }
            router.addItemBeam(data);

            if (fluidId != null) {
                Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    int fluidColor = FluidBeamHaloRenderer.getFluidRepresentativeColor(fluid);
                    FluidBeamHaloRenderer.add(routerPos, targetPos, duration, fluidColor, reversed);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
