package com.cadykaya.interregnum.system.dialogue;

import com.cadykaya.interregnum.Interregnum;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;

/**
 * The ghost, taking its killer out of a bed.
 *
 * `WORLD.md`, locked, on the Haunt: *"dream-audiences: **sleep** sometimes routes the
 * killer to a small dimension where the ghost…"* The scene, the gate and the once-only
 * rule have all existed for a long time in {@link TheHaunt} — and the only thing that
 * could reach them was `/interregnum haunt dream`. **The mod's best beat could not happen
 * in play.**
 *
 * <h2>It fires whether or not the bed would have worked, and that is the whole thing</h2>
 *
 * The death stops the daylight cycle. That is locked, and it is the entire announcement of
 * the death — *"there is simply a world that has stopped moving, and a player who has gone
 * very quiet"*. Which means that if the god dies in the afternoon, **night never comes**,
 * and a rule of "the dream arrives when you successfully sleep" would have quietly made
 * the Haunt unreachable in exactly the worlds it is about.
 *
 * So this does not look at {@link CanPlayerSleepEvent#getVanillaProblem}. You lie down in a
 * world where the sun has not moved since you did it, and the thing you cannot stop
 * thinking about takes you anyway. Two locked beats that would otherwise have contradicted
 * each other, and the contradiction is the better version of both.
 *
 * <h2>`OTHER_PROBLEM`, because it is the silent one</h2>
 *
 * The sleep is refused — the ghost has them, they are not going to wake up rested — and
 * `OTHER_PROBLEM` is the one refusal that carries no message. Vanilla says nothing, which
 * is the register the rest of the mod keeps: the world does not narrate what it is doing
 * to this player.
 *
 * <h2>Everyone else sleeps normally</h2>
 *
 * Every other outcome leaves the event exactly as vanilla decided it. {@link
 * TheHaunt#offer} already refuses a player who is not the killer, a world whose god is
 * alive, a second dream, and a player mid-conversation — so this handler adds no rule of
 * its own, and there is only one implementation of the gate to get wrong.
 *
 * <h2>[VERIFY] — the trigger, not the scene</h2>
 *
 * This container has no game client, so no player can right-click a bed on a headless
 * server: the branch below cannot be exercised here, and it is not claimed to be. What IS
 * verified is everything it calls — `haunt_check.sh` drives {@link TheHaunt#offer} through
 * the command seam and asserts the ghost reaches its killer once and nobody else ever.
 * This is deliberately three lines of adapter over that, the same arrangement {@link
 * com.cadykaya.interregnum.system.Deicide} documents and for the same reason. Clearing the
 * marker needs one player, one bed, and a client to hold them.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class HauntSleepEvents {
    private HauntSleepEvents() {}

    @SubscribeEvent
    public static void onCanSleep(CanPlayerSleepEvent event) {
        ServerPlayer player = event.getEntity();
        if (TheHaunt.offer(player.level().getServer(), player.getUUID(), false)
                == TheHaunt.Outcome.OPENED) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
    }
}
