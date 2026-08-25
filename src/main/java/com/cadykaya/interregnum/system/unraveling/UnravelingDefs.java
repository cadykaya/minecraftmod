package com.cadykaya.interregnum.system.unraveling;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import com.cadykaya.interregnum.core.chapter.Chapter;

import java.util.List;
import java.util.Locale;

/**
 * The on-disk shape of {@code data/&lt;namespace&gt;/unraveling/bands.json}.
 *
 * Blocks are decoded through the block registry rather than kept as strings, so a
 * typo'd id is a loud load failure instead of a rule that silently never matches.
 * A conversion that never fires is the worst possible bug here: the world simply
 * looks fine, and nobody finds out for a hundred hours.
 */
public final class UnravelingDefs {
    private UnravelingDefs() {}

    /** Where a band applies. */
    public enum Scope {
        /** Near the crater and the shrines only. The world has not understood yet. */
        THIN_PLACES("thin_places"),
        /** Everywhere in the overworld. */
        OVERWORLD("overworld");

        public final String id;

        Scope(String id) {
            this.id = id;
        }

        public static final Codec<Scope> CODEC = Codec.STRING.comapFlatMap(
                s -> {
                    for (Scope v : values()) {
                        if (v.id.equals(s)) {
                            return DataResult.success(v);
                        }
                    }
                    return DataResult.error(() -> "unknown unraveling scope: " + s);
                },
                v -> v.id);
    }

    /** One rule: this block becomes that block, this often. */
    public record ConversionDef(String id, Block from, Block to, float chance) {
        public static final Codec<ConversionDef> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(ConversionDef::id),
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("from").forGetter(ConversionDef::from),
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("to").forGetter(ConversionDef::to),
                Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(ConversionDef::chance)
        ).apply(i, ConversionDef::new));
    }

    /**
     * One band. `chapter` is decoded and cross-checked against `band` rather than
     * ignored as a comment: the two are the same fact written twice, and a data
     * file that says band 2 / VIGIL means somebody has renumbered the chapters and
     * not finished the job.
     */
    public record BandDef(int band, Chapter chapter, Scope scope, List<ConversionDef> conversions) {
        private static final Codec<Chapter> CHAPTER_CODEC = Codec.STRING.comapFlatMap(
                s -> {
                    try {
                        return DataResult.success(Chapter.valueOf(s.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException e) {
                        return DataResult.error(() -> "unknown chapter: " + s);
                    }
                },
                c -> c.name());

        public static final Codec<BandDef> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, Chapter.SUCCESSION.band).fieldOf("band").forGetter(BandDef::band),
                CHAPTER_CODEC.fieldOf("chapter").forGetter(BandDef::chapter),
                Scope.CODEC.fieldOf("scope").forGetter(BandDef::scope),
                ConversionDef.CODEC.listOf().fieldOf("conversions").forGetter(BandDef::conversions)
        ).apply(i, BandDef::new));
    }

    /** A whole file. `_comment` and `note` are for the author and are not read. */
    public record TableDef(List<BandDef> bands) {
        public static final Codec<TableDef> CODEC = RecordCodecBuilder.create(i -> i.group(
                BandDef.CODEC.listOf().fieldOf("bands").forGetter(TableDef::bands)
        ).apply(i, TableDef::new));
    }
}
