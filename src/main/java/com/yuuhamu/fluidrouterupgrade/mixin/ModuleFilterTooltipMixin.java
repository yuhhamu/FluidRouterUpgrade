package com.yuuhamu.fluidrouterupgrade.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuuhamu.fluidrouterupgrade.client.FluidFilterSlotRenderer;
import me.desht.modularrouters.container.ModuleMenu;
import net.minecraft.client.gui.screens.Screen;
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
 * SRG名も1.20.1(m_280072_)と異なる(javap/tsrgと合わせて確認: 1.19.2はm_7025_)。
 * renderComponentTooltipの実体はScreenのprotected renderComponentTooltipだが、ModularRouterScreenMixinと
 * 同じ「Screenへの偽装継承」パターンで呼ぶ必要がある。
 *
 * 追記(2026-09-01、実機Prism起動テストで発見): このMixinの対象(@Mixin value)はAbstractContainerScreen
 * そのもの(modクラスではなく素のvanillaクラス)なので、偽装継承の対象として同じAbstractContainerScreenを
 * extendsすると、SpongePowered Mixinの階層検証で
 * 「Super class 'AbstractContainerScreen' of ModuleFilterTooltipMixin was not found in the hierarchy of
 * target class 'AbstractContainerScreen'」というエラーで実行時に失敗する(ビルド自体は通ってしまうため、
 * SERVER dev-run(GUIクラスに触れない)では検出できず、実際のCLIENT起動で初めて顕在化した)。
 * 偽装継承で使うクラスは「Mixin対象クラス自身」ではなく「Mixin対象クラスの実際の親クラス」でなければ
 * ならないため、AbstractContainerScreenの親であるScreenを継承するよう修正した
 * (renderComponentTooltipはScreen自身がprotectedで宣言しているため、Screen継承でも同様にアクセス可能)。
 */
@Mixin(value = AbstractContainerScreen.class, remap = false)
public abstract class ModuleFilterTooltipMixin extends Screen {

    protected ModuleFilterTooltipMixin(Component title) {
        super(title);
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
