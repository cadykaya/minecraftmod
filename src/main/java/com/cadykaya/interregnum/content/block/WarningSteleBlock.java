package com.cadykaya.interregnum.content.block;

import com.cadykaya.interregnum.system.stele.SteleReading;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A warning stele, and the first thing that can read one.
 *
 * The block, its texture and its model have existed since the chapter-0 art pass. There
 * was **no text on it anywhere** — and the shrine-keeper has been telling players for
 * just as long that *"the steles are readable if you have the light for it; most people
 * don't bother, and I have never held it against anybody."* A shipped line of dialogue
 * describing a rule nothing implements is worse than a missing feature: it is the mod
 * lying in its own voice.
 *
 * See {@link SteleReading} for what it says and {@link
 * com.cadykaya.interregnum.core.stele.Steles} for which notice stands where — and for why
 * the words never change at the deicide, which is the joke `WORLD.md` names when it lists
 * *"steles that re-read differently"* among the things comedy is allowed to live in.
 *
 * Still a {@link RotatedPillarBlock}: a builder can lay one on its side, and a toppled one
 * reads as toppled. Reading it while it is on its side is allowed, and so is reading one
 * somebody has built into a wall — it is a public notice, not a puzzle.
 */
public class WarningSteleBlock extends RotatedPillarBlock {
    public static final MapCodec<WarningSteleBlock> CODEC = simpleCodec(WarningSteleBlock::new);

    public WarningSteleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        // Server only: light and position are the server's, and a client that guessed
        // would show a notice the server disagreed with.
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer reader)) {
            return InteractionResult.SUCCESS;
        }
        for (Component line : SteleReading.of(server, pos)) {
            reader.sendSystemMessage(line);
        }
        return InteractionResult.SUCCESS;
    }
}
