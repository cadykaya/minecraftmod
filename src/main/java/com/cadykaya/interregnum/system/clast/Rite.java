package com.cadykaya.interregnum.system.clast;

import com.cadykaya.interregnum.core.chapter.Milestone;
import com.cadykaya.interregnum.core.clast.Attunement;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.RegardSavedData;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * The rite: a clast, a shrine-keeper, and somebody the villages will vouch for.
 *
 * `WORLD.md`, locked: *"a rite at a shrine, and the keeper has to agree to witness it…
 * Standing that was previously a matter of prices and greetings now decides whether you
 * can hold the class at all."*
 *
 * <h2>Everything that decides lives here</h2>
 *
 * A headless server has nobody to hold a clast out to anybody, so the entity's interaction
 * is a few lines over this and `interregnum rite` is the second legitimate caller. Same
 * arrangement as the dream, the spoken word and the ferry's crossing, and for the same
 * reason: a decision only a right-click can reach is a decision nothing can check.
 *
 * <h2>The refusal is a person, not a mechanism</h2>
 *
 * Every other refusal in the mod is a rule: the god is alive, the hull is too big, you
 * have not been taught. This one is a shrine-keeper declining, and it is the only gate in
 * the game a player can open by living differently. It never says *how* — the keeper is
 * not a quest marker, and "get to KNOWN with the villages" is not a sentence anybody in
 * this world would say.
 */
public final class Rite {
    private static final Logger LOG = LogUtils.getLogger();

    private Rite() {}

    /** What happened when the clast was held out. */
    public enum Outcome {
        /** Attuned. There is one more Theoclast, and one fewer clast. */
        ATTUNED,
        /** The keeper will not witness it. Standing, and nothing else. */
        REFUSED,
        /** Already a Theoclast. A clast attunes a person, and this one is attuned. */
        ALREADY,
        /**
         * The god is still alive, so there are no clasts and nothing to attune.
         *
         * Checked even though a player could not be holding one: an operator can, and a
         * rite performed before the shattering would create a class out of nothing.
         */
        NO_GOD
    }

    /**
     * Ask a keeper to witness a rite for this person.
     *
     * The caller consumes the clast if and only if this returns {@link Outcome#ATTUNED} --
     * the item is not touched here, because a rite that ate the clast on a refusal would
     * destroy one of seven objects in the world for saying no.
     */
    public static Outcome offer(MinecraftServer server, UUID who) {
        ChapterSavedData chapter = ChapterSavedData.get(server);
        if (chapter.mechanicsDormant()) {
            return Outcome.NO_GOD;
        }
        Theoclasts theoclasts = Theoclasts.get(server);
        // `peek`, not `of`: asking whether the villages will vouch for somebody must not
        // bring a record into existence for somebody they have never dealt with. An
        // institution's opinion of a stranger is an absence, and `Attunement.judge` reads
        // a null as the bottom of the scale rather than as an exception.
        var verdict = Attunement.judge(
                RegardSavedData.get(server).peek(who), theoclasts.is(who));
        if (verdict != Attunement.Verdict.WITNESSED) {
            return verdict == Attunement.Verdict.ALREADY ? Outcome.ALREADY : Outcome.REFUSED;
        }
        theoclasts.attune(who);
        // FIRST, once, server-wide -- the milestone is about the class existing at all,
        // not about this person. `record` is idempotent, so the second Theoclast changes
        // nothing here and the log line below reports which one this was.
        boolean first = chapter.record(Milestone.FIRST_ATTUNEMENT);
        LOG.info("{} attuned a clast{}. Theoclasts: {}.",
                who, first ? " -- the first Theoclast" : "", theoclasts.count());
        return Outcome.ATTUNED;
    }
}
