package com.cadykaya.interregnum.content.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.cadykaya.interregnum.system.dialogue.Conversations;

/**
 * A Warden, walking.
 *
 * The statues wake and watch; these arrive. Two different objects doing two
 * different jobs, which is why waking a statue does not consume it -- the statue
 * your neighbour built into their garden wall stays exactly where it is, watching,
 * forever. The eye and the officer are not the same thing.
 *
 * **A Warden never attacks.** No attack damage, no target selector, no melee goal,
 * and no anger. This is the single most important fact about them and it is
 * enforced here rather than merely intended: {@link #createAttributes()} gives them
 * no ATTACK_DAMAGE at all, so there is nothing for a future careless goal to use.
 *
 * That is the dread covenant from docs/AESTHETIC.md doing its job. A thing that
 * walks up to you, stops at conversational distance, and *files a report* is worse
 * than a thing that swings, because there is no move that resolves it. You cannot
 * win a fight nobody is having. Wardens speak in procedure and they never quip; the
 * violence, when it eventually exists, is the Wardenate's, not this unit's.
 *
 * They are hard to move and hard to kill (100 health, full knockback resistance) so
 * that a player's first instinct -- hit it -- fails in the most informative
 * possible way: nothing happens, and it is still looking at you.
 * **[NEEDS PLAYTEST]** whether they should be killable at all.
 */
public class WardenEntity extends PathfinderMob {
    public WardenEntity(EntityType<? extends WardenEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    /**
     * Placed, never spawned, therefore never despawned. A Warden that wandered out
     * of simulation distance and evaporated would make the Wardenate look like
     * weather instead of an institution.
     *
     * This is the guarantee, and it is NOT the {@code setPersistenceRequired()}
     * call in the constructor. That call is real but it does not survive: `Mob`
     * reads `PersistenceRequired` straight back out of NBT in
     * {@link #readAdditionalSaveData}, defaulting to false, so anything the
     * constructor set is overwritten the moment the entity is loaded -- which
     * includes every `/summon`. A probe printed `PersistenceRequired: 0b` on a
     * freshly summoned Warden with the constructor call sitting right there in the
     * source. See docs/LESSONS.md #17.
     *
     * `requiresCustomPersistence` is the hook that cannot be undone by a tag, so
     * it is where the promise actually lives. The constructor call and the
     * re-apply below stay because they make the invariant visible in NBT, and an
     * invariant nothing can observe is one nothing can check.
     */
    @Override
    public boolean requiresCustomPersistence() {
        // A POSTED Warden is the exception, and it is the whole reason the flag
        // exists. A statue posts one while somebody is there to be inspected and it
        // stands down when they leave; the STATUE is the permanent thing and the
        // Warden is the response to it. Without this every statue on a long-running
        // server would accrete its own immortal Warden and never let one go, which
        // is not an institution either -- it is a leak.
        return !posted;
    }

    /**
     * Was this Warden posted by a statue, or placed?
     *
     * Saved in NBT because it decides whether the mob may ever despawn, and a flag
     * that vanishes on reload would silently promote every posted Warden to a
     * permanent one after a single restart (docs/LESSONS.md #17 is this exact shape).
     */
    private boolean posted;

    public boolean posted() {
        return posted;
    }

    public void setPosted(boolean value) {
        this.posted = value;
        if (!value) {
            setPersistenceRequired();
        }
    }

    /**
     * Both halves of the promise, because `checkDespawn` reads both.
     *
     * `Mob` gates despawning on `isPersistenceRequired() || requiresCustomPersistence()`,
     * and `Mob` offers no way to CLEAR the first: the field is private, the constructor
     * sets it, and `setPersistenceRequired()` only ever sets it true. So overriding
     * `requiresCustomPersistence` alone was not enough -- a posted Warden still reported
     * `PersistenceRequired: 1b` and would never have stood down. The check caught it;
     * reading the override and believing it did not.
     */
    @Override
    public boolean isPersistenceRequired() {
        return !posted && super.isPersistenceRequired();
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        posted = input.getBooleanOr("interregnum:posted", false);
        if (!posted) {
            setPersistenceRequired();
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("interregnum:posted", posted);
        // Re-stated after super, which writes the raw private field. An NBT flag that
        // disagrees with the behaviour is worse than no flag: it is what somebody
        // debugging this at two in the morning will read and believe.
        output.putBoolean("PersistenceRequired", isPersistenceRequired());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                // Slower than a walking player, and it does not matter: it is not
                // chasing anybody. It arrives.
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // 16 blocks, and a probability of 1.0: it does not glance, it looks. Vanilla
        // mobs use ~6-8 blocks and 0.02, which reads as an animal noticing you.
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 16.0F, 1.0F));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    /**
     * Wardens do not drown and do not suffocate.
     *
     * Not a convenience: they are not alive in the way the damage sources assume,
     * and an enforcement officer that can be defeated by a bucket of water is a
     * comedy the mod is not making yet.
     */
    @Override
    public boolean isInvulnerableTo(net.minecraft.server.level.ServerLevel level, DamageSource source) {
        return source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)
                || super.isInvulnerableTo(level, source);
    }

    @Override
    protected int decreaseAirSupply(int currentSupply) {
        return currentSupply;
    }

    /** Nothing to push around. They are not moved by crowds. */
    @Override
    public boolean isPushable() {
        return false;
    }

    /** The scene a Warden opens with. Datapack-defined; this only names it. */
    public static final net.minecraft.resources.Identifier INTERROGATION =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(
                    com.cadykaya.interregnum.Interregnum.MOD_ID, "warden_interrogation");

    public static final net.minecraft.resources.Identifier INTAKE =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(
                    com.cadykaya.interregnum.Interregnum.MOD_ID, "warden_intake");

    /**
     * Being addressed.
     *
     * The rule for who ends up at the table lives in
     * {@link Conversations#address} -- both mobs that can be spoken to need it, and
     * it is a fact about conversations rather than about Wardens.
     */
    @Override
    protected net.minecraft.world.InteractionResult mobInteract(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        if (player instanceof net.minecraft.server.level.ServerPlayer initiator) {
            Conversations.address(initiator, this, openingScene(initiator.level().getServer()));
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    /**
     * Which scene this unit opens with.
     *
     * The same mob, the same manner, one question changed. Before the death it is
     * conducting a census of the living; after it, it is taking statements about the
     * moment the count fell. The pair is the point -- a player who met a Warden in
     * Chapter 0 meets the identical procedure afterwards, and the only thing that has
     * moved is what the procedure is FOR.
     *
     * Read from the world's chapter data rather than from anything on the entity, so
     * a Warden that has been standing in a field since before the deicide answers the
     * same as one that walked up afterwards. A per-mob flag would make it a question
     * of which Warden you happened to meet.
     *
     * The shrine-keeper picks its scene the same way (from whether its box has been
     * opened). Both are `openingScene`, deliberately: it is the pattern for "an NPC
     * whose opening depends on what has happened", and the next one should be too.
     */
    public net.minecraft.resources.Identifier openingScene(
            net.minecraft.server.MinecraftServer server) {
        if (server == null) {
            return INTAKE;                  // no world to ask; the census is the default
        }
        return com.cadykaya.interregnum.system.ChapterSavedData.get(server)
                .has(com.cadykaya.interregnum.core.chapter.Milestone.DEICIDE)
                        ? INTERROGATION : INTAKE;
    }
}
