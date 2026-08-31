package com.yuuhamu.fluidrouterupgrade.client;

import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class JeiFluidTooltipBridge {

    private static final Logger FRU_DEBUG = LogManager.getLogger("FRU-DEBUG-TooltipBridge");
    private static boolean fru_debug_loggedNullProvider = false;

    private static volatile Function<FluidStack, List<Component>> provider;

    private JeiFluidTooltipBridge() {
    }

    public static void register(Function<FluidStack, List<Component>> newProvider) {
        FRU_DEBUG.info("[FRU-DEBUG][TooltipBridge] register() called, newProvider={}", newProvider);
        provider = newProvider;
    }

    public static Optional<List<Component>> getTooltip(FluidStack stack) {
        Function<FluidStack, List<Component>> current = provider;
        if (current == null) {
            if (!fru_debug_loggedNullProvider) {
                fru_debug_loggedNullProvider = true;
                FRU_DEBUG.info("[FRU-DEBUG][TooltipBridge] provider is null (register() never called, or JEI not present)");
            }
            return Optional.empty();
        }
        try {
            List<Component> lines = current.apply(stack);
            if (lines == null || lines.isEmpty()) {
                FRU_DEBUG.info("[FRU-DEBUG][TooltipBridge] provider returned null/empty for stack={}", stack);
                return Optional.empty();
            }
            return Optional.of(lines);
        } catch (Exception e) {
            FRU_DEBUG.info("[FRU-DEBUG][TooltipBridge] provider threw exception for stack={}: {}", stack, e.toString());
            return Optional.empty();
        }
    }
}

