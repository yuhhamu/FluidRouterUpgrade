package com.yuuhamu.fluidrouterupgrade.mixin;

import com.yuuhamu.fluidrouterupgrade.logic.FluidFilterTag;
import me.desht.modularrouters.client.gui.AbstractMRContainerScreen;
import me.desht.modularrouters.container.ModuleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractMRContainerScreen.class, remap = false)
public abstract class ModuleFilterFluidRenderMixin {

    private static final Logger FRU_DEBUG = LogManager.getLogger("FRU-DEBUG-Render");
    private static String fru_debug_lastState = "";

    @Inject(method = {"render", "m_88315_"}, at = @At("TAIL"))
    private void fluidrouterupgrade$onRenderTail(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        AbstractContainerMenu menu = self.getMenu();
        if (!(menu instanceof ModuleMenu)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("screen=").append(self.getClass().getName()).append(" menu=").append(menu.getClass().getName()).append(" | ");
        for (Slot slot : menu.slots) {
            if (slot.index < 0 || slot.index >= 9) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            sb.append('[').append(slot.index).append(']').append(stack.getItem())
                    .append(" hasTag=").append(stack.hasTag())
                    .append(" tagged=").append(FluidFilterTag.isTaggedAsFluidFilter(stack))
                    .append(" nbt=").append(stack.hasTag() ? stack.getTag() : "none")
                    .append(' ');
        }
        String state = sb.toString();
        if (!state.equals(fru_debug_lastState)) {
            fru_debug_lastState = state;
            FRU_DEBUG.info("[FRU-DEBUG][Render] {}", state);
        }
    }
}

