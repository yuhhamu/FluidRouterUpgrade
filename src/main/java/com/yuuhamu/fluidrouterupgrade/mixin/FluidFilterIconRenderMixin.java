package com.yuuhamu.fluidrouterupgrade.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuuhamu.fluidrouterupgrade.client.FluidFilterSlotRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 * 1.19.2向け移植メモ(2026-09-01): renderSlot(PoseStack,Slot)は1.19.2ではGuiGraphics化されておらず
 * SRG名も1.20.1(m_280092_)と異なる(javap/tsrg照合で確認: 1.19.2はm_97799_)。
 */
@Mixin(value = AbstractContainerScreen.class, remap = false)
public abstract class FluidFilterIconRenderMixin {

    @Inject(method = {"renderSlot", "m_97799_"}, at = @At("TAIL"))
    private void fluidrouterupgrade$onRenderSlotTail(PoseStack poseStack, Slot slot, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        FluidFilterSlotRenderer.renderFluidIconForSlot(poseStack, self, slot);
    }
}
