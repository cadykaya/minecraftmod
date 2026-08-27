package com.cadykaya.interregnum.data;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.registry.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

/**
 * The one advancement, and the one rule it has to keep.
 *
 * `WORLD.md` locks two things that meet here. The advancement at the moment of death
 * is called **Deicide**. And: *the mod never announces who did it. There is simply a
 * player online who has gone quiet.*
 *
 * <h2>Those two are in direct conflict by default</h2>
 *
 * Minecraft broadcasts advancements to chat. Shipping this with the default flag
 * would put **"Cady has made the advancement [Deicide]"** in front of everybody on
 * the server at the exact moment the design says nobody is told. It would be the
 * single loudest possible violation of the mod's central beat, delivered by a
 * boolean nobody looked at.
 *
 * So {@code announceChat} is false and {@code hidden} is true: the killer gets a
 * toast, alone, and the advancement tree does not display it to anyone who has not
 * earned it. The whole feature is that one flag.
 *
 * <h2>Why the criterion is impossible</h2>
 *
 * There is no vanilla trigger for "you took a god's heart out of a box". The
 * criterion is {@code minecraft:impossible} and {@link com.cadykaya.interregnum.system.Deicide}
 * awards it directly, which keeps the condition in the same place as the rest of the
 * deicide's consequences rather than split between Java and a JSON predicate.
 */
public final class ModAdvancements implements AdvancementSubProvider {

    public static final Identifier DEICIDE =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "deicide");

    /** The criterion name the code awards. Shared so the two cannot drift apart. */
    public static final String CRITERION = "took_the_heart";

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> output) {
        Advancement.Builder.advancement()
                .display(
                        ModItems.GOD_HEART.get(),
                        Component.translatable("advancements.interregnum.deicide.title"),
                        Component.translatable("advancements.interregnum.deicide.description"),
                        null,                    // no background: this is not a tab root
                        AdvancementType.CHALLENGE,
                        true,                    // a toast, for the killer alone
                        false,                   // NEVER to chat -- see the class comment
                        true)                    // hidden until earned
                .addCriterion(CRITERION, new Criterion<>(
                        CriteriaTriggers.IMPOSSIBLE,
                        new ImpossibleTrigger.TriggerInstance()))
                .save(output, DEICIDE.toString());
    }
}
