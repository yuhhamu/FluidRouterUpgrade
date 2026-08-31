package com.yuuhamu.fluidrouterupgrade.mixin;

import com.yuuhamu.fluidrouterupgrade.logic.FluidFilterTag;
import me.desht.modularrouters.network.ModuleFilterMessage;
import me.desht.modularrouters.network.PacketHandler;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "me.desht.modularrouters.integration.jei.GhostTarget", remap = false)
public abstract class ModularRoutersGhostTargetFluidFixMixin {

    private static final Logger FRU_DEBUG = LogManager.getLogger("FRU-DEBUG-GhostTargetFix");

    @Shadow
    public abstract Slot slot();

    @Inject(method = "accept", at = @At("HEAD"), cancellable = true)
    private void fluidrouterupgrade$interceptFluidStack(Object ingredient, CallbackInfo ci) {
        if (!(ingredient instanceof FluidStack fluidStack)) {
            return;
        }
        ItemStack bucket = FluidUtil.getFilledBucket(fluidStack);
        FRU_DEBUG.info("[FRU-DEBUG][GhostTargetFix] ModularRouters native GhostTarget#accept intercepted "
                        + "fluidStack={} bucket={} bucketEmpty={} slot={}",
                fluidStack, bucket, bucket.isEmpty(), slot().index);
        if (!bucket.isEmpty()) {
            ItemStack tagged = FluidFilterTag.createFluidFilterStack(bucket);
            PacketHandler.NETWORK.sendToServer(new ModuleFilterMessage(slot().index, tagged));
        }
        ci.cancel();
    }
}

