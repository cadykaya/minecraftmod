package com.cadykaya.interregnum.system.convert;

import com.cadykaya.interregnum.system.unraveling.UnravelingDefs.ConversionDef;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A set of one-step block conversions, keyed by the block they act on.
 *
 * <h2>Why this is one type and not three</h2>
 *
 * `WORLD.md` locks the first reuse: *"the block-aging registry powering the Turning **is
 * the same system that runs the unraveling.** One mechanism; a school and an
 * apocalypse."* Band 4's attrition is the third caller, and by then the shape had been
 * written out twice identically — a map from block to rule, a refusal to accept two rules
 * claiming the same `from`, and chains formed by one rule's `to` being another's `from`.
 *
 * A third copy would have been three places to fix the same bug, so the shape lives here
 * and the three systems differ only in what they contain. That difference is the whole
 * content:
 *
 * <ul>
 *   <li>the <b>unraveling</b> LOOSENS — intact to loosened to dry, a world nobody is
 *       holding. It carries bands and scopes, because it is an apocalypse on a clock
 *       that arrives in stages and reaches further as chapters pass.</li>
 *   <li>the <b>Turning</b> WEATHERS — a thing acquiring its history, in order, and never
 *       losing it. No bands, no frontier: it is simply what time does here, always, at
 *       the rate it did yesterday.</li>
 *   <li><b>attrition</b> GENERALISES — a thing losing what made it distinct. No bands
 *       either, but gated on ground nobody tends.</li>
 * </ul>
 *
 * <h2>Chains, not jumps</h2>
 *
 * Nothing arrives at its final state. Stone is stone, then cobble, then mossy cobble; a
 * flower is a flower, then grass, then nothing in particular. Each rule's `to` is another
 * rule's `from`, so the world passes through every intermediate state and a player can
 * read how long it has been since anybody was here off the ground itself.
 */
public final class StepTable {

    /** The empty table: nothing converts. What a broken datapack leaves behind. */
    public static final StepTable EMPTY = new StepTable(List.of());

    /** The file as it appears on disk. */
    public record File(List<ConversionDef> conversions) {
        public static final Codec<File> CODEC = RecordCodecBuilder.create(i -> i.group(
                ConversionDef.CODEC.listOf().fieldOf("conversions").forGetter(File::conversions)
        ).apply(i, File::new));
    }

    private final Map<Block, ConversionDef> byFrom;
    /**
     * The reverse index, for reading a table backwards.
     *
     * A block whose value here is null has MORE THAN ONE past: two rules converge on it,
     * so there is no single thing it used to be. The unraveling's table does exactly that
     * — a dandelion and a poppy both become a dead bush — and asking what a dead bush
     * used to be has no answer, only a choice.
     *
     * Null rather than "pick one" is the whole point. A table read backwards that guessed
     * would un-age a dead bush into a poppy half the time and a dandelion the other half,
     * and the god whose entire law is keeping every version would be the one making
     * things up.
     */
    private final Map<Block, ConversionDef> byTo;

    /**
     * @throws IllegalArgumentException if two rules claim the same `from`.
     *
     * Refused rather than resolved, because "which of these two applies" would be
     * answered by file iteration order — and a world whose law depends on the order a
     * datapack happened to be read in is a world with no law.
     */
    public StepTable(List<ConversionDef> conversions) {
        Map<Block, ConversionDef> map = new HashMap<>();
        Map<Block, ConversionDef> reverse = new HashMap<>();
        Set<Block> ambiguous = new HashSet<>();
        for (ConversionDef c : conversions) {
            ConversionDef other = map.put(c.from(), c);
            if (other != null) {
                throw new IllegalArgumentException(
                        "two rules both convert " + c.from() + ": "
                                + other.id() + " and " + c.id());
            }
            // A converging `to` is NOT an error -- the unraveling's table has one on
            // purpose, and forbidding it would forbid a legitimate table for the sake of
            // a reverse read that most callers never do. It is recorded and refused
            // instead, so reading backwards fails loudly at exactly the block where the
            // past is genuinely unknown.
            if (reverse.put(c.to(), c) != null) {
                ambiguous.add(c.to());
            }
        }
        ambiguous.forEach(reverse::remove);
        this.byFrom = Map.copyOf(map);
        this.byTo = Map.copyOf(reverse);
    }

    /** What this block becomes in one step, or null if this table has no opinion. */
    public ConversionDef stepFrom(Block block) {
        return byFrom.get(block);
    }

    /**
     * What this block USED to be, one step back — or null if nothing did, or if more than
     * one thing did.
     *
     * The two nulls are deliberately indistinguishable to callers, because the answer to
     * both is the same: this table cannot tell you. A caller that wanted to say *"a dead
     * bush had two pasts and I will not guess"* differently from *"stone has no past
     * here"* would be making a distinction the table does not have the standing to draw.
     */
    public ConversionDef stepTo(Block block) {
        return byTo.get(block);
    }

    public int ruleCount() {
        return byFrom.size();
    }
}
