package com.yuuhamu.fluidrouterupgrade.mixin;

import com.yuuhamu.fluidrouterupgrade.client.FluidFilterSlotRenderer;
import me.desht.modularrouters.container.ModuleMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(value = AbstractContainerScreen.class, remap = false)
public abstract class ModuleFilterTooltipMixin {

    @Inject(method = {"renderTooltip", "m_280072_"}, at = @At("HEAD"), cancellable = true)
    private void fluidrouterupgrade$onRenderTooltip(GuiGraphics guiGraphics, int x, int y, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        AbstractContainerMenu menu = self.getMenu();
        if (!(menu instanceof ModuleMenu)) {
            return;
        }
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        Slot hovered = self.getSlotUnderMouse();
        Optional<List<Component>> lines = FluidFilterSlotRenderer.getFluidTooltipLines(hovered);
        if (lines.isEmpty()) {
            return;
        }
        guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, lines.get(), x, y);
        ci.cancel();
    }
}
