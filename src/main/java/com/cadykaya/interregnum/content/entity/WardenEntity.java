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
        return true;
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        setPersistenceRequired();
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
    public static final net.minecraft.resources.Identifier INTAKE =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(
                    com.cadykaya.interregnum.Interregnum.MOD_ID, "warden_intake");

    /** How far a bystander can stand and still be part of the conversation. */
    private static final double TABLE_RADIUS = 8.0;

    /**
     * Being addressed.
     *
     * **Everyone nearby is pulled in**, not just whoever clicked. That is the
     * Star Wars: The Old Republic beat the owner asked for by name, and it is the
     * only version that makes the resolution rules mean anything -- a VOTE node
     * with one player at the table is an INITIATOR node with extra steps.
     *
     * Line of sight is required, so being underground or behind a wall keeps you
     * out of it, and standing back is a real way to decline. Anyone already in
     * another conversation is left alone.
     *
     * **[NEEDS PLAYTEST]** the radius, and the failure mode: an AFK player who gets
     * pulled in makes the rest of the table wait out the timeout before it can move.
     * The timeout exists precisely for this and a minute may still be too long.
     */
    @Override
    protected net.minecraft.world.InteractionResult mobInteract(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer initiator)) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        var server = level().getServer();
        if (server == null || Conversations.of(initiator.getUUID().toString()) != null) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        java.util.List<String> table = new java.util.ArrayList<>();
        table.add(initiator.getUUID().toString());
        for (net.minecraft.server.level.ServerPlayer other
                : level().getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class,
                        getBoundingBox().inflate(TABLE_RADIUS))) {
            String id = other.getUUID().toString();
            if (other != initiator && hasLineOfSight(other) && Conversations.of(id) == null) {
                table.add(id);
            }
        }

        try {
            Conversations.open(server, INTAKE, table, this);
        } catch (IllegalArgumentException e) {
            initiator.sendSystemMessage(net.minecraft.network.chat.Component.literal(e.getMessage()));
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
