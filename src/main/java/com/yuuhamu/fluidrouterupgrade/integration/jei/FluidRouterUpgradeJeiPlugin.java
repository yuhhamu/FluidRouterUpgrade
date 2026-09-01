package com.yuuhamu.fluidrouterupgrade.integration.jei;

import com.yuuhamu.fluidrouterupgrade.client.JeiFluidTooltipBridge;
import me.desht.modularrouters.client.gui.AbstractMRContainerScreen;
import me.desht.modularrouters.client.gui.module.AbstractModuleScreen;
import me.desht.modularrouters.client.gui.module.ActivatorModuleScreen;
import me.desht.modularrouters.client.gui.module.BreakerModuleScreen;
import me.desht.modularrouters.client.gui.module.DetectorModuleScreen;
import me.desht.modularrouters.client.gui.module.DistributorModuleScreen;
import me.desht.modularrouters.client.gui.module.ExtruderModule2Screen;
import me.desht.modularrouters.client.gui.module.FlingerModuleScreen;
import me.desht.modularrouters.client.gui.module.FluidModuleScreen;
import me.desht.modularrouters.client.gui.module.PlayerModuleScreen;
import me.desht.modularrouters.client.gui.module.VacuumModuleScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.helpers.IModIdHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class FluidRouterUpgradeJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID = new ResourceLocation("fluidrouterupgrade", "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(AbstractMRContainerScreen.class, new FluidModuleGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(AbstractModuleScreen.class, new FluidModuleGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(ActivatorModuleScreen.class, new FluidModuleGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(BreakerModuleScreen.class, new FluidModuleGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(DetectorModuleScreen.class, new FluidModuleGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(DistributorModuleScreen.class, new FluidModuleGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(ExtruderModule2Screen.class, new FluidModuleGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(FlingerModuleScreen.class, new FluidModuleGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(FluidModuleScreen.class, new FluidModuleGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(PlayerModuleScreen.class, new FluidModuleGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(VacuumModuleScreen.class, new FluidModuleGhostIngredientHandler<>());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        IIngredientManager ingredientManager = jeiRuntime.getIngredientManager();
        IModIdHelper modIdHelper = jeiRuntime.getJeiHelpers().getModIdHelper();
        IIngredientRenderer<FluidStack> renderer = ingredientManager.getIngredientRenderer(ForgeTypes.FLUID_STACK);
        JeiFluidTooltipBridge.register(fluidStack -> {
            TooltipFlag flag = Minecraft.getInstance().options.advancedItemTooltips
                    ? TooltipFlag.Default.ADVANCED
                    : TooltipFlag.Default.NORMAL;
            List<Component> result = new ArrayList<>(renderer.getTooltip(fluidStack, flag));
            ingredientManager.createTypedIngredient(ForgeTypes.FLUID_STACK, fluidStack)
                    .flatMap(modIdHelper::getModNameForTooltip)
                    .ifPresent(result::add);
            return result;
        });
    }
}
