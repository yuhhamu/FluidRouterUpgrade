package com.yuuhamu.fluidrouterupgrade.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

public class FluidRenderTypes extends RenderType {

    private static final RenderStateShard.LineStateShard HALO_LINE_SHARD = new RenderStateShard.LineStateShard(OptionalDouble.of(18.0));

    public static final RenderType HALO_LINE = FluidRenderTypes.create(
            "fluidrouterupgrade_halo_line",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setLineState(HALO_LINE_SHARD)
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    public FluidRenderTypes(String name, VertexFormat format, VertexFormat.Mode drawMode, int bufferSize,
                             boolean useDelegate, boolean needsSorting, Runnable pre, Runnable post) {
        super(name, format, drawMode, bufferSize, useDelegate, needsSorting, pre, post);
    }
}
