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
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * 使って描画する。太線部分はVanilla本体と同じ1秒周期のアルファ点滅
 * (getGameTime()基準のsin波)を再現し、ハローラインにも同じ位相の点滅を
 * 持たせている(ユーザー要望により、開始/継続/終了の切り替え自体が
 * durationに頼らず正確になったため、点滅演出そのものは復活させている)。
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

    /**
     * リログイン・チャンク再読込時に受け取ったサーバー側のスナップショットで、
     * このrouterに属するビーム表示をスナップショットと完全に一致させる(差分適用)。
     *
     * FluidBeamStartMessage/FluidBeamStopMessageは「現在オンラインで見ているクライアント」
     * にのみ届く即時イベント通知であり、切断中のクライアントには一切届かない。
     * 一方でBeamContinuityRegistry(RouterUpgradeCore側、サーバー)の状態は接続断・
     * 再接続をまたいでrouterの実体に紐付いたまま持続するため、再接続後に輸送が
     * 継続していても「既にアクティブ」と判定されて開始イベントが再送されず、
     * 再接続したクライアントのACTIVE(このクラス)は空のままになる
     * ―― これがリログイン時にビーム描画が消える根本原因と考えられる。
     *
     * 対策として、既存のタンク内容量と全く同じ経路(getUpdateTag/handleUpdateTag。
     * チャンク読込・再読込のたびに必ず呼ばれることは、この経路で運ばれている
     * タンク内容量がリログインでも正しく復元されている実績から確認済み)で、
     * 稼働中ビームのスナップショットも一緒に送るようにし、受信のたびにこの
     * routerに属する表示をスナップショットへ同期する(スナップショットに無い
     * キーは停止、あるキーは開始または更新)。
     */
    public static void syncRouter(BlockPos routerPos, List<SyncedBeam> beams) {
        Set<FluidBeamKey> keep = new HashSet<>();
        for (SyncedBeam beam : beams) {
            FluidBeamKey key = new FluidBeamKey(routerPos, beam.targetPos(), beam.isPull(), beam.crossDimensionSender());
            keep.add(key);
            start(key, routerPos, beam.targetPos(), beam.beamColor(), resolveHaloColor(beam.fluidId()));
        }
        ACTIVE.keySet().removeIf(key -> key.routerPos().equals(routerPos) && !keep.contains(key));
    }

    @Nullable
    public static Integer resolveHaloColor(@Nullable ResourceLocation fluidId) {
        if (fluidId == null) {
            return null;
        }
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return null;
        }
        return getFluidRepresentativeColor(fluid);
    }

    /**
     * サーバーのgetUpdateTagで運ばれる、稼働中ビーム1件分のスナップショット
     * (routerPosは呼び出し側で分かっているため含めない)。
     */
    public record SyncedBeam(BlockPos targetPos, boolean isPull, boolean crossDimensionSender,
                              int beamColor, @Nullable ResourceLocation fluidId) {
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

        long gameTime = level.getGameTime();
        for (Entry e : ACTIVE.values()) {
            renderBeam(buffer, matrixStack, e, gameTime);
        }
        buffer.endBatch(ModRenderTypes.BEAM_LINE_THICK);
        buffer.endBatch(ModRenderTypes.BEAM_LINE_THIN);
        buffer.endBatch(FluidRenderTypes.HALO_LINE);
        matrixStack.popPose();
    }

    private static void renderBeam(MultiBufferSource.BufferSource buffer, PoseStack matrixStack, Entry e, long gameTime) {
        Matrix4f positionMatrix = matrixStack.last().pose();
        double len = e.start.distanceTo(e.end);
        if (len < 1.0e-6) {
            return;
        }
        float xn = (float) ((e.end.x() - e.start.x()) / len);
        float yn = (float) ((e.end.y() - e.start.y()) / len);
        float zn = (float) ((e.end.z() - e.start.z()) / len);

        // Vanilla本体のModularRouterBER#renderBeamLineと同じ1秒周期のsin波(alpha 32〜160)。
        int thickAlpha = (int) (Mth.sin((gameTime % 20) / 20f * 3.1415927f) * 128 + 32);
        // Vanilla本体の細線部分は固定値192(点滅しない)。
        int thinAlpha = 192;
        // ハローラインは、上と同じ位相でピーク45・トラフ9になるよう比率を縮小して点滅させる。
        int haloAlpha = (int) (Mth.sin((gameTime % 20) / 20f * 3.1415927f) * 36 + 9);

        int br = (e.beamColor >> 16) & 0xFF;
        int bg = (e.beamColor >> 8) & 0xFF;
        int bb = e.beamColor & 0xFF;

        VertexConsumer thick = buffer.getBuffer(ModRenderTypes.BEAM_LINE_THICK);
        ClientUtil.posF(thick, positionMatrix, e.start)
                .color(br, bg, bb, thickAlpha)
                .normal(matrixStack.last().normal(), xn, yn, zn)
                .endVertex();
        ClientUtil.posF(thick, positionMatrix, e.end)
                .color(br, bg, bb, thickAlpha)
                .normal(matrixStack.last().normal(), xn, yn, zn)
                .endVertex();

        VertexConsumer thin = buffer.getBuffer(ModRenderTypes.BEAM_LINE_THIN);
        ClientUtil.posF(thin, positionMatrix, e.start)
                .color(br, bg, bb, thinAlpha)
                .normal(matrixStack.last().normal(), xn, yn, zn)
                .endVertex();
        ClientUtil.posF(thin, positionMatrix, e.end)
                .color(br, bg, bb, thinAlpha)
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
                .color(hr, hg, hb, haloAlpha)
                .normal(matrixStack.last().normal(), xn, yn, zn)
                .endVertex();
        ClientUtil.posF(halo, positionMatrix, e.end)
                .color(hr, hg, hb, haloAlpha)
                .normal(matrixStack.last().normal(), xn, yn, zn)
                .endVertex();
    }
}
