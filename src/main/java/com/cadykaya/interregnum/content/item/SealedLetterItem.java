package com.cadykaya.interregnum.content.item;

import com.cadykaya.interregnum.registry.ModComponents;
import com.cadykaya.interregnum.system.letters.LetterPage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * One of the dead god's letters, and the first thing that can open one.
 *
 * The four letters have been written, loaded, validated and checked since early on. The
 * item that carries them was `registerSimpleItem` — no behaviour at all — so **nobody
 * could read one**, and the mid-game's best reveal (three letters open with a name; the
 * fourth opens `To —`) could not happen in play.
 *
 * Unlike casting or attuning, the affordance was never an open question. You read a letter
 * by opening it.
 *
 * <h2>Which letter it is, is on the stack</h2>
 *
 * A single item id carries all four, through {@link ModComponents#LETTER}. A stack with no
 * component is not an error and does not say "broken": it is an unmarked letter, and it
 * says so. `WORLD.md` has the dead god's correspondence *"filed, in the ferry's own
 * desk"*, and a filing system that has lost which one this is is exactly in register.
 *
 * <h2>It does not consume itself</h2>
 *
 * A letter is a document, not a scroll. You can read it again, show it to somebody, and
 * still be carrying it when you get to the god it is for — which the delivery scenes
 * assume, since delivering one is a conversation rather than a hand-over.
 */
public class SealedLetterItem extends Item {
    public SealedLetterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        // Server only: the page is built from the datapack-loaded post, and a client has
        // no post. A client that guessed would render a letter the server disagreed with.
        if (!(player instanceof ServerPlayer reader)) {
            return InteractionResult.SUCCESS;
        }
        String id = held.get(ModComponents.LETTER.get());
        if (id == null) {
            reader.sendSystemMessage(Component.translatable("interregnum.letter.unmarked")
                    .withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
        List<Component> page = LetterPage.of(id);
        if (page == null) {
            // A letter naming something the post does not have. The datapack is wrong,
            // and saying so is more use than pretending the letter is blank.
            reader.sendSystemMessage(Component.translatable("interregnum.letter.missing", id)
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        for (Component line : page) {
            reader.sendSystemMessage(line);
        }
        // AND THE GOD NOTICES. `WORLD.md`: raw god-script -- letters and shrine
        // inscriptions -- read without transcription at the ferry's desk marks the reader.
        // This is the only way to read a letter that exists, so every letter opened is
        // opened raw; the desk is the half of that sentence still unbuilt.
        //
        // Nothing is said about it. The hazard is locked as "no affliction bar, no debuff",
        // and a line announcing that your manifestation rate has risen would be an
        // affliction bar made of text. See RawScript.saidTo.
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            com.cadykaya.interregnum.system.haunt.RawScript.readLetter(
                    server, id, reader.getUUID());
        }
        return InteractionResult.SUCCESS;
    }
}
