package com.cadykaya.interregnum.content.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.regard.Institution;
import com.cadykaya.interregnum.system.RegardSavedData;
import com.cadykaya.interregnum.system.dialogue.Conversations;

/**
 * Somebody is still doing the accounts.
 *
 * The opposite of a Warden in every way that shows. Where the Warden is cold worked
 * metal on a tall frame under a wide mantle, the keeper is short, hooded in cloth,
 * warm, and carrying a ledger -- and the palette does the characterisation without
 * a word: HELD is cool, SPENT is warm, and a person still reconciling a ledger for a
 * reader who is dead is spending themselves on it.
 *
 * They do not flee and they do not fight. Fleeing would make the scene unreachable,
 * and a keeper who threw a punch would be a different character entirely. They stay
 * at their post, because the post is the only thing left that has a shape.
 */
public class ShrineKeeperEntity extends PathfinderMob {
    public static final Identifier LEDGER =
            Identifier.fromNamespaceAndPath(Interregnum.MOD_ID, "shrine_keeper");

    /**
     * What killing one costs with the villages.
     *
     * `WORLD.md` says regard is moved by choices **and deeds**, and until now only
     * choices moved it -- which quietly taught the opposite lesson, that the only
     * thing anybody is judged on is dialogue. Murdering the person who tends the
     * shrine is a deed with an obvious constituency.
     */
    private static final int MURDER_COST = -25;

    public ShrineKeeperEntity(EntityType<? extends ShrineKeeperEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    /** Placed at a shrine, not spawned by weather. See WardenEntity for the details. */
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
                // An ordinary person. They are killable and that is deliberate:
                // the cost of doing it is a real cost, not an impossibility.
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F, 0.9F));
        // Barely moves. They are AT something, and wandering off would make the
        // whole scene a game of chase.
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.35, 0.6F));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer initiator) {
            Conversations.address(initiator, this, LEDGER);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * The villages hear about it.
     *
     * Only when a PLAYER did it. A keeper who drowns, burns, or is shot by a skeleton
     * is a tragedy the villages do not blame anybody for, and charging a player for
     * a creeper's work is the kind of unfairness that teaches people never to go near
     * an NPC again.
     */
    @Override
    public void die(DamageSource cause) {
        if (level() instanceof ServerLevel server
                && cause.getEntity() instanceof ServerPlayer killer) {
            RegardSavedData regard = RegardSavedData.get(server.getServer());
            regard.of(server.getServer(), killer.getUUID())
                    .adjust(Institution.VILLAGES, MURDER_COST);
            regard.touch();
        }
        super.die(cause);
    }
}
