package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.Ripen;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/**
 * Casting {@link Ripen}. See that class for what the spell is and where it stops.
 *
 * <h2>The creature is looked for first, and it matters which way round</h2>
 *
 * A calf standing on a wheat field is both a young animal and a position holding a crop.
 * `WORLD.md` names *"crop, sapling, animal"* and the animal is the one a person is pointing
 * at — nobody aims a spell at the ground under a cow and means the wheat. So the entity
 * wins, and the block is what the spell falls back to when there is nothing alive to find.
 */
public final class RipenSpell {
    private RipenSpell() {}

    /** What one cast did. */
    public record Cast(Ripen.Subject subject, String what, int frayed, String refused) {
        static Cast no(String why) {
            return new Cast(Ripen.Subject.NOTHING, "", 0, why);
        }

        /** Whether anything was ripened. A cast that found nothing still cast. */
        public boolean worked() {
            return subject != Ripen.Subject.NOTHING;
        }
    }

    public static Cast cast(ServerLevel level, BlockPos pos, Grimoire grimoire) {
        if (!Casting.permitted(grimoire, Ripen.SCHOOL)) {
            return Cast.no("unlearned");
        }
        Cast done = onCreature(level, pos);
        if (done == null) {
            done = onPlant(level, pos);
        }
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            return done;
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        int frayed = Unraveling.frayAround(level, pos, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
        return new Cast(done.subject(), done.what(), frayed, done.refused());
    }

    /**
     * The nearest young animal within reach, grown.
     *
     * <b>{@code isBaby}, not merely {@code AgeableMob}.</b> An adult has nowhere kind to go
     * — forward from grown is toward the end, which is `Rot`'s country and locked as never
     * aimed at a creature. The spell does not refuse an adult by naming it; it asks for
     * something with growing left to do, and an adult is not that.
     *
     * A player is not an {@link AgeableMob} and so is never a subject at all. That is the
     * shape of the question rather than a rule about players, which is the stronger version:
     * there is no state in this game where a person has growing left to do.
     *
     * @return the cast, or null when there was nothing alive and young to find.
     */
    private static Cast onCreature(ServerLevel level, BlockPos pos) {
        AABB reach = new AABB(pos).inflate(Ripen.REACH);
        List<AgeableMob> young = level.getEntitiesOfClass(AgeableMob.class, reach,
                AgeableMob::isBaby);
        AgeableMob subject = young.stream()
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)))
                .orElse(null);
        if (subject == null) {
            return null;
        }
        // Straight to grown. `setAge(0)` is the moment of being an adult, and the alternative
        // -- ageing by some number of seconds -- would make a cast worth a different amount
        // depending on how newly born the thing was, which is not something a player can see
        // and therefore not something they could learn.
        subject.setAge(0);
        return new Cast(Ripen.Subject.CREATURE,
                BuiltInRegistries.ENTITY_TYPE.getKey(subject.getType()).toString(), 0, "");
    }

    /**
     * The block at the position, moved along.
     *
     * Vanilla's own growth through {@link BlockState#randomTick}, the same mechanism
     * `Verdant.quicken` uses and for the reason that class gives at length: every crop,
     * sapling, vine and mushroom is covered without being named, and so is whatever the next
     * game drop adds. A hand-written list of growable blocks would be wrong the first time
     * Mojang shipped a plant.
     *
     * <b>It does not consult the claim ledger,</b> and that is `LESSONS.md` #35 rather than
     * an oversight: the ledger gates what you did not aim at, and this is one block that
     * somebody pointed at. Wheat you planted is exactly the wheat you meant.
     */
    private static Cast onPlant(ServerLevel level, BlockPos pos) {
        BlockState before = level.getBlockState(pos);
        if (!before.isRandomlyTicking()) {
            return new Cast(Ripen.Subject.NOTHING, "", 0, "");
        }
        // UNTIL IT MOVES, not a fixed number of pushes. Vanilla growth is a dice roll --
        // wheat on unhydrated farmland advances on about one random tick in twenty-six --
        // so a fixed count is a spell that works sometimes, which is not a verb. See
        // `Ripen.ATTEMPTS`: the number bounds the asking, and one cast is worth exactly one
        // step whatever the roll does.
        BlockState after = before;
        for (int n = 0; n < Ripen.ATTEMPTS; n++) {
            BlockState now = level.getBlockState(pos);
            // A thing that has finished growing must stop costing anything.
            if (!now.isRandomlyTicking()) {
                break;
            }
            now.randomTick(level, pos, level.getRandom());
            after = level.getBlockState(pos);
            if (!after.equals(before)) {
                break;
            }
        }
        if (after.equals(before)) {
            // It could have grown and did not. That is a cast that found something and
            // achieved nothing, which is honest to report as nothing rather than as a
            // plant -- the player is looking at an unchanged block either way.
            return new Cast(Ripen.Subject.NOTHING, "", 0, "");
        }
        return new Cast(Ripen.Subject.PLANT,
                BuiltInRegistries.BLOCK.getKey(after.getBlock()).toString(), 0, "");
    }
}
