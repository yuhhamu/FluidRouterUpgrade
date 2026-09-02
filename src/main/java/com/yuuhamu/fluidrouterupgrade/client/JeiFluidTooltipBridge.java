package com.yuuhamu.fluidrouterupgrade.client;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class JeiFluidTooltipBridge {

    private static final Logger LOGGER = LogManager.getLogger(JeiFluidTooltipBridge.class);

    private static volatile Function<FluidStack, List<Component>> provider;

    private JeiFluidTooltipBridge() {
    }

    public static void register(Function<FluidStack, List<Component>> newProvider) {
        provider = newProvider;
    }

    public static Optional<List<Component>> getTooltip(FluidStack stack) {
        Function<FluidStack, List<Component>> current = provider;
        if (current == null) {
            return Optional.empty();
        }
        try {
            List<Component> lines = current.apply(stack);
            if (lines == null || lines.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(lines);
        } catch (Exception e) {
            LOGGER.warn("Failed to build JEI fluid tooltip for {}", stack, e);
            return Optional.empty();
        }
    }
}
