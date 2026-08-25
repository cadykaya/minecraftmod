package com.cadykaya.interregnum.content.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

import com.cadykaya.interregnum.core.dialogue.DialogueGraph;
import com.cadykaya.interregnum.core.dialogue.DialogueNode;
import com.cadykaya.interregnum.core.dialogue.DialogueOption;
import com.cadykaya.interregnum.core.dialogue.ResolutionRule;
import com.cadykaya.interregnum.core.dialogue.StandingGate;
import com.cadykaya.interregnum.core.dialogue.TextVariant;
import com.cadykaya.interregnum.core.regard.Institution;
import com.cadykaya.interregnum.core.regard.Standing;

/**
 * The on-disk shape of a dialogue file, and the adapter from it to the engine.
 *
 * The Codecs live HERE, in the game module, and not in core: core is
 * loader-independent by design (docs/ARCHITECTURE.md) and must not gain a
 * dependency on Mojang's serialization just to be loadable. This class is the
 * seam, and it is the only place that knows both shapes.
 */
public final class DialogueDefs {
    private DialogueDefs() {}

    /**
     * Institutions are decoded through a codec that names the bad value, because the
     * realistic failure here is a writer typing VILLAGE for VILLAGES at two in the
     * morning -- and a silently dropped effect is a choice that reads as consequential
     * and is not.
     */
    private static final Codec<Institution> INSTITUTION = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    return com.mojang.serialization.DataResult.success(
                            Institution.valueOf(s.toUpperCase(java.util.Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return com.mojang.serialization.DataResult.error(
                            () -> "unknown institution: " + s);
                }
            },
            Institution::name);

    /**
     * Bands decode the same way and for the same reason. A misspelt band is worse
     * than a misspelt institution: an option gated on TRUSTD would not fail, it would
     * be silently ungated, and content nobody was supposed to see yet is the one bug
     * a playtester cannot report because it looks like the game working.
     */
    private static final Codec<Standing> STANDING = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    return com.mojang.serialization.DataResult.success(
                            Standing.valueOf(s.toUpperCase(java.util.Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return com.mojang.serialization.DataResult.error(
                            () -> "unknown standing: " + s);
                }
            },
            Standing::name);

    private static final Codec<Map<Institution, Standing>> STANDINGS =
            Codec.unboundedMap(INSTITUTION, STANDING);

    public record OptionDef(String id, String textKey, String target, List<String> requiredTags,
                            Map<Institution, Integer> regard,
                            Map<Institution, Standing> atLeast,
                            Map<Institution, Standing> atMost) {
        public static final Codec<OptionDef> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(OptionDef::id),
                Codec.STRING.fieldOf("text_key").forGetter(OptionDef::textKey),
                Codec.STRING.fieldOf("target").forGetter(OptionDef::target),
                Codec.STRING.listOf().optionalFieldOf("required_tags", List.of())
                        .forGetter(OptionDef::requiredTags),
                Codec.unboundedMap(INSTITUTION, Codec.intRange(-100, 100))
                        .optionalFieldOf("regard", Map.of()).forGetter(OptionDef::regard),
                // "at_least" / "at_most" rather than "min"/"max": these are bands, and
                // a writer reading `"at_most": {"VILLAGES": "WARY"}` gets the meaning
                // without having to remember which end of the enum is larger.
                STANDINGS.optionalFieldOf("standing_at_least", Map.of())
                        .forGetter(OptionDef::atLeast),
                STANDINGS.optionalFieldOf("standing_at_most", Map.of())
                        .forGetter(OptionDef::atMost)
        ).apply(i, OptionDef::new));

        DialogueOption toOption() {
            return new DialogueOption(id, textKey, target, requiredTags, regard,
                    new StandingGate(atLeast, atMost));
        }
    }

    /** One alternative wording of a node's line, for a player with a given file. */
    public record VariantDef(String textKey, Map<Institution, Standing> atLeast,
                             Map<Institution, Standing> atMost) {
        public static final Codec<VariantDef> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("text_key").forGetter(VariantDef::textKey),
                STANDINGS.optionalFieldOf("standing_at_least", Map.of())
                        .forGetter(VariantDef::atLeast),
                STANDINGS.optionalFieldOf("standing_at_most", Map.of())
                        .forGetter(VariantDef::atMost)
        ).apply(i, VariantDef::new));

        TextVariant toVariant() {
            return new TextVariant(textKey, new StandingGate(atLeast, atMost));
        }
    }

    public record NodeDef(String id, String speaker, String textKey, String rule,
                          List<OptionDef> options, List<VariantDef> textVariants) {
        public static final Codec<NodeDef> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(NodeDef::id),
                Codec.STRING.fieldOf("speaker").forGetter(NodeDef::speaker),
                Codec.STRING.fieldOf("text_key").forGetter(NodeDef::textKey),
                Codec.STRING.fieldOf("rule").forGetter(NodeDef::rule),
                OptionDef.CODEC.listOf().optionalFieldOf("options", List.of())
                        .forGetter(NodeDef::options),
                VariantDef.CODEC.listOf().optionalFieldOf("text_variants", List.of())
                        .forGetter(NodeDef::textVariants)
        ).apply(i, NodeDef::new));

        DialogueNode toNode() {
            return new DialogueNode(id, speaker, textKey,
                    ResolutionRule.valueOf(rule),
                    options.stream().map(OptionDef::toOption).toList(),
                    textVariants.stream().map(VariantDef::toVariant).toList());
        }
    }

    public record GraphDef(String id, String start, List<NodeDef> nodes) {
        public static final Codec<GraphDef> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(GraphDef::id),
                Codec.STRING.fieldOf("start").forGetter(GraphDef::start),
                NodeDef.CODEC.listOf().fieldOf("nodes").forGetter(GraphDef::nodes)
        ).apply(i, GraphDef::new));

        /**
         * Build the engine graph. DialogueGraph validates on construction, so a
         * dangling target or an unreachable node throws HERE -- at datapack load,
         * named, with the pack reload reporting it -- rather than mid-conversation
         * in front of a player. That is the entire reason validation lives in the
         * constructor instead of in a check somebody might forget to call.
         */
        public DialogueGraph toGraph() {
            return new DialogueGraph(start, nodes.stream().map(NodeDef::toNode).toList());
        }
    }
}
