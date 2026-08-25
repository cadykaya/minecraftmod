package com.cadykaya.interregnum.system.letters;

import com.cadykaya.interregnum.core.letters.Letter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

/** The on-disk shape of the dead god's outgoing mail. */
public final class LetterDefs {
    private LetterDefs() {}

    public record LetterDef(String id, Optional<String> addressee, String subjectKey,
                            List<String> bodyKeys) {
        public static final Codec<LetterDef> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(LetterDef::id),
                // optionalFieldOf, NOT a default of "": the Quiet One's letter has no
                // addressee, and `To --` is a decision while `To ` is a typo. The core
                // Letter refuses a blank string for exactly that reason, so a default
                // here would turn a caught mistake into an uncaught one.
                Codec.STRING.optionalFieldOf("addressee").forGetter(LetterDef::addressee),
                Codec.STRING.fieldOf("subject_key").forGetter(LetterDef::subjectKey),
                Codec.STRING.listOf().fieldOf("body_keys").forGetter(LetterDef::bodyKeys)
        ).apply(i, LetterDef::new));

        /** Letter validates on construction, so a bad letter throws HERE, at load. */
        public Letter toLetter() {
            return new Letter(id, addressee, subjectKey, bodyKeys);
        }
    }

    public record PostFile(List<LetterDef> letters) {
        public static final Codec<PostFile> CODEC = RecordCodecBuilder.create(i -> i.group(
                LetterDef.CODEC.listOf().fieldOf("letters").forGetter(PostFile::letters)
        ).apply(i, PostFile::new));
    }
}
