package com.yuuhamu.fluidrouterupgrade.block;

import com.yuuhamu.routerupgradecore.api.RouterVisualBlock;
import me.desht.modularrouters.block.ModularRouterBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class FluidRouterVisualBlock extends Block implements RouterVisualBlock {

    private final int imageColor;

    public FluidRouterVisualBlock(Properties props, int imageColor) {
        super(props);
        this.imageColor = imageColor;
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(ModularRouterBlock.FACING, Direction.NORTH)
                .setValue(ModularRouterBlock.ACTIVE, false));
    }

    @Override
    public int getRouterImageColor() {
        return imageColor;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ModularRouterBlock.FACING, ModularRouterBlock.ACTIVE);
    }
}
