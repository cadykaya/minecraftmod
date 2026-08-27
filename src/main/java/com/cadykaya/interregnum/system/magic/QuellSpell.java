package com.cadykaya.interregnum.system.magic;

import com.cadykaya.interregnum.core.magic.Casting;
import com.cadykaya.interregnum.core.magic.Grimoire;
import com.cadykaya.interregnum.core.magic.Quell;
import com.cadykaya.interregnum.system.ChapterSavedData;
import com.cadykaya.interregnum.system.unraveling.Unraveling;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/** Casting {@link Quell}. See that class for what the spell is and what it does not claim. */
public final class QuellSpell {
    private QuellSpell() {}

    /** What one cast did. {@code subject} is empty when nothing was quelled. */
    public record Cast(boolean took, String subject, int frayed, String refused) {
        static Cast no(String why) {
            return new Cast(false, "", 0, why);
        }
    }

    /**
     * Quell the nearest mob to {@code pos}, if there is one within {@link Quell#REACH}.
     *
     * <b>Nearest, not all of them.</b> "Strip one ability" is singular twice over in the
     * locked text -- one ability, and from the reading of the example, one creature. A
     * version that quelled everything in the radius would be a zone with extra steps, and
     * the mod already has two of those in this school.
     *
     * <b>It costs the corpse even when it finds nothing.</b> The fraying is what the cast
     * spent, not what it achieved, and a spell that were free when it missed would teach a
     * player to spam it -- which is exactly the reading of the ban that `WORLD.md` rules
     * out. Missing is refused only for the two things that are not casts at all: not
     * knowing the school, and there being nothing there.
     */
    public static Cast cast(ServerLevel level, BlockPos pos, Grimoire grimoire) {
        if (!Casting.permitted(grimoire, Quell.SCHOOL)) {
            return Cast.no("unlearned");
        }
        AABB reach = new AABB(pos).inflate(Quell.REACH);
        List<Mob> found = level.getEntitiesOfClass(Mob.class, reach);
        Mob subject = found.stream()
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)))
                .orElse(null);
        if (subject == null) {
            return Cast.no("nothing there");
        }
        Quelled.mark(level, subject.getUUID());
        String name = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(subject.getType()).toString();
        if (!Casting.drawsOnTheCorpse(level.dimension() == Level.OVERWORLD)) {
            return new Cast(true, name, 0, "");
        }
        ChapterSavedData data = ChapterSavedData.get(level.getServer());
        int frayed = Unraveling.frayAround(level, pos, data, level.getRandom(),
                Casting.FRAY_RADIUS, Casting.FRAY_SAMPLES);
        return new Cast(true, name, frayed, "");
    }
}
