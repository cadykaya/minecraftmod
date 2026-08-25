package com.cadykaya.interregnum.content.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

import com.cadykaya.interregnum.core.dialogue.DialogueGraph;
import com.cadykaya.interregnum.core.dialogue.DialogueNode;
import com.cadykaya.interregnum.core.dialogue.DialogueOption;
import com.cadykaya.interregnum.core.dialogue.ResolutionRule;

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

    public record OptionDef(String id, String textKey, String target, List<String> requiredTags) {
        public static final Codec<OptionDef> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(OptionDef::id),
                Codec.STRING.fieldOf("text_key").forGetter(OptionDef::textKey),
                Codec.STRING.fieldOf("target").forGetter(OptionDef::target),
                Codec.STRING.listOf().optionalFieldOf("required_tags", List.of())
                        .forGetter(OptionDef::requiredTags)
        ).apply(i, OptionDef::new));

        DialogueOption toOption() {
            return new DialogueOption(id, textKey, target, requiredTags);
        }
    }

    public record NodeDef(String id, String speaker, String textKey, String rule,
                          List<OptionDef> options) {
        public static final Codec<NodeDef> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(NodeDef::id),
                Codec.STRING.fieldOf("speaker").forGetter(NodeDef::speaker),
                Codec.STRING.fieldOf("text_key").forGetter(NodeDef::textKey),
                Codec.STRING.fieldOf("rule").forGetter(NodeDef::rule),
                OptionDef.CODEC.listOf().optionalFieldOf("options", List.of())
                        .forGetter(NodeDef::options)
        ).apply(i, NodeDef::new));

        DialogueNode toNode() {
            return new DialogueNode(id, speaker, textKey,
                    ResolutionRule.valueOf(rule),
                    options.stream().map(OptionDef::toOption).toList());
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
