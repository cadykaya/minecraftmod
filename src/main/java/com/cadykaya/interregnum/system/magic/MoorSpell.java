package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.Moor;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/**
 * Casting {@link Moor}. See that class for what the spell is and why one rule refuses all
 * three of the forces `WORLD.md` names.
 *
 * Shaped exactly like {@link QuellSpell}, which is the point: the two are the same kind of
 * spell in different schools, and a player who has learned one already knows how the other
 * is aimed.
 */
public final class MoorSpell {
    private MoorSpell() {}

    /** What one cast did. {@code subject} is empty when nothing was moored. */
    public record Cast(boolean took, String subject, int frayed, String refused) {
        static Cast no(String why) {
            return new Cast(false, "", 0, why);
        }
    }

    /**
     * Moor the nearest thing to {@code pos}, if there is one within {@link Moor#REACH}.
     *
     * <b>Any entity, not only a mob</b> — and that is the difference from {@code Quell},
     * which takes a creature's throwing arm and so has only creatures to talk to. The three
     * forces this refuses push boats, items and falling blocks as readily as they push
     * anything alive, and the most Anchorite use of the spell is on a falling block in a
     * world where those rise and do not stop.
     *
     * <b>It costs the corpse even when it finds nothing</b>, for the reason `QuellSpell`
     * gives: the fraying is what the cast spent rather than what it achieved.
     */
    public static Cast cast(ServerLevel level, BlockPos pos, Grimoire grimoire) {
        if (!Casting.permitted(grimoire, Moor.SCHOOL)) {
            return Cast.no("unlearned");
        }
        AABB reach = new AABB(pos).inflate(Moor.REACH);
        List<Entity> found = level.getEntities((Entity) null, reach, e -> true);
        Entity subject = found.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)))
                .orElse(null);
        if (subject == null) {
            return Cast.no("nothing there");
        }
        // The exact position, not the block. A thing snapped to the middle of the block it
        // happened to be in would visibly JUMP on being moored, which is the wrong feel for
        // a spell whose whole claim is that nothing moved.
        Moored.moor(level, subject.getUUID(), subject.position());
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(subject.getType()).toString();
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            return new Cast(true, name, 0, "");
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        int frayed = Unraveling.frayAround(level, pos, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
        return new Cast(true, name, frayed, "");
    }
}
