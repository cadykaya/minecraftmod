package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.Incantation;
import com.cadykaya.interregnum.core.magic.Spell;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Hearing a word and casting the spell it names.
 *
 * `WORLD.md`, locked: casting is a **spoken word**, said in chat, in front of whoever is
 * present. This is the whole seam between "somebody said something" and the ten spells,
 * and it is deliberately the only one.
 *
 * <h2>The spells do not know how they were triggered, and must not learn</h2>
 *
 * Every {@code *Spell.cast(...)} in this package took a level, a position and a grimoire
 * before this class existed and takes exactly the same arguments now. Nothing below the
 * seam was changed to make speech work, and nothing below it would change if the
 * affordance became a focus item tomorrow.
 *
 * That is not tidiness. `WORLD.md` marks the affordance **[NEEDS PLAYTEST]** in as many
 * words — typing a word to cast may simply feel bad, and nobody can find that out in a
 * container with no client. The cost of being wrong has to stay one file.
 *
 * <h2>Nothing is cancelled: the word still goes to chat</h2>
 *
 * The obvious implementation swallows the message so the spell word does not clutter the
 * channel, and it destroys the feature. The locked reason casting is speech at all is that
 * the offence is **audible**: a Warden in the room has witnessed it, a bystander can
 * repeat what you said, and casting quietly in a cellar becomes a real choice. A word
 * nobody else sees is a keybind with extra steps.
 *
 * <h2>Every decision is here, where a command can reach it</h2>
 *
 * A headless server has nobody to type in chat, so {@link SpeechEvents} is four lines over
 * this and `interregnum speak` is the second legitimate caller — the same arrangement as
 * {@link com.cadykaya.interregnum.system.dialogue.TheHaunt} and for the same reason.
 */
public final class Speech {
    private static final Logger LOG = LogUtils.getLogger();

    private Speech() {}

    /** What happened when the word was said. */
    public enum Outcome {
        /** The spell was cast. Whether it *did* anything is the spell's own business. */
        CAST,
        /**
         * Not a spell word. Somebody said something.
         *
         * The overwhelmingly common case, and it must stay silent: chat is where players
         * talk, and a magic system that answered back every time it did not recognise a
         * sentence would be unusable within a minute.
         */
        NOT_A_WORD,
        /**
         * A real word, said by somebody who has not been taught that school.
         *
         * They still said it out loud, and everybody still heard them say it. That is the
         * joke and it is also correct: knowing the word is not knowing the spell.
         */
        UNLEARNED,
        /**
         * They are holding their breath, and a spell is a word.
         *
         * `WORLD.md`, on what falls out of casting being *a word you are on record as
         * having said*: *"you cannot cast silently — which is what makes Held-breath
         * interesting rather than a stealth trinket: while you hold it you have no voice,
         * so you have no spells."*
         *
         * The refusal lives HERE, at the mouth, and not in each spell. That is the whole
         * claim: it is not that the spells stop working, it is that nothing was said.
         * Which is also why a breath cannot be put down early — ending it would take a
         * word, and the word is the thing that has been taken.
         */
        NO_VOICE
    }

    /** What one spoken word did. {@code detail} is the spell's own reply, for the log. */
    public record Heard(Outcome outcome, Spell spell, String detail) {
        static Heard no(Outcome why) {
            return new Heard(why, null, "");
        }
    }

    /**
     * Somebody at {@code from}, looking at {@code toward}, said {@code word}.
     *
     * @param toward what the caster is looking at, or {@code from} when they are looking
     *               at nothing. A spell that reaches for a target and finds the caster's
     *               own feet is a spell cast on the ground in front of you, which is what
     *               casting into the sky should do.
     */
    public static Heard speak(ServerLevel level, BlockPos from, BlockPos toward,
                              UUID who, String word, Grimoire grimoire) {
        Spell spell = Incantation.of(word);
        if (spell == null) {
            return Heard.no(Outcome.NOT_A_WORD);
        }
        if (!Casting.permitted(grimoire, spell.school())) {
            return Heard.no(Outcome.UNLEARNED);
        }
        // BEFORE THE SPELL IS DISPATCHED AND AFTER THE WORD IS RECOGNISED, and the order
        // is the fiction. They said a real word they know; they simply had no voice to say
        // it with. Checking earlier would make an unlearned word and a held breath
        // indistinguishable, and checking later would mean the spell happened and was
        // then undone.
        if (Holding.holds(level, who)) {
            return new Heard(Outcome.NO_VOICE, spell, "");
        }
        BlockPos at = Incantation.aimed(spell) ? toward : from;
        String detail = dispatch(level, from, at, who, spell, grimoire);
        LOG.info("{} said \"{}\" at {}: {}", who, word, at, detail);
        return new Heard(Outcome.CAST, spell, detail);
    }

    /**
     * The one place that knows which class casts which spell.
     *
     * A switch rather than a registry, and deliberately: ten cases the compiler will
     * complain about if a spell is added, versus a map that silently has no entry for the
     * eleventh. `Spell` is an enum and this switch is exhaustive over it, so a new verb
     * cannot be speakable and undispatched at the same time.
     */
    private static String dispatch(ServerLevel level, BlockPos from, BlockPos at, UUID who,
                                   Spell spell, Grimoire g) {
        return switch (spell) {
            case WEATHER -> String.valueOf(Weather.cast(level, at, g).became());
            case REWIND -> RewindSpell.describe(RewindSpell.cast(level, at, g));
            case LIGHTEN -> String.valueOf(LightenSpell.cast(level, at, g).opened());
            case DROP_FORGE -> String.valueOf(DropForgeSpell.cast(level, at, g).opened());
            // The only spell that needs to know where the caster is standing as well as
            // what they are looking at: it grows a span BETWEEN the two.
            case BRIDGEROOT -> String.valueOf(BridgerootSpell.cast(level, from, at, g).grew());
            case WILDGROWTH -> String.valueOf(WildgrowthSpell.cast(level, at, g).grew());
            case HUSH -> String.valueOf(HushSpell.cast(level, at, g).opened());
            case STILL -> String.valueOf(StillSpell.cast(level, at, g).opened());
            case QUELL -> QuellSpell.cast(level, at, g).subject();
            // Loft is two verbs and one word. You say it, and the building goes up or
            // comes down -- which is what a person carrying something would mean by
            // saying it twice, and it spares the kit a second incantation for a hand
            // that is either full or empty and never ambiguous.
            case LOFT -> Lofted.get(level.getServer()).held(who) == null
                    ? "lift " + LoftSpell.lift(level, at, who, g).refusal()
                    : "place " + LoftSpell.place(level, at, who, g).refusal();
            // The subject is always the speaker. `WORLD.md`: "your own sound" -- a version
            // that could be aimed would be a silence you inflict, which is a different
            // spell and a much worse one.
            case HELD_BREATH -> String.valueOf(
                    HeldBreathSpell.cast(level, from, who, g).held());
            case MOOR -> MoorSpell.cast(level, at, g).subject();
        };
    }
}
