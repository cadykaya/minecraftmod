package com.cadykaya.interregnum.content.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * A Warden walks a beat.
 *
 * <h2>The whole point is that it is boring, and that you can predict it</h2>
 *
 * The Warden used a {@link net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal}
 * until now, which is the goal a sheep uses. It looks fine for ten seconds and it is
 * wrong for every reason this mod exists: a random walk is an <em>animal</em> foraging,
 * and a Warden is a <em>unit on a post</em>. What separates the two is not speed or path
 * or animation. It is that the unit does the same thing in the same order.
 *
 * So the route is fixed: {@link #LEGS} points on a ring around the statue, always in the
 * same order, always starting from the same one. A player who watches for two minutes
 * can say where it will be next, and being able to say that is the entire payload.
 *
 * <h2>Why predictability is the feature and not a shortcut</h2>
 *
 * `WORLD.md` locks the thesis that <b>violence does nothing and paperwork works</b>, and
 * that the exploit a player finds is administrative. An enforcement system you can plan
 * around is a prerequisite for that. A Warden that wandered unpredictably would make the
 * only viable answer "wait and hope"; a Warden with a beat makes the answer "it is at
 * the north point for four seconds every circuit", which is a thing a person can use.
 *
 * The same fact reads as dread rather than as exploitability, because it arrives first
 * as *this thing is not improvising, it is executing*, and only later as *and therefore
 * I can time it.*
 *
 * <h2>What happens at a corner</h2>
 *
 * It stops, and it files a return on the site it is standing in — see
 * {@link com.cadykaya.interregnum.system.warden.SiteReturn}, which explains at length
 * why that return is a census and not an accusation. Filing happens on ARRIVAL, not on
 * a leg being abandoned: a unit that could not reach a corner has not inspected it, and
 * an institution that filed returns on places it never stood would be a different and
 * much worse joke than the one the mod is making.
 *
 * <h2>Deliberately not here</h2>
 *
 * It does not cite, confiscate or escalate. A citation needs an offence and the mod does
 * not yet have one it can find; the reasoning is in `SiteReturn`.
 */
public class WardenPatrolGoal extends Goal {

    /**
     * Four points, on the cardinals.
     *
     * Four rather than eight: at {@link #RING} blocks with a {@link #DWELL} pause, eight
     * legs put the Warden in motion nearly all the time, and a unit that never stops
     * reads as agitated. Standing still at a corner for two seconds is most of what
     * makes it look like it is *checking* something rather than travelling.
     */
    public static final int LEGS = 4;

    /** Radius of the beat, in blocks. Comfortably inside the posting tether. */
    public static final int RING = 8;

    /** Ticks spent standing at each point before moving on. */
    public static final int DWELL = 40;

    /**
     * How close counts as arrived.
     *
     * Generous, because a Warden that re-paths because it stopped 1.4 blocks short
     * would jitter at every corner, and jitter is the single most animal-looking thing
     * a mob can do.
     */
    private static final double ARRIVED = 2.5;

    /**
     * Give up on a leg after this long and take the next one.
     *
     * Terrain changes: the unraveling eats the ground these things stand on, and a
     * player may wall one of the four points off — which is a legitimate thing to do
     * with a system you have worked out. Either way the Warden must not spend the rest
     * of the game trying to reach a point it cannot reach. It moves on, without
     * comment, which is also what it would do.
     */
    private static final int LEG_TIMEOUT = 200;

    /**
     * How many ticks to wait before asking for a path again after being refused.
     *
     * Short, because the commonest refusal lasts exactly one tick -- see
     * {@link #tick()} -- and long enough that a genuinely unreachable corner is not
     * re-pathed sixty times a second while the Warden stands there.
     */
    private static final int RETRY_DELAY = 5;

    /**
     * Refusals in a row before the leg is abandoned.
     *
     * Eight tries at {@link #RETRY_DELAY} ticks is about two seconds of trying, which
     * is long enough to outlast anything transient and short enough that a Warden
     * whose north point has been walled in gets on with the rest of its round.
     */
    private static final int MAX_ATTEMPTS = 8;

    private final PathfinderMob mob;
    private final double speed;

    /**
     * Which leg is next. Not persisted, so a Warden that reloads restarts its circuit
     * at the north point -- which is correct rather than merely convenient: a unit
     * coming back on duty starts its round from the beginning.
     */
    private int leg;
    private int legTicks;
    private int dwellTicks;
    private int attempts;
    private BlockPos target;

    public WardenPatrolGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        // MOVE and LOOK: the beat owns both, so a look-at-player goal at a higher
        // priority can interrupt the whole thing rather than fighting it for the head.
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /** A Warden only has a beat if something posted it. An unposted one just stands. */
    @Override
    public boolean canUse() {
        return mob.hasHome();
    }

    @Override
    public boolean canContinueToUse() {
        return mob.hasHome();
    }

    @Override
    public void start() {
        legTicks = 0;
        dwellTicks = 0;
        attempts = 0;
        target = null;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        target = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (dwellTicks > 0) {
            dwellTicks--;
            return;
        }
        if (target == null) {
            BlockPos next = pointFor(mob.getHomePosition(), leg);
            // The return value is load-bearing, and ignoring it cost an afternoon.
            //
            // `GroundPathNavigation` refuses to build a path unless the mob is
            // `onGround`, and a mob that has just been spawned is not: goals tick
            // BEFORE movement inside the same `aiStep`, so on a posted Warden's very
            // first tick this call ALWAYS fails. Accepting that silently left the
            // Warden standing at its statue for a full LEG_TIMEOUT before it tried
            // anything else -- which reads exactly like a patrol that does not work,
            // and was one, for ten seconds at a time.
            if (mob.getNavigation().moveTo(next.getX() + 0.5, next.getY(),
                                           next.getZ() + 0.5, speed)) {
                target = next;
                legTicks = 0;
                attempts = 0;
                return;
            }
            // Refused. Try again shortly rather than burning the leg's whole timeout;
            // abandon the corner only after MAX_ATTEMPTS, which is the case where
            // somebody has walled it off -- a legitimate thing to do to a system you
            // have worked out, and not a reason for the round to stop.
            if (++attempts >= MAX_ATTEMPTS) {
                leg = (leg + 1) % LEGS;
                attempts = 0;
            }
            dwellTicks = RETRY_DELAY;
            return;
        }

        legTicks++;
        boolean arrived = mob.position().closerThan(
                new net.minecraft.world.phys.Vec3(target.getX() + 0.5, mob.getY(),
                                                  target.getZ() + 0.5), ARRIVED);
        if (arrived && mob.level() instanceof net.minecraft.server.level.ServerLevel level) {
            // On arrival only. See the class javadoc: a corner it could not reach is a
            // corner it did not inspect.
            com.cadykaya.interregnum.system.warden.SiteReturn.file(
                    com.cadykaya.interregnum.system.warden.SiteReturn.survey(
                            level, mob.blockPosition()));
        }
        if (arrived || legTicks > LEG_TIMEOUT) {
            // Advance whether it arrived or gave up. A leg it cannot walk is still a
            // leg of the round -- the round does not stop because one corner is
            // unreachable, and neither does anything else this institution does.
            mob.getNavigation().stop();
            leg = (leg + 1) % LEGS;
            target = null;
            attempts = 0;
            dwellTicks = DWELL;
        }
    }

    /**
     * Leg {@code i}'s point: the ring's cardinal offsets, in a fixed order.
     *
     * Deliberately integer offsets rather than trigonometry. Four points do not need
     * sin and cos, and the arithmetic being trivially readable is what lets somebody
     * check by eye that the route is the same every circuit -- which is the property
     * the whole class exists to have.
     */
    public static BlockPos pointFor(BlockPos home, int i) {
        return switch (i % LEGS) {
            case 0 -> home.offset(0, 0, -RING);   // north
            case 1 -> home.offset(RING, 0, 0);    // east
            case 2 -> home.offset(0, 0, RING);    // south
            default -> home.offset(-RING, 0, 0);  // west
        };
    }
}
