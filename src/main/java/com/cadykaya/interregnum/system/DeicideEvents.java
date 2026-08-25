package com.cadykaya.interregnum.system;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.registry.ModItems;

/**
 * The trigger: a player picks up the heart.
 *
 * Deliberately thin. Everything it decides is in {@link Deicide#commit}, which is
 * reachable and tested without a player; this class only answers "was that the
 * heart, and who took it".
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class DeicideEvents {
    private DeicideEvents() {}

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;                       // logical client: never decides anything
        }
        if (!event.getItemEntity().getItem().is(ModItems.GOD_HEART.get())) {
            return;
        }
        // ServerPlayer#getServer() no longer exists; the server comes off the level.
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        Deicide.commit(server, player.getUUID());
    }
}
