package com.yuuhamu.fluidrouterupgrade.registry;

import com.yuuhamu.fluidrouterupgrade.FluidRouterUpgradeMod;
import com.yuuhamu.fluidrouterupgrade.block.FluidRouterVisualBlock;
import com.yuuhamu.fluidrouterupgrade.logic.FluidRouterModeProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister<Block> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.BLOCK, FluidRouterUpgradeMod.MODID);

    private static final BlockBehaviour.Properties VISUAL_PROPS = BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(1.5f, 6.0f)
            .sound(SoundType.METAL)
            .noOcclusion();

    public static final DeferredHolder<Block, Block> FLUID_ROUTER_VISUAL = REGISTRY.register("fluid_router_visual",
            () -> new FluidRouterVisualBlock(VISUAL_PROPS, FluidRouterModeProvider.IMAGE_COLOR));
}
