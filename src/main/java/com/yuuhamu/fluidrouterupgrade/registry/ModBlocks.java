package com.yuuhamu.fluidrouterupgrade.registry;

import com.yuuhamu.fluidrouterupgrade.FluidRouterUpgradeMod;
import com.yuuhamu.fluidrouterupgrade.block.FluidRouterVisualBlock;
import com.yuuhamu.fluidrouterupgrade.logic.FluidRouterModeProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> REGISTRY =
            DeferredRegister.create(ForgeRegistries.BLOCKS, FluidRouterUpgradeMod.MODID);

    private static final BlockBehaviour.Properties VISUAL_PROPS = BlockBehaviour.Properties.of(Material.METAL)
            .color(MaterialColor.METAL)
            .strength(1.5f, 6.0f)
            .sound(SoundType.METAL)
            .noOcclusion();

    public static final RegistryObject<Block> FLUID_ROUTER_VISUAL = REGISTRY.register("fluid_router_visual",
            () -> new FluidRouterVisualBlock(VISUAL_PROPS, FluidRouterModeProvider.IMAGE_COLOR));
}

