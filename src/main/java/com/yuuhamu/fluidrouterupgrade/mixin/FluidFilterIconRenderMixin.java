package com.yuuhamu.fluidrouterupgrade.mixin;

import com.yuuhamu.fluidrouterupgrade.client.FluidFilterSlotRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerScreen.class, remap = false)
public abstract class FluidFilterIconRenderMixin {

    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void fluidrouterupgrade$onRenderSlotTail(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        FluidFilterSlotRenderer.renderFluidIconForSlot(guiGraphics, self, slot);
    }
}
