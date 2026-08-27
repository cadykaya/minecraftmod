package com.cadykaya.interregnum.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A Warden, standing still.
 *
 * In Chapter 0 these are scenery. Players walk past them, build around them, put
 * them in gardens. That is the entire point: the mod hands you a decorative statue
 * for hours, and the moment the god dies **every one of them on the server opens
 * its eyes at once** -- including the ones in your garden.
 *
 * `WOKEN` is a blockstate rather than anything cleverer so that it is visible,
 * persistent, and free: no block entity, no ticking, no per-statue bookkeeping. A
 * statue is a block that knows one thing about itself.
 */
public class WardenStatueBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<WardenStatueBlock> CODEC = simpleCodec(WardenStatueBlock::new);
    public static final BooleanProperty WOKEN = BooleanProperty.create("woken");

    public WardenStatueBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WOKEN, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, WOKEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Faces the placer, like every other statue-ish block in the game. A woken
        // world hands you woken statues: a Warden carved after the death does not
        // get to be asleep.
        boolean woken = !com.cadykaya.interregnum.system.ChapterSavedData
                .isDormant(context.getLevel());
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WOKEN, woken);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Open this statue's eyes, if they are not already open. */
    public static boolean wake(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof WardenStatueBlock) || state.getValue(WOKEN)) {
            return false;
        }
        level.setBlock(pos, state.setValue(WOKEN, true), 3);
        return true;
    }
}
