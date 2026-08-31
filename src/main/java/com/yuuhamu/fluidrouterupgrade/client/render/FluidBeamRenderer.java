package com.yuuhamu.fluidrouterupgrade.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yuuhamu.fluidrouterupgrade.logic.FluidBeamKey;
import me.desht.modularrouters.client.render.ModRenderTypes;
import me.desht.modularrouters.client.util.ClientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * FluidRouterUpgradeの転送ビーム(中心ビーム+液体色ハローライン)を描画するクラス。
 *
 * Vanilla本体のBeamData/addItemBeamが持つ「durationで自動的に消える」仕組みは、
 * 「稼働タイミングで輸送が開始したら描画開始、継続していれば表示したまま、
 * 行われなかったら終了」という要件に合わないため使用せず、FluidBeamKeyを使って
 * 完全に自前でライフサイクルを管理する(start/stopの明示呼び出しのみで増減し、
 * tick経過による自動消滅は一切行わない)。
 *
 * 中心ビームはVanilla本体と同じRenderType(ModRenderTypes.BEAM_LINE_THICK/THIN)を
 * 使って描画するが、アルファ値は常に固定とし、Vanilla本体が本来かけている
 * 1秒周期の点滅は再現しない(ちらつき対策として明示的に排除している)。
 */
public class FluidBeamRenderer {

    private static final class Entry {
        final Vec3 start;
        final Vec3 end;
        final int beamColor;
        final Integer haloColor;

        Entry(Vec3 start, Vec3 end, int beamColor, Integer haloColor) {
            this.start = start;
            this.end = end;
            this.beamColor = beamColor;
            this.haloColor = haloColor;
        }
    }

    private static final Map<FluidBeamKey, Entry> ACTIVE = new HashMap<>();

    private static final Map<Fluid, Integer> FLUID_COLOR_CACHE = new HashMap<>();

    private FluidBeamRenderer() {
    }

    public static void start(FluidBeamKey key, BlockPos routerPos, BlockPos targetPos, int beamColor, Integer haloColor) {
        Vec3 routerVec = Vec3.atLowerCornerOf(routerPos).add(0.5, 0.5, 0.5);
        Vec3 targetVec = Vec3.atLowerCornerOf(targetPos).add(0.5, 0.5, 0.5);
        Vec3 start = key.isPull() ? targetVec : routerVec;
        Vec3 end = key.isPull() ? routerVec : targetVec;
        ACTIVE.put(key, new Entry(start, end, beamColor, haloColor));
    }

    public static void stop(FluidBeamKey key) {
        ACTIVE.remove(key);
    }

    public static int getFluidRepresentativeColor(Fluid fluid) {
        return FLUID_COLOR_CACHE.computeIfAbsent(fluid, FluidBeamRenderer::computeFluidRepresentativeColor);
    }

    private static int computeFluidRepresentativeColor(Fluid fluid) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        int tint = ext.getTintColor();
        int tr = (tint >> 16) & 0xFF;
        int tg = (tint >> 8) & 0xFF;
        int tb = tint & 0xFF;

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ext.getStillTexture());

        long rSum = 0;
        long gSum = 0;
        long bSum = 0;
        long count = 0;
        int width = sprite.contents().width();
        int height = sprite.contents().height();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = sprite.contents().getOriginalImage().getPixelRGBA(x, y);
                int alpha = FastColor.ABGR32.alpha(argb);
                if (alpha < 16) {
                    continue;
                }
                rSum += FastColor.ABGR32.red(argb);
                gSum += FastColor.ABGR32.green(argb);
                bSum += FastColor.ABGR32.blue(argb);
                count++;
            }
        }

        int avgR = count == 0 ? 255 : (int) (rSum / count);
        int avgG = count == 0 ? 255 : (int) (gSum / count);
        int avgB = count == 0 ? 255 : (int) (bSum / count);

        int r = avgR * tr / 255;
        int g = avgG * tg / 255;
        int b = avgB * tb / 255;
        return (r << 16) | (g << 8) | b;
    }

    @SubscribeEvent
    public static void renderWorldLastEvent(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE.isEmpty()) {
            return;
        }
        Level level = ClientUtil.theClientWorld();
        if (level == null) {
            return;
        }

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        PoseStack matrixStack = event.getPoseStack();
        matrixStack.pushPose();
        Vec3 projectedView = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        matrixStack.translate(-projectedView.x(), -projectedView.y(), -projectedView.z());

        for (Entry e : ACTIVE.values()) {
            renderBeam(buffer, matrixStack, e);
        }
        buffer.endBatch(ModRenderTypes.BEAM_LINE_THICK);
        buffer.endBatch(ModRenderTypes.BEAM_LINE_THIN);
        buffer.endBatch(FluidRenderTypes.HALO_LINE);
        matrixStack.popPose();
    }

    private static void renderBeam(MultiBufferSource.BufferSource buffer, PoseStack matrixStack, Entry e) {
        Matrix4f positionMatrix = matrixStack.last().pose();
        double len = e.start.distanceTo(e.end);
        if (len < 1.0e-6) {
            return;
        }
        float xn = (float) ((e.end.x() - e.start.x()) / len);
        float yn = (float) ((e.end.y() - e.start.y()) / len);
        float zn = (float) ((e.end.z() - e.start.z()) / len);

        int br = (e.beamColor >> 16) & 0xFF;
        int bg = (e.beamColor >> 8) & 0xFF;
        int bb = e.beamColor & 0xFF;

        VertexConsumer thick = buffer.getBuffer(ModRenderTypes.BEAM_LINE_THICK);
        ClientUtil.posF(thick, positionMatrix, e.start)
                .color(br, bg, bb, 160)
                .normal(matrixStack.last().normal(), xn, yn, zn)
                .endVertex();
        ClientUtil.posF(thick, positionMatrix, e.end)
                .color(br, bg, bb, 160)
                .normal(matrixStack.last().normal(), xn, yn, zn)
                .endVertex();

        VertexConsumer thin = buffer.getBuffer(ModRenderTypes.BEAM_LINE_THIN);
        ClientUtil.posF(thin, positionMatrix, e.start)
                .color(br, bg, bb, 192)
                .normal(matrixStack.last().normal(), xn, yn, zn)
                .endVertex();
        ClientUtil.posF(thin, positionMatrix, e.end)
                .color(br, bg, bb, 192)
                .normal(matrixStack.last().normal(), xn, yn, zn)
                .endVertex();

        if (e.haloColor == null) {
            return;
        }
        int hr = (e.haloColor >> 16) & 0xFF;
        int hg = (e.haloColor >> 8) & 0xFF;
        int hb = e.haloColor & 0xFF;
        VertexConsumer halo = buffer.getBuffer(FluidRenderTypes.HALO_LINE);
        ClientUtil.posF(halo, positionMatrix, e.start)
                .color(hr, hg, hb, 45)
                .normal(matrixStack.last().normal(), xn, yn, zn)
                .endVertex();
        ClientUtil.posF(halo, positionMatrix, e.end)
                .color(hr, hg, hb, 45)
                .normal(matrixStack.last().normal(), xn, yn, zn)
                .endVertex();
    }
}
