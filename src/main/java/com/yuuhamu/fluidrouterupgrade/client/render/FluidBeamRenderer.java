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
import com.mojang.math.Matrix4f;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        int width = sprite.getWidth();
        int height = sprite.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = sprite.getPixelRGBA(0, x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 16) {
                    continue;
                }
                rSum += argb & 0xFF;
                gSum += (argb >> 8) & 0xFF;
                bSum += (argb >> 16) & 0xFF;
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

        int thickAlpha = (int) (Mth.sin((gameTime % 20) / 20f * 3.1415927f) * 128 + 32);
        int thinAlpha = 192;
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
