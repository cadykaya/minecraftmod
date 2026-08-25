package com.cadykaya.interregnum.content.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Set;

import com.cadykaya.interregnum.system.ChapterSavedData;

/**
 * True only while the overworld still has a god.
 *
 * This is what makes the heart unique without ever choosing a shrine in advance.
 * Loot tables roll when a container is first OPENED, not when it generates, so
 * every shrine in the world is a candidate right up until the moment one of them
 * pays out — and from that moment on, none of them are. A player who opens a
 * hundred shrines after the deicide finds a hundred ordinary offerings.
 *
 * Nothing has to be decided at worldgen time, nothing has to be tracked per-shrine,
 * and the fiction is exact: the god's heart is in a shrine somewhere, and once it
 * is taken it is not anywhere any more.
 */
public class GodLivesCondition implements LootItemCondition {
    private static final GodLivesCondition INSTANCE = new GodLivesCondition();
    public static final MapCodec<GodLivesCondition> MAP_CODEC = MapCodec.unit(INSTANCE);

    private GodLivesCondition() {}

    public static LootItemCondition.Builder godLives() {
        return () -> INSTANCE;
    }

    @Override
    public MapCodec<GodLivesCondition> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of();
    }

    @Override
    public boolean test(LootContext context) {
        MinecraftServer server = context.getLevel().getServer();
        if (server == null) {
            return false;   // no server, no god, no heart. Fail closed.
        }
        return ChapterSavedData.get(server).mechanicsDormant();
    }
}
