package com.cadykaya.interregnum.content.block;

import com.cadykaya.interregnum.system.haunt.RawScript;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The carved shrine stone, and the first thing that can read one.
 *
 * <h2>It has been carrying script nobody could read since the chapter-0 art pass</h2>
 *
 * The block's own javadoc has said *"the same masonry, carrying a band of the dead god's
 * script"* from the day it was registered, and nothing anywhere could look at it. The
 * reachability audit found it and deliberately **left it**, because making it readable
 * would have settled a question `WORLD.md` still had open: whether raw script read without
 * the desk marks the reader. Settling that in the safe direction by shipping plain readable
 * inscriptions was not this file's decision to make.
 *
 * That question is now locked — *reading raw script marks the reader, and marks means the
 * ghost gets louder* — so the stone can say what it always said it said.
 *
 * <h2>What it does not give you</h2>
 *
 * Lore. See {@link RawScript}: this is the god's hand untranscribed, and the codex desk
 * exists precisely because a person cannot read it. A stone that handed over a paragraph
 * would make the desk pointless and turn the hazard into a toll paid for content.
 */
public class ShrineScriptBlock extends Block {
    public static final MapCodec<ShrineScriptBlock> CODEC = simpleCodec(ShrineScriptBlock::new);

    public ShrineScriptBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        // Server only: the light, the ledger and the god's attention are all the server's.
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer reader)) {
            return InteractionResult.SUCCESS;
        }
        var outcome = RawScript.read(server, pos, reader.getUUID());
        for (Component line : RawScript.saidTo(outcome)) {
            reader.sendSystemMessage(line);
        }
        return InteractionResult.SUCCESS;
    }
}
