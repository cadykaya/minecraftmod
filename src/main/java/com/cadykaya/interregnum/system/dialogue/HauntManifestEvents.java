package com.cadykaya.interregnum.system.dialogue;

import com.cadykaya.interregnum.Interregnum;
import com.cadykaya.interregnum.core.haunt.Manifestation;
import com.cadykaya.interregnum.core.haunt.Script;
import com.cadykaya.interregnum.system.haunt.RawScript;
import com.cadykaya.interregnum.system.ChapterSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The tick that occasionally lets the ghost move something.
 *
 * See {@link Manifest} for what happens and {@link Manifestation} for how often. This is
 * the adapter: it finds the killer if they are online, asks on the interval, and rolls the
 * odds. Every rule it appears to have is really in `Manifest.move`, which a command can
 * reach on a server with no players -- the same arrangement as the dream's, and for the
 * same reason.
 *
 * <h2>The killer only, looked up rather than scanned for</h2>
 *
 * The chapter record already knows who did it, so this asks it by name instead of walking
 * the player list and testing each one. On a server with fifty players that is one lookup
 * every ten seconds rather than fifty, and more importantly it means the "only the killer"
 * rule cannot be got wrong twice -- there is one place that decides, and this is not it.
 */
@EventBusSubscriber(modid = Interregnum.MOD_ID)
public final class HauntManifestEvents {
    private HauntManifestEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (!Manifestation.due(server.overworld().getGameTime())) {
            return;
        }
        ChapterSavedData data = ChapterSavedData.get(server);
        if (data.mechanicsDormant() || data.killer() == null) {
            return;
        }
        ServerPlayer killer = server.getPlayerList().getPlayer(data.killer());
        if (killer == null || !(killer.level() instanceof ServerLevel level)) {
            return;
        }
        // THE ODDS ARE THE KILLER'S, not the world's. `WORLD.md`: raw god-script read
        // without transcription marks the reader, and "marks" means exactly this -- the
        // ghost gets louder, and nothing else changes. A killer who has read nothing gets
        // `Manifestation.ODDS` unchanged, which is why this reads as a substitution rather
        // than as a second mechanism.
        int odds = Script.oddsFor(RawScript.by(level, killer.getUUID()));
        if (level.getRandom().nextInt(odds) != 0) {
            return;
        }
        Manifest.move(level, killer.blockPosition(), killer.getUUID());
    }
}
