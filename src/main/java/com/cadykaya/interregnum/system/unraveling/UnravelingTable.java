package com.cadykaya.interregnum.system.unraveling;

import net.minecraft.world.level.block.Block;

import com.cadykaya.interregnum.system.unraveling.UnravelingDefs.BandDef;
import com.cadykaya.interregnum.system.unraveling.UnravelingDefs.ConversionDef;
import com.cadykaya.interregnum.system.unraveling.UnravelingDefs.Scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The loaded bands, indexed the way the runtime asks about them.
 *
 * Validated at construction and never afterwards, exactly like
 * {@link com.cadykaya.interregnum.core.dialogue.DialogueGraph}: an invalid table
 * cannot exist, so no caller has to wonder.
 *
 * The same rules are checked by `tools/unraveling_check.py`, and that is not
 * duplication -- the tool only ever sees the file in THIS repository, while a
 * datapack can add or replace bands on a server nobody here will ever run. The
 * checker protects the authors; this protects the players.
 */
public final class UnravelingTable {
    /** The empty table: what the runtime holds before the first datapack load. */
    public static final UnravelingTable EMPTY = new UnravelingTable(List.of());

    /** One conversion, with the band conditions that gate it. */
    public record Rule(int band, Scope scope, ConversionDef conversion) {
        public String id() {
            return conversion.id();
        }
    }

    private final Map<Block, List<Rule>> bySource;
    private final int ruleCount;
    private final int bandCount;

    public UnravelingTable(List<BandDef> bands) {
        Map<Block, List<Rule>> index = new HashMap<>();
        Set<Integer> seenBands = new HashSet<>();
        Set<String> seenIds = new HashSet<>();
        Map<Block, Block> arrows = new HashMap<>();
        int rules = 0;

        for (BandDef band : bands) {
            if (!seenBands.add(band.band())) {
                throw new IllegalArgumentException("duplicate band " + band.band());
            }
            if (band.band() != band.chapter().band) {
                throw new IllegalArgumentException(
                        "band " + band.band() + " is labelled " + band.chapter()
                                + ", which is band " + band.chapter().band);
            }
            for (ConversionDef c : band.conversions()) {
                if (!seenIds.add(c.id())) {
                    throw new IllegalArgumentException("duplicate conversion id " + c.id());
                }
                if (c.chance() <= 0.0F) {
                    throw new IllegalArgumentException(c.id() + ": chance must be above zero");
                }
                if (c.from() == c.to()) {
                    throw new IllegalArgumentException(c.id() + ": converts a block to itself");
                }
                // The unraveling runs one way. If anything anywhere turns A into B,
                // nothing may turn B back into A -- two such rules would sit in the
                // same world flickering a block between them forever, which is not
                // an apocalypse, it is a light switch.
                if (arrows.get(c.to()) == c.from()) {
                    throw new IllegalArgumentException(
                            c.id() + ": reverses an existing conversion; the unraveling never runs backwards");
                }
                arrows.put(c.from(), c.to());
                index.computeIfAbsent(c.from(), k -> new ArrayList<>())
                        .add(new Rule(band.band(), band.scope(), c));
                rules++;
            }
        }

        this.bySource = Map.copyOf(index);
        this.ruleCount = rules;
        this.bandCount = bands.size();
    }

    /**
     * Every rule that could ever apply to this block, at any band.
     *
     * Keyed on the block rather than scanned, because this is asked once per
     * sampled position and the overwhelmingly common answer is "none".
     */
    public List<Rule> rulesFor(Block block) {
        return bySource.getOrDefault(block, List.of());
    }

    public int ruleCount() {
        return ruleCount;
    }

    public int bandCount() {
        return bandCount;
    }
}
