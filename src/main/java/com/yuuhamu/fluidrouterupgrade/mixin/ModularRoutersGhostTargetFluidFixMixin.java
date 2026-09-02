package com.yuuhamu.fluidrouterupgrade.mixin;

import com.yuuhamu.fluidrouterupgrade.logic.FluidFilterTag;
import me.desht.modularrouters.network.messages.ModuleFilterMessage;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "me.desht.modularrouters.integration.jei.GhostTarget", remap = false)
public abstract class ModularRoutersGhostTargetFluidFixMixin {

    @Shadow
    public abstract Slot slot();

    @Inject(method = "accept", at = @At("HEAD"), cancellable = true)
    private void fluidrouterupgrade$interceptFluidStack(Object ingredient, CallbackInfo ci) {
        if (!(ingredient instanceof FluidStack fluidStack)) {
            return;
        }
        ItemStack bucket = FluidUtil.getFilledBucket(fluidStack);
        if (!bucket.isEmpty()) {
            ItemStack tagged = FluidFilterTag.createFluidFilterStack(bucket);
            PacketDistributor.sendToServer(new ModuleFilterMessage(slot().index, tagged));
        }
        ci.cancel();
    }
}
