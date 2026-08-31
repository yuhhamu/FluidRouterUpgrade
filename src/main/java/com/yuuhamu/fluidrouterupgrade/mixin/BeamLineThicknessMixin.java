package com.yuuhamu.fluidrouterupgrade.mixin;

import me.desht.modularrouters.client.render.ModRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = ModRenderTypes.class, remap = false)
public class BeamLineThicknessMixin {
    private static final double SCALE = 2.0 / 3.0;

    @ModifyConstant(method = "<clinit>", constant = @Constant(doubleValue = 10.0))
    private static double fluidrouterupgrade$scaleThickBeam(double original) {
        return original * SCALE;
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(doubleValue = 3.0))
    private static double fluidrouterupgrade$scaleThinBeam(double original) {
        return original * SCALE;
    }
}
