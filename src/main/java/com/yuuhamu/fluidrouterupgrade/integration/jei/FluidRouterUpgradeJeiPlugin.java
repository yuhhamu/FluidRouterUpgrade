package com.yuuhamu.fluidrouterupgrade.integration.jei;

import com.yuuhamu.fluidrouterupgrade.client.JeiFluidTooltipBridge;
import me.desht.modularrouters.client.gui.ModularRouterScreen;
import me.desht.modularrouters.client.gui.module.ModuleScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IModIdHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class FluidRouterUpgradeJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath("fluidrouterupgrade", "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(ModularRouterScreen.class, new FluidModuleGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(ModuleScreen.class, new FluidModuleGhostIngredientHandler<>());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        IIngredientManager ingredientManager = jeiRuntime.getIngredientManager();
        IModIdHelper modIdHelper = jeiRuntime.getJeiHelpers().getModIdHelper();
        IIngredientRenderer<FluidStack> renderer = ingredientManager.getIngredientRenderer(NeoForgeTypes.FLUID_STACK);
        JeiFluidTooltipBridge.register(fluidStack -> {
            TooltipFlag flag = Minecraft.getInstance().options.advancedItemTooltips
                    ? TooltipFlag.Default.ADVANCED
                    : TooltipFlag.Default.NORMAL;
            List<Component> result = new ArrayList<>(renderer.getTooltip(fluidStack, flag));
            ingredientManager.createTypedIngredient(NeoForgeTypes.FLUID_STACK, fluidStack)
                    .flatMap(modIdHelper::getModNameForTooltip)
                    .ifPresent(result::add);
            return result;
        });
    }
}
