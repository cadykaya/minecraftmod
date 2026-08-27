package com.cadykaya.interregnum.content.block;

import com.cadykaya.interregnum.registry.ModComponents;
import com.cadykaya.interregnum.registry.ModItems;
import com.cadykaya.interregnum.system.ferry.FerryDocket;
import com.cadykaya.interregnum.system.ferry.Sailing;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
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
 * <h2>Ask with an empty hand, go with a full one</h2>
 *
 * `WORLD.md`, locked: *"No menu on the keel, no destination written by hand. **You hold a
 * letter, and the ferry reads it.** No letter, no voyage."* So the keel has two
 * affordances and they divide cleanly:
 *
 * <ul>
 *   <li><b>Empty hand</b> — the docket for all four crossings. The teaching, unchanged.</li>
 *   <li><b>A sealed letter</b> — it reads the envelope and sails.</li>
 * </ul>
 *
 * Neither had to be explained to build the other, and a player who touches a keel before
 * they have any mail is taught what the crossings want long before they can attempt one.
 *
 * <h2>The blank envelope still sails</h2>
 *
 * One of the four opens `To —`, and it is the Quiet One's. It routes like any other: see
 * {@link com.cadykaya.interregnum.core.ferry.Routing}, which explains at length why the
 * appealing rule — an unaddressed letter cannot be routed — would have made that god's
 * world permanently unreachable.
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

    /**
     * A letter, held out to the keel.
     *
     * The letter is **not consumed**. It is a document, and the god it is addressed to has
     * not read it yet -- the delivery scene at the far end is the point of carrying it, and
     * a ferry that ate the mail on departure would be a ferry that could never deliver
     * anything.
     *
     * Anything that is not one of the dead god's letters falls through to vanilla, so a
     * player putting a torch on a keel puts a torch on a keel.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!held.is(ModItems.SEALED_LETTER.get())) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer to)) {
            return InteractionResult.SUCCESS;
        }
        var done = Sailing.byLetter(server, pos, held.get(ModComponents.LETTER.get()));
        for (Component line : FerryDocket.report(done)) {
            to.sendSystemMessage(line);
        }
        return InteractionResult.SUCCESS;
    }
}
