package com.cadykaya.interregnum.content.block;

import com.cadykaya.interregnum.system.ferry.FerryDocket;
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
 * The mail-ferry's keel, and the one thing a player can do with it by hand.
 *
 * <h2>Inspection, not departure</h2>
 *
 * `WORLD.md`, locked: *"a keel block captures the structure, validates it against the
 * destination's law, and re-places it at the far pad. <b>The validation checklist teaches
 * each world's rule before arrival</b>."* Touching the keel runs the capture and hands
 * back the docket — see {@link FerryDocket}, which is where the page is built and why it
 * is not built here.
 *
 * <h2>It does not sail, and that is recorded rather than forgotten</h2>
 *
 * Nothing in `WORLD.md` says how a player names the destination, and the options are not
 * interchangeable: a keel that cycles four worlds is a menu, while a ferry that goes where
 * the letter in your hand is addressed is the mod's central image — *"you are the only one
 * carrying their mail"* — made mechanical. That is a design decision rather than plumbing,
 * so it is in HANDOFF under "Waiting on owner" and this ships the half that is locked.
 */
public class FerryKeelBlock extends Block {
    public static final MapCodec<FerryKeelBlock> CODEC = simpleCodec(FerryKeelBlock::new);

    public FerryKeelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        // Every line of the docket is read off the claim ledger and the loaded crossing
        // laws, and a client has neither -- a client that guessed would show a page the
        // server disagreed with, which is worse than showing none.
        //
        // `sendSystemMessage`, not `displayClientMessage`: 26.2 removed the latter from
        // LivingEntity and the replacement is on ServerPlayer. PLATFORM.md carries the row.
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer to)) {
            return InteractionResult.SUCCESS;
        }
        for (Component line : FerryDocket.of(server, pos)) {
            to.sendSystemMessage(line);
        }
        return InteractionResult.SUCCESS;
    }
}
