package com.cadykaya.interregnum.system.dialogue;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import com.cadykaya.interregnum.core.dialogue.DialogueOption;
import com.cadykaya.interregnum.core.regard.RegardState;
import com.cadykaya.interregnum.core.dialogue.Resolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A conversation, as chat.
 *
 * This is the whole interface for now, and it is not a placeholder for the screen so
 * much as a decision about what to build first. Rendering to chat needs **no client
 * code at all** -- clickable text is vanilla, so a player on an unmodified client can
 * play every scene in the mod. That means the dialogue system is finishable and
 * testable end to end in an environment with no game client in it, and the eventual
 * screen becomes an upgrade to something already working rather than the thing
 * standing between the writing and anyone reading it.
 *
 * Everything here is presentation. No method on this class decides anything: the
 * table's state comes from {@link Conversations} and every click routes back through
 * the same `submit` the tested path uses.
 *
 * Text is always a translation key, never English, exactly like every other
 * player-facing string in the mod.
 */
public final class ConversationView {
    private ConversationView() {}

    private static final Style SPEAKER = Style.EMPTY.withColor(ChatFormatting.AQUA);
    private static final Style LINE = Style.EMPTY.withColor(ChatFormatting.WHITE);
    private static final Style OPTION = Style.EMPTY.withColor(ChatFormatting.YELLOW);
    private static final Style GATED = Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE);
    private static final Style QUIET = Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);
    private static final Style RULE = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);

    /**
     * What this participant should see right now.
     *
     * @param tags the viewer's tags, which decide whether gated options appear at
     *             all. A player who cannot pick an option is not shown it -- an
     *             option greyed out with a reason is a different design (it teaches
     *             the player what they are missing) and this mod deliberately does
     *             not do that: finding out what you could have said is the point.
     */
    public static List<Component> render(Conversations.Table table, String viewer, Set<String> tags) {
        return render(table, viewer, tags, null);
    }

    /**
     * The same view, with the viewer's standing consulted.
     *
     * Passing the regard is what makes `standing_at_least` mean anything: without it
     * a gate is data that loads, validates, and is never read, which is the shape of
     * a feature that is finished everywhere except where it matters. A null record --
     * a player with no history, or a caller that genuinely has none -- reads as
     * default standing rather than as "hide everything".
     */
    public static List<Component> render(Conversations.Table table, String viewer,
                                         Set<String> tags, RegardState regard) {
        List<Component> out = new ArrayList<>();
        var node = table.node();
        out.add(Component.literal("- - - - - - - - - - - - - - - -").withStyle(RULE));
        out.add(Component.empty()
                .append(Component.translatable("interregnum.speaker." + node.speaker()).withStyle(SPEAKER))
                .append(Component.literal("  ").withStyle(LINE))
                .append(Component.translatable(node.textKey()).withStyle(LINE)));

        for (DialogueOption option : node.options()) {
            if (!option.visibleTo(tags, regard)) {
                continue;
            }
            boolean gated = !option.requiredTags().isEmpty() || !option.standing().isOpen();
            MutableComponent line = Component.empty()
                    .append(Component.literal("  > ").withStyle(RULE))
                    .append(Component.translatable(option.textKey())
                            .withStyle((gated ? GATED : OPTION)
                                    .withClickEvent(new ClickEvent.RunCommand(
                                            "/interregnum reply " + option.id()))
                                    .withHoverEvent(new HoverEvent.ShowText(
                                            Component.translatable("interregnum.dlg.say")))));
            out.add(line);
        }

        // What the table is waiting on -- FROM THIS VIEWER'S SEAT, which is the only
        // useful framing. In a conversation several people are in at once, the
        // difference between "the game is broken" and "Cady has not picked yet" is
        // the whole difference between waiting patiently and reloading.
        //
        // Counting the viewer among the people being waited for was the first
        // version, and it read as "waiting on 2 more" to one of the two people it
        // was waiting for.
        if (table.conversation.ended()) {
            // Nothing is outstanding and nothing can be re-picked. Telling a player
            // the finished conversation is "waiting on 1 other" would be the worst
            // possible last impression of the scene.
            return out;
        }
        Map<String, String> picks = table.conversation.picks();
        int othersOutstanding = 0;
        for (String p : table.conversation.participants()) {
            if (!p.equals(viewer) && !picks.containsKey(p)) {
                othersOutstanding++;
            }
        }
        String mine = picks.get(viewer);
        if (mine != null) {
            // Repicking before the node resolves is legal, so this is a reminder
            // rather than a receipt: the options above are still live.
            out.add(Component.translatable("interregnum.dlg.you_said",
                    Component.translatable(optionText(table, mine))).withStyle(QUIET));
        }
        if (othersOutstanding > 0) {
            out.add(Component.translatable("interregnum.dlg.waiting", othersOutstanding)
                    .withStyle(QUIET));
        }
        return out;
    }

    /** The translation key of an option on the current node, or the raw id. */
    private static String optionText(Conversations.Table table, String optionId) {
        for (DialogueOption o : table.node().options()) {
            if (o.id().equals(optionId)) {
                return o.textKey();
            }
        }
        return optionId;
    }

    /**
     * What everyone said, once a node resolves.
     *
     * This is the entire "players argue with each other" mechanic as far as the
     * interface is concerned: every stance is shown to the whole table, including
     * the ones that lost. Dissent that nobody sees is not dissent.
     */
    public static List<Component> stances(Resolution resolution) {
        List<Component> out = new ArrayList<>();
        for (var entry : resolution.stances().entrySet()) {
            out.add(Component.empty()
                    .append(Component.literal("  " + entry.getKey() + ": ").withStyle(QUIET))
                    .append(Component.literal(entry.getValue()).withStyle(QUIET)));
        }
        if (resolution.kind() == Resolution.Kind.REPROMPT) {
            out.add(Component.translatable("interregnum.dlg.reprompt").withStyle(QUIET));
        }
        return out;
    }

    /**
     * Plain text of a rendered view, for logs and for `/interregnum talk show`.
     *
     * Split on embedded newlines, because a scene's lines use them as a beat --
     * "Yes.\n\nThe quarter still closes." is two breaths, and chat renders it that
     * way. Without the split, only the first breath carries whatever prefix the
     * caller is putting on each line, and the rest of the sentence falls out of
     * every log and every grep looking for it.
     */
    public static List<String> plain(List<Component> lines) {
        List<String> out = new ArrayList<>();
        for (Component c : lines) {
            for (String piece : c.getString().split("\n", -1)) {
                out.add(piece);
            }
        }
        return out;
    }
}
