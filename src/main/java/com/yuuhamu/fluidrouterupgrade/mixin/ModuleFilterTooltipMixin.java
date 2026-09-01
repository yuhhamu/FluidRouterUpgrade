package com.yuuhamu.fluidrouterupgrade.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuuhamu.fluidrouterupgrade.client.FluidFilterSlotRenderer;
import me.desht.modularrouters.container.ModuleMenu;
import net.minecraft.client.Minecraft;
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

/*
 * 1.19.2向け移植メモ(2026-09-01): renderTooltip(PoseStack,int,int)は1.19.2ではGuiGraphics化されておらず
 * SRG名も1.20.1(m_280072_)と異なる(javap/tsrg照合で確認: 1.19.2はm_7025_)。
 * GuiGraphics#renderComponentTooltipの代わりにScreenのprotected renderComponentTooltipを、
 * ModularRouterScreenMixinと同じ「Screenへの見せかけ継承」パターンで呼ぶ必要があるが、
 * このMixinの対象はAbstractContainerScreen自体(mod classではなく素のvanillaクラス)なので、
 * remap=falseのままキャストで直接protectedメソッドへアクセスできず、代わりに
 * AbstractContainerScreenを継承するダミーサブクラスパターンを使う。
 */
@Mixin(value = AbstractContainerScreen.class, remap = false)
public abstract class ModuleFilterTooltipMixin extends AbstractContainerScreen<AbstractContainerMenu> {

    protected ModuleFilterTooltipMixin(AbstractContainerMenu menu, net.minecraft.world.entity.player.Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = {"renderTooltip", "m_7025_"}, at = @At("HEAD"), cancellable = true)
    private void fluidrouterupgrade$onRenderTooltip(PoseStack poseStack, int x, int y, CallbackInfo ci) {
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
        this.renderComponentTooltip(poseStack, lines.get(), x, y);
        ci.cancel();
    }
}
