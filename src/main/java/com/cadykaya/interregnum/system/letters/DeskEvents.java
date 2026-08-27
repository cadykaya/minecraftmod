package com.cadykaya.interregnum.system.letters;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.registry.ModComponents;
import com.cadykaya.interregnum.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The desk, as a player reaches it.
 *
 * <h2>A lectern only behaves oddly for one of four objects</h2>
 *
 * The event is cancelled — and the lectern's own behaviour suppressed — in exactly two
 * cases: somebody offering it a sealed letter, and somebody reaching for a letter it is
 * holding. Books, quills, and every other use of a lectern in the game are untouched,
 * which is the price of using a vanilla block for this and is worth paying. See
 * {@link Desk} for why a lectern at all.
 *
 * <h2>The letter leaves your hands, and that is the cost</h2>
 *
 * `WORLD.md`: *"the safe path costs time and a trip to the desk. The unsafe path costs
 * nothing at all, which is exactly why people will take it."* A desk that copied the letter
 * while you held it would make the hazard a formality. For thirty seconds you cannot carry
 * it, deliver it, or read it, and the thing you wanted to know is exactly as unknown as it
 * was.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class DeskEvents {
    private DeskEvents() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)
                || !Desk.isDesk(level, event.getPos())) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (held.is(ModItems.SEALED_LETTER.get())) {
            lodge(event, level, player, held);
        } else if (held.isEmpty()) {
            collect(event, level, player);
        }
    }

    private static void lodge(PlayerInteractEvent.RightClickBlock event, ServerLevel level,
                              ServerPlayer player, ItemStack held) {
        String id = held.get(ModComponents.LETTER.get());
        if (id == null) {
            // An unmarked letter. The Post can no more copy one than deliver it, and
            // SealedLetterItem already treats a lost component as in-character rather than
            // as an error -- a filing system that has lost which one this is.
            say(player, "interregnum.desk.unmarked", ChatFormatting.GRAY);
            take(event);
            return;
        }
        Desk.Outcome outcome = Desk.lodge(level, event.getPos(), id);
        if (outcome != Desk.Outcome.LODGED) {
            say(player, "interregnum.desk.occupied", ChatFormatting.GRAY);
            take(event);
            return;
        }
        // ONE letter, off the stack. Somebody carrying two of the same letter leaves one.
        held.shrink(1);
        say(player, "interregnum.desk.lodged", ChatFormatting.GRAY);
        take(event);
    }

    private static void collect(PlayerInteractEvent.RightClickBlock event, ServerLevel level,
                                ServerPlayer player) {
        Desk.Outcome state = Desk.state(level, event.getPos());
        if (state == Desk.Outcome.EMPTY) {
            // Nothing of ours here. Left alone entirely, so an empty lectern is an
            // ordinary lectern and a player can put a book on it.
            return;
        }
        if (state == Desk.Outcome.NOT_YET) {
            say(player, "interregnum.desk.working", ChatFormatting.DARK_GRAY);
            take(event);
            return;
        }
        String id = Desk.collect(level, event.getPos());
        if (id == null) {
            return;
        }
        ItemStack back = new ItemStack(ModItems.SEALED_LETTER.get());
        back.set(ModComponents.LETTER.get(), id);
        // Into the hand if there is room, and onto the floor at their feet otherwise.
        // Dropping is deliberate rather than a refusal: four letters exist in a world, and
        // a desk that declined to hand one back because an inventory was full would be a
        // desk holding a piece of the endgame hostage.
        if (!player.getInventory().add(back)) {
            player.drop(back, false);
        }
        say(player, "interregnum.desk.collected", ChatFormatting.GRAY);
        take(event);
    }

    /**
     * Take the interaction, so the lectern does not also do its own thing.
     *
     * `setCancellationResult` before cancelling: without it the client is told the
     * interaction failed and plays the swing animation twice.
     */
    private static void take(PlayerInteractEvent.RightClickBlock event) {
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void say(ServerPlayer player, String key, ChatFormatting style) {
        player.sendSystemMessage(Component.translatable(key).withStyle(style));
    }
}
