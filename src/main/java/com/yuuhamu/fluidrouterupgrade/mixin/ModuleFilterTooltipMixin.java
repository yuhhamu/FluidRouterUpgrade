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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

@Mixin(value = AbstractContainerScreen.class, remap = false)
public abstract class ModuleFilterTooltipMixin {

    private static final Logger FRU_DEBUG = LogManager.getLogger("FRU-DEBUG-Tooltip");
    private static String fru_debug_lastState = "";

    @Inject(method = {"renderTooltip", "m_280072_"}, at = @At("HEAD"), cancellable = true)
    private void fluidrouterupgrade$onRenderTooltip(GuiGraphics guiGraphics, int x, int y, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        AbstractContainerMenu menu = self.getMenu();
        if (!(menu instanceof ModuleMenu)) {
            return;
        }
        if (!menu.getCarried().isEmpty()) {
            String state = "carrying-item, skip";
            if (!state.equals(fru_debug_lastState)) {
                fru_debug_lastState = state;
                FRU_DEBUG.info("[FRU-DEBUG][Tooltip] {}", state);
            }
            return;
        }
        Slot hovered = self.getSlotUnderMouse();
        String hoveredDesc = hovered == null ? "null"
                : ("idx=" + hovered.index + " stack=" + hovered.getItem()
                    + " hasTag=" + hovered.getItem().hasTag()
                    + " tagged=" + com.yuuhamu.fluidrouterupgrade.logic.FluidFilterTag.isTaggedAsFluidFilter(hovered.getItem()));
        Optional<List<Component>> lines = FluidFilterSlotRenderer.getFluidTooltipLines(hovered);
        String state = "hovered=[" + hoveredDesc + "] linesPresent=" + lines.isPresent()
                + (lines.isPresent() ? (" lines=" + lines.get()) : "");
        if (!state.equals(fru_debug_lastState)) {
            fru_debug_lastState = state;
            FRU_DEBUG.info("[FRU-DEBUG][Tooltip] {}", state);
        }
        if (lines.isEmpty()) {
            return;
        }
        guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, lines.get(), x, y);
        ci.cancel();
    }

    @Inject(method = {"mouseReleased", "m_6348_"}, at = @At("HEAD"))
    private void fluidrouterupgrade$onMouseReleasedDebug(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        AbstractContainerMenu menu = self.getMenu();
        if (!(menu instanceof ModuleMenu)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("mouseReleased at (").append(mouseX).append(',').append(mouseY).append(") button=").append(button)
                .append(" guiLeft=").append(self.getGuiLeft()).append(" guiTop=").append(self.getGuiTop()).append(" | ");
        for (Slot slot : menu.slots) {
            if (slot.index < 0 || slot.index >= 9) {
                continue;
            }
            int rx = slot.x + self.getGuiLeft();
            int ry = slot.y + self.getGuiTop();
            boolean contains = mouseX >= rx && mouseX < rx + 16 && mouseY >= ry && mouseY < ry + 16;
            sb.append('[').append(slot.index).append("] rect=(").append(rx).append(',').append(ry)
                    .append(",16,16) contains=").append(contains).append(' ');
        }
        FRU_DEBUG.info("[FRU-DEBUG][MouseReleased] {}", sb.toString());
    }
}

