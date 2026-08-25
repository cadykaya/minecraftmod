package com.cadykaya.interregnum.system.ferry;

import com.cadykaya.interregnum.core.ferry.Law;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The on-disk shape of a crossing law.
 *
 * Blocks are decoded through the block registry rather than kept as free strings, so
 * `minecraft:note_blocK` is a **loud load failure** instead of a rule that silently
 * refuses nothing. That is the same choice the unraveling table makes and for the same
 * reason: a rule nobody can see failing is worse than no rule, and a crossing that
 * quietly admits a note block has broken the one thing it was for.
 */
public final class FerryDefs {
    private FerryDefs() {}

    /** Decoded through the registry so a typo names itself at load. */
    private static final Codec<String> BLOCK_ID = Codec.STRING.comapFlatMap(
            s -> {
                Identifier id = Identifier.tryParse(s);
                if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
                    return DataResult.error(() -> "unknown block: " + s);
                }
                return DataResult.success(id.toString());
            },
            s -> s);

    public record RuleDef(String reasonKey, List<String> blocks) {
        public static final Codec<RuleDef> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("reason_key").forGetter(RuleDef::reasonKey),
                BLOCK_ID.listOf().fieldOf("blocks").forGetter(RuleDef::blocks)
        ).apply(i, RuleDef::new));
    }

    /**
     * Where this crossing goes.
     *
     * A dimension id, decoded to an {@link Identifier} at load so a typo is a loud
     * failure rather than a law that clears you for nowhere. It is deliberately NOT
     * checked against the loaded dimensions here: datapacks load before levels do, and
     * a law naming a dimension another datapack supplies is legitimate. The refusal for
     * a destination that does not exist is at sail time, where it can say so.
     */
    private static final Codec<Identifier> DIMENSION_ID = Codec.STRING.comapFlatMap(
            s -> {
                Identifier id = Identifier.tryParse(s);
                return id == null
                        ? DataResult.error(() -> "not a dimension id: " + s)
                        : DataResult.success(id);
            },
            Identifier::toString);

    public record LawDef(String id, Identifier destination, Map<String, RuleDef> rules) {
        public static final Codec<LawDef> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(LawDef::id),
                DIMENSION_ID.fieldOf("destination").forGetter(LawDef::destination),
                Codec.unboundedMap(Codec.STRING, RuleDef.CODEC).fieldOf("rules")
                        .forGetter(LawDef::rules)
        ).apply(i, LawDef::new));

        /** Law validates on construction, so a bad law throws HERE, at load. */
        public Law toLaw() {
            Map<String, Law.Rule> out = new LinkedHashMap<>();
            rules.forEach((name, r) ->
                    out.put(name, new Law.Rule(Set.copyOf(r.blocks()), r.reasonKey())));
            return new Law(id, out);
        }
    }

    public record LawsFile(List<LawDef> laws) {
        public static final Codec<LawsFile> CODEC = RecordCodecBuilder.create(i -> i.group(
                LawDef.CODEC.listOf().fieldOf("laws").forGetter(LawsFile::laws)
        ).apply(i, LawsFile::new));
    }
}
