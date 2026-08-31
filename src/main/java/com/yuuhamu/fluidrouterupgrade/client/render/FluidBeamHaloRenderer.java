package com.yuuhamu.fluidrouterupgrade.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FluidBeamHaloRenderer {

    private static final class Entry {
        final Vec3 start;
        final Vec3 end;
        final int color;
        final int duration;
        int ticksLived = 0;

        Entry(Vec3 start, Vec3 end, int color, int duration) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.duration = duration;
        }

        boolean expired() {
            return ticksLived > duration;
        }
    }

    private static final List<Entry> ACTIVE = new ArrayList<>();

    private static final Map<Fluid, Integer> FLUID_COLOR_CACHE = new HashMap<>();

    private FluidBeamHaloRenderer() {
    }

    public static void add(BlockPos routerPos, BlockPos targetPos, int duration, int color, boolean reversed) {
        Vec3 routerVec = Vec3.atLowerCornerOf(routerPos).add(0.5, 0.5, 0.5);
        Vec3 targetVec = Vec3.atLowerCornerOf(targetPos).add(0.5, 0.5, 0.5);
        Vec3 start = reversed ? targetVec : routerVec;
        Vec3 end = reversed ? routerVec : targetVec;
        ACTIVE.add(new Entry(start, end, color, duration));
    }

    public static int getFluidRepresentativeColor(Fluid fluid) {
        return FLUID_COLOR_CACHE.computeIfAbsent(fluid, FluidBeamHaloRenderer::computeFluidRepresentativeColor);
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
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        ACTIVE.removeIf(e -> {
            e.ticksLived++;
            return e.expired();
        });
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

        for (Entry e : ACTIVE) {
            renderHalo(buffer, matrixStack, e);
        }
        buffer.endBatch(FluidRenderTypes.HALO_LINE);
        matrixStack.popPose();
    }

    private static void renderHalo(MultiBufferSource.BufferSource buffer, PoseStack matrixStack, Entry e) {
        Matrix4f positionMatrix = matrixStack.last().pose();
        int r = (e.color >> 16) & 0xFF;
        int g = (e.color >> 8) & 0xFF;
        int b = e.color & 0xFF;
        int alpha = 45;

        double len = e.start.distanceTo(e.end);
        if (len < 1.0e-6) {
            return;
        }
        float xn = (float) ((e.end.x() - e.start.x()) / len);
        float yn = (float) ((e.end.y() - e.start.y()) / len);
        float zn = (float) ((e.end.z() - e.start.z()) / len);

        VertexConsumer builder = buffer.getBuffer(FluidRenderTypes.HALO_LINE);
        ClientUtil.posF(builder, positionMatrix, e.start)
                .color(r, g, b, alpha)
                .normal(matrixStack.last().normal(), xn, yn, zn)
                .endVertex();
        ClientUtil.posF(builder, positionMatrix, e.end)
                .color(r, g, b, alpha)
                .normal(matrixStack.last().normal(), xn, yn, zn)
                .endVertex();
    }
}
