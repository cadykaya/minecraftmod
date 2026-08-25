package com.cadykaya.interregnum.registry;

import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.entity.EntityType;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.content.entity.ShrineKeeperEntity;
import com.cadykaya.interregnum.content.entity.WardenEntity;

/**
 * Every entity, in one place, for the same reason every block is.
 *
 * `MobCategory.MISC` on purpose: Wardens are never spawned by the world's natural
 * spawner. They are placed, by the Wardenate, for a reason. A Warden appearing in a
 * cave at night because the mob cap had room would say the opposite of everything
 * they are.
 */
public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister.Entities ENTITIES =
            DeferredRegister.createEntities(Interregnum.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<WardenEntity>> WARDEN =
            ENTITIES.registerEntityType("warden", WardenEntity::new, MobCategory.MISC,
                    b -> b.sized(0.7F, 2.4F)          // taller than a player, and narrow
                            .eyeHeight(2.15F)
                            .clientTrackingRange(12)); // they are meant to be seen coming

    /** A person at a shrine, still reconciling. See ShrineKeeperEntity. */
    public static final DeferredHolder<EntityType<?>, EntityType<ShrineKeeperEntity>> SHRINE_KEEPER =
            ENTITIES.registerEntityType("shrine_keeper", ShrineKeeperEntity::new, MobCategory.MISC,
                    b -> b.sized(0.6F, 1.9F)
                            .eyeHeight(1.62F)
                            .clientTrackingRange(10));

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        // A mod-bus event, so it is added to the bus explicitly rather than through
        // @EventBusSubscriber. An entity type with no attribute supplier does not
        // fail at registration -- it fails when something tries to spawn one, which
        // is much later and much harder to read.
        modBus.addListener(ModEntities::onAttributeCreation);
    }

    private static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(WARDEN.get(), WardenEntity.createAttributes().build());
        event.put(SHRINE_KEEPER.get(), ShrineKeeperEntity.createAttributes().build());
    }
}
