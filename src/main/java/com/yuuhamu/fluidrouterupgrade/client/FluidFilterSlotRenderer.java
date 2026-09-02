package com.yuuhamu.fluidrouterupgrade.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yuuhamu.fluidrouterupgrade.logic.FluidFilterTag;
import me.desht.modularrouters.container.ModuleMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class FluidFilterSlotRenderer {

    private static final int OVERLAY_Z = 300;

    private FluidFilterSlotRenderer() {
    }

    public static void renderFluidIconForSlot(GuiGraphics guiGraphics, AbstractContainerScreen<?> screen, Slot slot) {
        AbstractContainerMenu menu = screen.getMenu();
        if (!(menu instanceof ModuleMenu)) {
            return;
        }
        if (slot.index < 0 || slot.index >= 9) {
            return;
        }
        getTaggedFluid(slot).ifPresent(fluidStack ->
                renderFluidIcon(guiGraphics, slot.x, slot.y, fluidStack));
    }

    public static Optional<List<Component>> getFluidTooltipLines(Slot hoveredSlot) {
        if (hoveredSlot == null || hoveredSlot.index < 0 || hoveredSlot.index >= 9) {
            return Optional.empty();
        }
        Optional<FluidStack> fluidOpt = getTaggedFluid(hoveredSlot);
        if (fluidOpt.isEmpty()) {
            return Optional.empty();
        }
        FluidStack fluidStack = fluidOpt.get();
        Optional<List<Component>> jeiLines = JeiFluidTooltipBridge.getTooltip(fluidStack);
        if (jeiLines.isPresent()) {
            return jeiLines;
        }
        return Optional.of(Collections.singletonList(fluidStack.getHoverName()));
    }

    private static Optional<FluidStack> getTaggedFluid(Slot slot) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty() || !FluidFilterTag.isTaggedAsFluidFilter(stack)) {
            return Optional.empty();
        }
        Optional<FluidStack> fluidOpt = FluidUtil.getFluidContained(stack);
        if (fluidOpt.isEmpty() || fluidOpt.get().isEmpty()) {
            return Optional.empty();
        }
        return fluidOpt;
    }

    private static void renderFluidIcon(GuiGraphics guiGraphics, int x, int y, FluidStack fluidStack) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ext.getStillTexture(fluidStack));
        int color = ext.getTintColor(fluidStack);
        float a = (color >>> 24) / 255f;
        if (a <= 0f) {
            a = 1f;
        }
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(r, g, b, a);
        guiGraphics.blit(x, y, OVERLAY_Z, 16, 16, sprite);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
    }
}
