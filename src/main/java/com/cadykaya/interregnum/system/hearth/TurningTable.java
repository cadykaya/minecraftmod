package com.cadykaya.interregnum.system.hearth;

import com.cadykaya.interregnum.system.unraveling.UnravelingDefs.ConversionDef;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the Hearth-Turner's world does to a block that has been standing a while.
 *
 * <h2>One mechanism, two uses -- and it is locked that way</h2>
 *
 * `WORLD.md`: *"the block-aging registry powering the Turning **is the same system that
 * runs the unraveling.** One mechanism; a school and an apocalypse."* So this reuses
 * {@link ConversionDef} exactly -- same record, same codec, same JSON shape as the
 * unraveling's bands. Not a copy of it: the same type, imported.
 *
 * <h2>What is NOT shared, and why that is the point</h2>
 *
 * The unraveling's rules carry a **band** and a **scope**, because it is an apocalypse
 * on a clock that arrives in stages and reaches further as chapters pass. Ageing has
 * neither. It is not escalating and it has no frontier; it is simply what time does
 * here, everywhere, always, at the same rate it did yesterday.
 *
 * That difference is the two gods in one sentence. The overworld is coming apart because
 * nobody is holding it. This world is not coming apart at all — it is *accumulating*,
 * and refusing to let any of it go.
 *
 * <h2>Chains, not jumps</h2>
 *
 * Stone does not arrive mossy. It is stone, then cobble, then mossy cobble, because each
 * rule's `to` is another rule's `from`. A player walking back through a place they built
 * can read how long ago they were there off the walls. That is what *keeping every past*
 * means when it is a block rather than a grievance.
 */
public final class TurningTable {

    /** The empty table: nothing ages. What a broken datapack leaves behind. */
    public static final TurningTable EMPTY = new TurningTable(List.of());

    /** The file as it appears on disk. */
    public record File(List<ConversionDef> conversions) {
        public static final Codec<File> CODEC = RecordCodecBuilder.create(i -> i.group(
                ConversionDef.CODEC.listOf().fieldOf("conversions").forGetter(File::conversions)
        ).apply(i, File::new));
    }

    private final Map<Block, ConversionDef> byFrom;

    /**
     * @throws IllegalArgumentException if two rules claim the same `from`.
     *
     * Refused rather than resolved, because "which of these two applies" would be
     * answered by file iteration order — and a world whose law depends on the order
     * a datapack happened to be read in is a world with no law.
     */
    public TurningTable(List<ConversionDef> conversions) {
        Map<Block, ConversionDef> map = new HashMap<>();
        for (ConversionDef c : conversions) {
            ConversionDef other = map.put(c.from(), c);
            if (other != null) {
                throw new IllegalArgumentException(
                        "two ageing rules both age " + c.from() + ": "
                                + other.id() + " and " + c.id());
            }
        }
        this.byFrom = Map.copyOf(map);
    }

    /** What this block becomes with age, or null if age does nothing to it. */
    public ConversionDef ageOf(Block block) {
        return byFrom.get(block);
    }

    public int ruleCount() {
        return byFrom.size();
    }
}
